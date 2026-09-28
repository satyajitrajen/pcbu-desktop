#ifndef PCBU_DESKTOP_LOCKSERVER_H
#define PCBU_DESKTOP_LOCKSERVER_H

#include <atomic>
#include <string>
#include <thread>

#include "connection/BaseConnection.h"

class LockServer : public BaseConnection {
public:
  LockServer();
  ~LockServer() override;

  bool Start();
  void Stop();

private:
  void AcceptThread();
  void ClientThread(SOCKET clientSocket);
  void HandleClient(SOCKET clientSocket);
  bool WriteError(SOCKET clientSocket, const std::string &code);
  bool WriteSuccess(SOCKET clientSocket, const std::string &encKey, const std::string &nonce);

  SOCKET m_ServerSocket;
  std::atomic<bool> m_IsRunning{false};
  std::atomic<int> m_NumConnections{0};
  std::thread m_AcceptThread;
};

#endif
