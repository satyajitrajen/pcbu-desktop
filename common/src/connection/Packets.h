#ifndef PCBU_DESKTOP_PACKETS_H
#define PCBU_DESKTOP_PACKETS_H

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <nlohmann/json.hpp>

#include "storage/PairingMethod.h"

constexpr uint64_t PACKET_HEADER = 0xDB065AC7AFDFA4CC;

// Optional Tailscale overlay IPv4. Additive JSON; omit when empty. Do not put this on 0xB0 (raw pairing id).
inline std::string JsonTsIP(const nlohmann::json &json) {
  if(json.contains("tsIP") && json["tsIP"].is_string())
    return json["tsIP"].get<std::string>();
  return {};
}

inline void SetJsonTsIP(nlohmann::json &json, const std::string &tsIP) {
  if(!tsIP.empty())
    json["tsIP"] = tsIP;
}

constexpr uint16_t PACKET_ID_PAIR_INIT = 0x50;
constexpr uint16_t PACKET_ID_PAIR_RESPONSE = 0x51;

constexpr uint16_t PACKET_ID_DEVICE_ID = 0xB0;
constexpr uint16_t PACKET_ID_UNLOCK_REQUEST = 0xB1;
constexpr uint16_t PACKET_ID_UNLOCK_RESPONSE = 0xB2;
constexpr uint16_t PACKET_ID_LOCK_REQUEST = 0xB3;
constexpr uint16_t PACKET_ID_LOCK_RESPONSE = 0xB4;

struct PacketPairInit { // From phone
  std::string protoVersion{};
  std::string deviceUUID{};
  std::string deviceName{};
  std::string ipAddress{};
  uint16_t tcpPort{};
  uint16_t udpPort{};
  uint16_t udpManualPort{};
  std::string cloudToken{};
  std::string tsIP{};

  static std::optional<PacketPairInit> FromJson(const std::string &jsonStr) {
    try {
      auto json = nlohmann::json::parse(jsonStr);
      auto packet = PacketPairInit();
      packet.protoVersion = json["protoVersion"];
      try {
        packet.deviceUUID = json["deviceUUID"];
        packet.deviceName = json["deviceName"];
        packet.ipAddress = json["ipAddress"];
        packet.tcpPort = json["tcpPort"];
        packet.udpPort = json["udpPort"];
        packet.udpManualPort = json["udpManualPort"];
        packet.cloudToken = json["cloudToken"];
        packet.tsIP = JsonTsIP(json);
      } catch(...) {
      }
      return packet;
    } catch(...) {
    }
    return {};
  }
};

struct PacketPairResponseData { // From PC
  PairingMethod pairingMethod{};
  std::string deviceId{};
  std::string deviceName{};
  std::string deviceOS{};
  uint16_t unlockServerPort{};
  std::vector<std::string> macAddresses{};
  std::string userName{};
  std::string passwordKey{};

  nlohmann::json ToJson() {
    return {{"pairingMethod", PairingMethodUtils::ToString(pairingMethod)},
            {"deviceId", deviceId},
            {"deviceName", deviceName},
            {"deviceOS", deviceOS},
            {"unlockServerPort", unlockServerPort},
            {"macAddresses", macAddresses},
            {"userName", userName},
            {"passwordKey", passwordKey}};
  }
};

struct PacketPairResponse { // From PC
  std::string errMsg{};
  PacketPairResponseData data{};

  nlohmann::json ToJson() {
    return {{"errMsg", errMsg}, {"data", data.ToJson()}};
  }
};

struct PacketUnlockRequest {
  std::string protoVersion;
  std::string deviceId;
  std::string encData;
  std::string tsIP{};

  nlohmann::json ToJson() {
    nlohmann::json json = {{"protoVersion", protoVersion}, {"deviceId", deviceId}, {"encData", encData}};
    SetJsonTsIP(json, tsIP);
    return json;
  }
};

struct PacketUnlockRequestData {
  std::string user;
  std::string program;
  std::string unlockToken;

  nlohmann::json ToJson() {
    return {{"user", user}, {"program", program}, {"unlockToken", unlockToken}};
  }
};

struct PacketUnlockResponse {
  std::string error;
  std::string encData;
  std::string tsIP{};

  static std::optional<PacketUnlockResponse> FromJson(const std::string &jsonStr) {
    try {
      auto json = nlohmann::json::parse(jsonStr);
      auto packet = PacketUnlockResponse();
      packet.error = json["error"];
      packet.encData = json["encData"];
      packet.tsIP = JsonTsIP(json);
      return packet;
    } catch(...) {
    }
    return {};
  }
};

struct PacketUnlockResponseData {
  std::string unlockToken;
  std::string passwordKey;

  static std::optional<PacketUnlockResponseData> FromJson(const std::string &jsonStr) {
    try {
      auto json = nlohmann::json::parse(jsonStr);
      auto packet = PacketUnlockResponseData();
      packet.unlockToken = json["unlockToken"];
      packet.passwordKey = json["passwordKey"];
      return packet;
    } catch(...) {
    }
    return {};
  }
};

struct PacketUDPBroadcast {
  std::string deviceId;
  std::string pcbuIP;
  uint16_t pcbuPort;
  bool isManual;
  std::string tsIP{};

  nlohmann::json ToJson() {
    nlohmann::json json = {{"deviceId", deviceId}, {"pcbuIP", pcbuIP}, {"pcbuPort", pcbuPort}, {"isManual", isManual}};
    SetJsonTsIP(json, tsIP);
    return json;
  }
};

struct PacketUDPPairBeacon {
  std::string serverId;
  std::string ip;
  uint16_t port;

  nlohmann::json ToJson() {
    return {{"serverId", serverId}, {"ip", ip}, {"port", port}};
  }
};

struct PacketLockRequest {
  std::string protoVersion{};
  std::string deviceId{};
  std::string encData{};
  std::string tsIP{};

  static std::optional<PacketLockRequest> FromJson(const std::string &jsonStr) {
    try {
      auto json = nlohmann::json::parse(jsonStr);
      auto packet = PacketLockRequest();
      packet.protoVersion = json["protoVersion"];
      packet.deviceId = json["deviceId"];
      packet.encData = json["encData"];
      packet.tsIP = JsonTsIP(json);
      return packet;
    } catch(...) {
    }
    return {};
  }
};

struct PacketLockRequestData {
  std::string pcDeviceId{};
  std::string nonce{};
  std::string action{};

  static std::optional<PacketLockRequestData> FromJson(const std::string &jsonStr) {
    try {
      auto json = nlohmann::json::parse(jsonStr);
      auto packet = PacketLockRequestData();
      packet.pcDeviceId = json["pcDeviceId"];
      packet.nonce = json["nonce"];
      if(json.contains("action") && json["action"].is_string())
        packet.action = json["action"].get<std::string>();
      return packet;
    } catch(...) {
    }
    return {};
  }
};

struct PacketLockResponse {
  std::string error{};
  std::string encData{};

  nlohmann::json ToJson() {
    return {{"error", error}, {"encData", encData}};
  }
};

struct PacketLockResponseData {
  std::string nonce{};

  nlohmann::json ToJson() {
    return {{"nonce", nonce}};
  }
};

#endif // PCBU_DESKTOP_PACKETS_H
