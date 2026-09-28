#ifndef PCBU_DESKTOP_APPINFO_H
#define PCBU_DESKTOP_APPINFO_H

#include <string>

class AppInfo {
public:
  static std::string GetName();
  static std::string GetVersion();
  static std::string GetPairingProtocolVersion();
  static std::string GetUnlockProtocolVersion();
  static std::string GetLockProtocolVersion();
  static std::string GetLockPowerProtocolVersion();

  static std::string GetOperatingSystem();
  static std::string GetArchitecture();

  static int CompareVersion(const std::string &thisVersion, const std::string &otherVersion);

private:
  AppInfo() = default;
};

#endif // PCBU_DESKTOP_APPINFO_H
