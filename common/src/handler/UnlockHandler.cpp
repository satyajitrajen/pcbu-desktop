#include "UnlockHandler.h"

#include "KeyScanner.h"
#include "connection/unlock/clients/BTUnlockClient.h"
#include "connection/unlock/clients/TCPUnlockClient.h"
#include "connection/unlock/servers/TCPUnlockServer.h"
#include "platform/NetworkHelper.h"
#include "storage/PairedDevicesStorage.h"

#ifdef WINDOWS
#include <Windows.h>
#define KEY_LEFTCTRL VK_LCONTROL
#define KEY_LEFTALT VK_LMENU
#elif LINUX
#include <linux/input.h>
#elif APPLE
#include <Carbon/Carbon.h>
#define KEY_LEFTCTRL kVK_Control
#define KEY_LEFTALT kVK_Option
#endif

UnlockHandler::UnlockHandler(const std::function<void(std::string)> &printMessage) {
  m_PrintMessage = printMessage;
}

UnlockResult UnlockHandler::GetResult(const std::string &authUser, const std::string &authProgram, std::atomic<bool> *isRunning) {
  auto devices = PairedDevicesStorage::GetDevicesForUser(authUser);
  if(devices.empty()) {
    auto errorMsg = I18n::Get("error_not_paired", authUser);
    spdlog::error(errorMsg);
    m_PrintMessage(errorMsg);
    return UnlockResult(UnlockState::NOT_PAIRED_ERROR);
  }
  UDPUnlockBroadcaster *udpBroadcaster{};
  std::vector<BaseUnlockConnection *> connections{};

  auto addTcpClient = [&](const std::string &ip, uint16_t port, const PairedDevice &device) {
    if(ip.empty() || port == 0)
      return;
    auto *connection = new TCPUnlockClient(ip, static_cast<int>(port), device);
    connection->SetUnlockInfo(authUser, authProgram);
    connections.emplace_back(connection);
  };

  for(const auto &device : devices) {
    switch(device.pairingMethod) {
      case PairingMethod::TCP:
        addTcpClient(device.ipAddress, device.tcpPort, device);
        break;
      case PairingMethod::BLUETOOTH: {
        auto *bt = new BTUnlockClient(device.bluetoothAddress, device);
        bt->SetUnlockInfo(authUser, authProgram);
        connections.emplace_back(bt);
        break;
      }
      case PairingMethod::MANUAL_UDP:
      case PairingMethod::UDP: {
        if(udpBroadcaster == nullptr)
          udpBroadcaster = new UDPUnlockBroadcaster();
        auto port = device.pairingMethod == PairingMethod::UDP ? device.udpPort : device.udpManualPort;
        udpBroadcaster->AddDevice(device.id, port, device.pairingMethod == PairingMethod::MANUAL_UDP);
        break;
      }
      case PairingMethod::CLOUD_TCP:
        break;
      default:
        spdlog::error("Invalid pairing method.");
        break;
    }
    if(NetworkHelper::IsOverlayIpv4(device.tsIP) && device.tsIP != device.ipAddress)
      addTcpClient(device.tsIP, device.tcpPort, device);
  }

  auto *server = new TCPUnlockServer();
  server->SetUnlockInfo(authUser, authProgram);
  connections.emplace_back(server);

  // Start servers
  std::vector<std::thread> threads{};
  AtomicUnlockResult currentResult{};
  std::atomic completed(0);
  std::mutex mutex{};
  std::condition_variable cv{};
  auto numServers = connections.size();
  threads.reserve(numServers);
  for(auto connection : connections) {
    threads.emplace_back([this, connection, numServers, isRunning, &currentResult, &completed, &cv, &mutex, udpBroadcaster]() {
      auto serverResult = RunServer(connection, udpBroadcaster, &currentResult, isRunning);
      completed.fetch_add(1);
      if(completed.load() == numServers) {
        if(currentResult.load().state != UnlockState::SUCCESS && !currentResult.isClaimed())
          currentResult.store(serverResult);
        std::lock_guard l(mutex);
        cv.notify_one();
      }
    });
  }

  // UDP Broadcast
  if(udpBroadcaster) {
    udpBroadcaster->Start();
  }

  // Wait
  std::unique_lock lock(mutex);
  cv.wait(lock, [&] { return completed.load() == numServers; });
  auto result = currentResult.load();

  // Cleanup
  if(udpBroadcaster) {
    udpBroadcaster->Stop();
    delete udpBroadcaster;
  }
  for(auto &thread : threads) {
    if(thread.joinable())
      thread.join();
  }
  for(const auto connection : connections)
    delete connection;
  return result;
}

UnlockResult UnlockHandler::RunServer(BaseUnlockConnection *connection, UDPUnlockBroadcaster *udpBroadcaster, AtomicUnlockResult *currentResult,
                                      std::atomic<bool> *isRunning) {
  if(!connection->Start()) {
    auto errorMsg = I18n::Get("error_start_handler");
    spdlog::error(errorMsg);
    m_PrintMessage(errorMsg);
    return UnlockResult(UnlockState::START_ERROR);
  }

  auto connectMessage = I18n::Get(connection->IsServer() ? "wait_server_phone_connect" : "wait_client_phone_connect");
  m_PrintMessage(connectMessage);
  auto keyScanner = KeyScanner();
  keyScanner.Start();

  auto state = UnlockState::UNKNOWN;
  auto startTime = Utils::GetCurrentTimeMs();
  auto isWaitingForConnection = true;
  auto isFutureCancel = false;
  auto claimedSuccess = false;
  while(true) {
    if(currentResult->isClaimed() || (isRunning != nullptr && !isRunning->load())) {
      state = UnlockState::CANCELED;
      isFutureCancel = true;
      break;
    }
    if(connection->HasClient() && isWaitingForConnection) {
      m_PrintMessage(I18n::Get("wait_phone_unlock"));
      isWaitingForConnection = false;
      if(udpBroadcaster) {
        udpBroadcaster->Stop();
      }
    }

    state = connection->PollResult();
    if(state == UnlockState::SUCCESS) {
      if(!currentResult->tryClaimSuccess()) {
        state = UnlockState::CANCELED;
        isFutureCancel = true;
        break;
      }
      claimedSuccess = true;
    }
    if(state != UnlockState::UNKNOWN)
      break;
    if(!connection->HasClient() && Utils::GetCurrentTimeMs() - startTime > CRYPT_PACKET_TIMEOUT) {
      state = UnlockState::TIMEOUT;
      break;
    }
    if(keyScanner.GetKeyState(KEY_LEFTCTRL) && keyScanner.GetKeyState(KEY_LEFTALT)) {
      state = UnlockState::CANCELED;
      break;
    }

    if(!connection->HasClient() && !isWaitingForConnection) {
      m_PrintMessage(connectMessage);
      isWaitingForConnection = true;
      if(udpBroadcaster) {
        udpBroadcaster->Start();
      }
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(20));
  }

  connection->Stop();
  keyScanner.Stop();
  if(!isFutureCancel)
    m_PrintMessage(UnlockStateUtils::ToString(state));
  spdlog::info("Connection result: {}", UnlockStateUtils::ToString(state));

  auto pwDec = CryptUtils::DecryptAES(connection->GetDevice().passwordEnc, connection->GetResponseData().passwordKey);
  if(!pwDec.has_value() && state == UnlockState::SUCCESS) {
    auto errorMsg = I18n::Get("error_password_decrypt");
    spdlog::error(errorMsg);
    m_PrintMessage(errorMsg);
    auto failed = UnlockResult(UnlockState::DATA_ERROR);
    currentResult->store(failed);
    return failed;
  }

  auto result = UnlockResult();
  result.state = state;
  result.device = connection->GetDevice();
  result.password = pwDec.has_value() ? pwDec.value() : "";
  if(claimedSuccess)
    currentResult->store(result);
  return result;
}
