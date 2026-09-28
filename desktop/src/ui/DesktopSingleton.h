#ifndef PCBU_DESKTOP_DESKTOPSINGLETON_H
#define PCBU_DESKTOP_DESKTOPSINGLETON_H

#include <QObject>

class QLocalServer;

class DesktopSingleton : public QObject {
  Q_OBJECT
public:
  explicit DesktopSingleton(QObject *parent = nullptr);

  bool claimPrimary();

signals:
  void showRequested();

private:
  QLocalServer *m_Server = nullptr;
};

#endif
