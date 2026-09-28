#ifndef PCBU_DESKTOP_TRAYCONTROLLER_H
#define PCBU_DESKTOP_TRAYCONTROLLER_H

#include <QMenu>
#include <QObject>
#include <QPointer>
#include <QSystemTrayIcon>

class QWindow;

class TrayController : public QObject {
  Q_OBJECT
public:
  explicit TrayController(QWindow *window, QObject *parent = nullptr);

  bool isEnabled() const;

public slots:
  void showWindow();
  void quitApp();

private:
  void setupTray();

  QPointer<QWindow> m_Window;
  QSystemTrayIcon m_Tray;
  QMenu m_Menu;
  bool m_Enabled = false;
};

#endif
