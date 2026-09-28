#include "DesktopSingleton.h"

#include <QLocalServer>
#include <QLocalSocket>

#include <spdlog/spdlog.h>

namespace {
constexpr auto kServerName = "JarvisAuthGui";
}

DesktopSingleton::DesktopSingleton(QObject *parent) : QObject(parent) {}

bool DesktopSingleton::claimPrimary() {
  QLocalSocket probe;
  probe.connectToServer(kServerName);
  if(probe.waitForConnected(250)) {
    probe.write("SHOW");
    probe.waitForBytesWritten(250);
    probe.disconnectFromServer();
    probe.waitForDisconnected(250);
    return false;
  }

  QLocalServer::removeServer(kServerName);
  m_Server = new QLocalServer(this);
  if(!m_Server->listen(kServerName)) {
    spdlog::warn("Desktop singleton listen failed: {}", m_Server->errorString().toStdString());
    return true;
  }
  QObject::connect(m_Server, &QLocalServer::newConnection, this, [this]() {
    auto *sock = m_Server->nextPendingConnection();
    if(!sock)
      return;
    QObject::connect(sock, &QLocalSocket::readyRead, this, [this, sock]() {
      sock->readAll();
      emit showRequested();
      sock->disconnectFromServer();
      sock->deleteLater();
    });
  });
  return true;
}
