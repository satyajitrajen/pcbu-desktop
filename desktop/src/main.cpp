#include <QApplication>
#include <QIcon>
#include <QQmlApplicationEngine>
#include <QWindow>

#include "storage/LoggingSystem.h"
#include "ui/DesktopSingleton.h"
#include "ui/TrayController.h"
#include "utils/AppInfo.h"

#ifdef WINDOWS
#include "connection/lock/LockServer.h"
#endif

int main(int argc, char *argv[]) {
  qputenv("QT_QUICK_CONTROLS_STYLE", QByteArray("Material"));
  qputenv("QT_QUICK_CONTROLS_MATERIAL_THEME", QByteArray("Dark"));
  qputenv("QT_QUICK_CONTROLS_MATERIAL_VARIANT", QByteArray("Dense"));
  LoggingSystem::Init("desktop");

  QApplication app(argc, argv);
  QApplication::setApplicationName(QString::fromUtf8(AppInfo::GetName()));
  QApplication::setApplicationDisplayName(QString::fromUtf8(AppInfo::GetName()));
  QApplication::setWindowIcon(QIcon(":/res/icons/icon.png"));

  DesktopSingleton singleton;
  if(!singleton.claimPrimary()) {
    LoggingSystem::Destroy();
    return 0;
  }

#ifdef WINDOWS
  LockServer lockServer;
  lockServer.Start();
#endif

  auto url = QUrl("qrc:/ui/MainWindow.qml");
  QQmlApplicationEngine engine{};
  QObject::connect(
      &engine, &QQmlApplicationEngine::objectCreated, &app,
      [url](QObject *obj, const QUrl &objUrl) {
        if(!obj && url == objUrl) {
          QCoreApplication::exit(-1);
        }
      },
      Qt::QueuedConnection);
  engine.load(url);

  QWindow *window = nullptr;
  if(!engine.rootObjects().isEmpty())
    window = qobject_cast<QWindow *>(engine.rootObjects().constFirst());

  TrayController tray(window);
  QApplication::setQuitOnLastWindowClosed(!tray.isEnabled());
  QObject::connect(&singleton, &DesktopSingleton::showRequested, &tray, &TrayController::showWindow);

  auto result = QApplication::exec();
#ifdef WINDOWS
  lockServer.Stop();
#endif
  LoggingSystem::Destroy();
  return result;
}
