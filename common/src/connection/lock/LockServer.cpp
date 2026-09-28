#include "LockServer.h"

#include <chrono>
#include <spdlog/spdlog.h>
#include <thread>
#include <vector>

#include "connection/Packets.h"
#include "connection/SocketDefs.h"
#include "storage/AppSettings.h"
#include "storage/PairedDevicesStorage.h"
#include "utils/AppInfo.h"
#include "utils/CryptUtils.h"
#include "utils/StringUtils.h"

#ifdef WINDOWS
#include <Windows.h>
#include <Ws2tcpip.h>
#include <reason.h>
#else
#include <netinet/tcp.h>
#endif

constexpr int LOCK_MAX_CLIENTS = 4;
constexpr uint32_t LOCK_SOCKET_TIMEOUT_SECS = 8;

#ifdef WINDOWS
namespace {

bool EnableShutdownPrivilege() {
  HANDLE token = nullptr;
  if(!OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &token))
    return false;
  TOKEN_PRIVILEGES tp{};
  LUID luid{};
  if(!LookupPrivilegeValue(nullptr, SE_SHUTDOWN_NAME, &luid)) {
    CloseHandle(token);
    return false;
  }
  tp.PrivilegeCount = 1;
  tp.Privileges[0].Luid = luid;
  tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
  const BOOL adjusted = AdjustTokenPrivileges(token, FALSE, &tp, sizeof(tp), nullptr, nullptr);
  const DWORD err = GetLastError();
  CloseHandle(token);
  return adjusted && err == ERROR_SUCCESS;
}

bool ForcePower(bool reboot) {
  const UINT flags = (reboot ? EWX_REBOOT : EWX_SHUTDOWN) | EWX_FORCE;
  return ExitWindowsEx(flags, SHTDN_REASON_MAJOR_OTHER | SHTDN_REASON_MINOR_OTHER | SHTDN_REASON_FLAG_PLANNED) != FALSE;
}

} // namespace
#endif

LockServer::LockServer() : m_ServerSocket(SOCKET_INVALID) {}

LockServer::~LockServer() {
  Stop();
}

bool LockServer::Start() {
  if(m_IsRunning)
    return true;

  WSA_STARTUP
  m_IsRunning = true;
  m_AcceptThread = std::thread(&LockServer::AcceptThread, this);
  return true;
}

void LockServer::Stop() {
  m_IsRunning = false;
  SOCKET_CLOSE(m_ServerSocket);
  if(m_AcceptThread.joinable())
    m_AcceptThread.join();
}

void LockServer::AcceptThread() {
  struct sockaddr_in address {};
  socklen_t addrLen = sizeof(address);
  auto settings = AppSettings::Get();
  auto clientSockets = std::vector<SOCKET>();
  auto clientThreads = std::vector<std::thread>();
  // Dedicated port: login unlock still binds 43296 after Win+L.
  auto port = settings.lockServerPort ? settings.lockServerPort : static_cast<uint16_t>(43298);
  spdlog::info("Starting lock server...");

  if((m_ServerSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)) == SOCKET_INVALID) {
    spdlog::error("lock socket() failed. (Code={})", SOCKET_LAST_ERROR);
    m_IsRunning = false;
    return;
  }

  int opt = 1;
  if(setsockopt(m_ServerSocket, SOL_SOCKET, SO_REUSEADDR, reinterpret_cast<const char *>(&opt), sizeof(opt))) {
    spdlog::error("lock setsockopt(SO_REUSEADDR) failed. (Code={})", SOCKET_LAST_ERROR);
    goto threadEnd;
  }
  if(setsockopt(m_ServerSocket, IPPROTO_TCP, TCP_NODELAY, reinterpret_cast<const char *>(&opt), sizeof(opt))) {
    spdlog::error("lock setsockopt(TCP_NODELAY) failed. (Code={})", SOCKET_LAST_ERROR);
    goto threadEnd;
  }

  address.sin_family = AF_INET;
  address.sin_addr.s_addr = INADDR_ANY;
  address.sin_port = htons(port);
  if(bind(m_ServerSocket, reinterpret_cast<struct sockaddr *>(&address), sizeof(address)) < 0) {
    spdlog::error("lock bind({}) failed. (Code={})", port, SOCKET_LAST_ERROR);
    goto threadEnd;
  }
  if(listen(m_ServerSocket, LOCK_MAX_CLIENTS) < 0) {
    spdlog::error("lock listen() failed. (Code={})", SOCKET_LAST_ERROR);
    goto threadEnd;
  }
  if(!SetSocketBlocking(m_ServerSocket, false)) {
    spdlog::error("lock server socket non-blocking failed. (Code={})", SOCKET_LAST_ERROR);
    goto threadEnd;
  }

  spdlog::info("Lock server started on port '{}'.", port);
  while(m_IsRunning) {
    if(m_NumConnections >= LOCK_MAX_CLIENTS) {
      std::this_thread::sleep_for(std::chrono::milliseconds(50));
      continue;
    }
    SOCKET clientSocket;
    if((clientSocket = accept(m_ServerSocket, reinterpret_cast<struct sockaddr *>(&address), (socklen_t *)&addrLen)) == SOCKET_INVALID) {
      auto err = SOCKET_LAST_ERROR;
      if(err == SOCKET_ERROR_TRY_AGAIN || err == SOCKET_ERROR_WOULD_BLOCK) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
        continue;
      }
      if(err != SOCKET_ERROR_CONNECT_ABORTED && m_IsRunning)
        spdlog::error("lock accept() failed. (Code={})", err);
      break;
    }
    clientSockets.emplace_back(clientSocket);
    clientThreads.emplace_back(&LockServer::ClientThread, this, clientSocket);
  }

threadEnd:
  for(auto clientSocket : clientSockets) {
    SOCKET_CLOSE(clientSocket);
  }
  SOCKET_CLOSE(m_ServerSocket);
  for(auto &thread : clientThreads)
    if(thread.joinable())
      thread.join();
  m_IsRunning = false;
  spdlog::info("Lock server stopped.");
}

void LockServer::ClientThread(SOCKET clientSocket) {
  ++m_NumConnections;
  try {
    HandleClient(clientSocket);
  } catch(const std::exception &ex) {
    spdlog::error("lock client exception: {}", ex.what());
  }
  --m_NumConnections;
  SOCKET_CLOSE(clientSocket);
}

void LockServer::HandleClient(SOCKET clientSocket) {
  int opt = 1;
  setsockopt(clientSocket, IPPROTO_TCP, TCP_NODELAY, reinterpret_cast<const char *>(&opt), sizeof(opt));
  if(!SetSocketBlocking(clientSocket, true) || !SetSocketRWTimeout(clientSocket, LOCK_SOCKET_TIMEOUT_SECS)) {
    spdlog::error("lock client socket setup failed. (Code={})", SOCKET_LAST_ERROR);
    return;
  }

  auto packet = ReadPacket(clientSocket);
  if(packet.error != PacketError::NONE) {
    spdlog::error("lock read failed.");
    return;
  }
  if(packet.id != PACKET_ID_LOCK_REQUEST) {
    WriteError(clientSocket, "DATA_ERROR");
    return;
  }

  auto request = PacketLockRequest::FromJson({packet.data.begin(), packet.data.end()});
  if(!request.has_value() || request->deviceId.empty()) {
    WriteError(clientSocket, "DATA_ERROR");
    return;
  }

  auto device = PairedDevicesStorage::GetDeviceByID(request->deviceId);
  if(!device.has_value()) {
    spdlog::info("lock NOT_PAIRED ({})", request->deviceId.size() >= 8 ? request->deviceId.substr(0, 8) : request->deviceId);
    WriteError(clientSocket, "NOT_PAIRED");
    return;
  }

  // Accept lock 1.0.0 and power 1.1.0. Exact-match on GetLockProtocolVersion() would reject shutdown.
  const bool isLockV1 = AppInfo::CompareVersion(AppInfo::GetLockProtocolVersion(), request->protoVersion) == 0;
  const bool isPowerV11 = AppInfo::CompareVersion(AppInfo::GetLockPowerProtocolVersion(), request->protoVersion) == 0;
  if(!isLockV1 && !isPowerV11) {
    WriteError(clientSocket, "PROTOCOL_ERROR");
    return;
  }

  auto cryptData = StringUtils::FromHexString(request->encData);
  auto cryptResult = CryptUtils::DecryptAESPacket(cryptData, device->encryptionKey);
  if(cryptResult.result != PacketCryptResult::OK) {
    WriteError(clientSocket, cryptResult.result == PacketCryptResult::INVALID_TIMESTAMP ? "TIME_ERROR" : "DATA_ERROR");
    return;
  }

  auto inner = PacketLockRequestData::FromJson({cryptResult.data.begin(), cryptResult.data.end()});
  if(!inner.has_value() || inner->nonce.empty()) {
    WriteError(clientSocket, "DATA_ERROR");
    return;
  }
  if(inner->pcDeviceId != device->id) {
    WriteError(clientSocket, "NOT_PAIRED");
    return;
  }
  if(isPowerV11) {
    if(inner->action != "shutdown" && inner->action != "restart" && inner->action != "lock") {
      WriteError(clientSocket, "DATA_ERROR");
      return;
    }
  }

  PairedDevicesStorage::RememberTsIP(device->id, request->tsIP);
  const auto idShort = device->id.size() >= 8 ? device->id.substr(0, 8) : device->id;

#ifdef WINDOWS
  const bool powerOff = isPowerV11 && (inner->action == "shutdown" || inner->action == "restart");
  if(powerOff) {
    if(!EnableShutdownPrivilege()) {
      spdlog::error("SE_SHUTDOWN_NAME failed. (Code={})", GetLastError());
      WriteError(clientSocket, "APP_ERROR");
      return;
    }
    if(!WriteSuccess(clientSocket, device->encryptionKey, inner->nonce)) {
      spdlog::error("power success write failed.");
      return;
    }
    spdlog::info("{} ok ({})", inner->action, idShort);
    if(!ForcePower(inner->action == "restart"))
      spdlog::error("ExitWindowsEx failed. (Code={})", GetLastError());
    return;
  }

  if(!LockWorkStation()) {
    spdlog::error("LockWorkStation failed. (Code={})", GetLastError());
    WriteError(clientSocket, "APP_ERROR");
    return;
  }
#else
  WriteError(clientSocket, "APP_ERROR");
  return;
#endif

  if(!WriteSuccess(clientSocket, device->encryptionKey, inner->nonce)) {
    spdlog::error("lock success write failed.");
    return;
  }
  spdlog::info("lock ok ({})", idShort);
}

bool LockServer::WriteError(SOCKET clientSocket, const std::string &code) {
  auto response = PacketLockResponse();
  response.error = code;
  auto json = response.ToJson().dump();
  return WritePacket(clientSocket, PACKET_ID_LOCK_RESPONSE, {json.begin(), json.end()}) == PacketError::NONE;
}

bool LockServer::WriteSuccess(SOCKET clientSocket, const std::string &encKey, const std::string &nonce) {
  auto data = PacketLockResponseData();
  data.nonce = nonce;
  auto plain = data.ToJson().dump();
  auto cryptResult = CryptUtils::EncryptAESPacket({plain.begin(), plain.end()}, encKey);
  if(cryptResult.result != PacketCryptResult::OK)
    return false;
  auto response = PacketLockResponse();
  response.error = "";
  response.encData = StringUtils::ToHexString(cryptResult.data);
  auto json = response.ToJson().dump();
  return WritePacket(clientSocket, PACKET_ID_LOCK_RESPONSE, {json.begin(), json.end()}) == PacketError::NONE;
}
