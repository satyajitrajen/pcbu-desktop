#include "TrayController.h"

#include "utils/AppInfo.h"
#include "utils/I18n.h"

#include <QAction>
#include <QCoreApplication>
#include <QIcon>
#include <QWindow>

TrayController::TrayController(QWindow *window, QObject *parent) : QObject(parent), m_Window(window), m_Tray(this) {
  setupTray();
}

bool TrayController::isEnabled() const {
  return m_Enabled;
}

void TrayController::setupTray() {
  if(!m_Window || !QSystemTrayIcon::isSystemTrayAvailable())
    return;

  m_Window->setProperty("trayAvailable", true);

  const auto appName = QString::fromUtf8(AppInfo::GetName());
  m_Tray.setIcon(QIcon(":/res/icons/icon.png"));
  m_Tray.setToolTip(appName);

  auto *openAction = m_Menu.addAction(QString::fromUtf8(I18n::Get("tray_open")));
  QObject::connect(openAction, &QAction::triggered, this, &TrayController::showWindow);
  m_Menu.addSeparator();
  auto *quitAction = m_Menu.addAction(QString::fromUtf8(I18n::Get("tray_quit")));
  QObject::connect(quitAction, &QAction::triggered, this, &TrayController::quitApp);
  m_Tray.setContextMenu(&m_Menu);

  QObject::connect(&m_Tray, &QSystemTrayIcon::activated, this, [this](QSystemTrayIcon::ActivationReason reason) {
    if(reason == QSystemTrayIcon::Trigger || reason == QSystemTrayIcon::DoubleClick)
      showWindow();
  });

  m_Tray.show();
  m_Enabled = m_Tray.isVisible();
}

void TrayController::showWindow() {
  if(!m_Window)
    return;
  m_Window->show();
  m_Window->raise();
  m_Window->requestActivate();
}

void TrayController::quitApp() {
  if(m_Window)
    m_Window->setProperty("quitting", true);
  QCoreApplication::quit();
}
