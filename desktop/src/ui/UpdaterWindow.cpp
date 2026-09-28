#include "UpdaterWindow.h"

#include "utils/AppInfo.h"

UpdaterWindow::~UpdaterWindow() {
  if(m_CheckThread.joinable())
    m_CheckThread.join();
  if(m_DownloadThread.joinable())
    m_DownloadThread.join();
}

QString UpdaterWindow::GetAppVersion() {
  return QString::fromUtf8(AppInfo::GetVersion());
}

QString UpdaterWindow::GetLatestVersion() {
  std::lock_guard<std::mutex> lock(m_VersionMutex);
  return m_LatestVersion;
}

void UpdaterWindow::CheckForUpdates(QObject *window) {
  (void)window;
}

void UpdaterWindow::OnDownloadClicked(QObject *window) {
  (void)window;
}
