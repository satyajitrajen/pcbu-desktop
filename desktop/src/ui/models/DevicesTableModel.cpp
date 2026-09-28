#include "DevicesTableModel.h"

#include "storage/PairedDevicesStorage.h"

DevicesTableModel::DevicesTableModel(QObject *parent) : QAbstractTableModel(parent) {
  for(const auto &device : PairedDevicesStorage::GetDevices()) {
    auto pairingId = QString::fromUtf8(device.id);
    auto deviceName = QString::fromUtf8(device.deviceName);
    auto userName = QString::fromUtf8(device.userName);
    auto method = QString::fromUtf8(PairingMethodUtils::ToString(device.pairingMethod));
    m_TableData.append({pairingId, deviceName, userName, method});
  }
}

QVector<QString> DevicesTableModel::get(int rowIdx) {
  if(rowIdx < 0 || rowIdx >= m_TableData.size())
    return {};
  return m_TableData.at(rowIdx);
}

int DevicesTableModel::count() const {
  return (int)m_TableData.size();
}

int DevicesTableModel::rowCount(const QModelIndex &) const {
  return (int)m_TableData.size();
}

int DevicesTableModel::columnCount(const QModelIndex &) const {
  return !m_TableData.empty() ? (int)m_TableData.at(0).size() - 1 : 0;
}

QVariant DevicesTableModel::data(const QModelIndex &index, int role) const {
  if(!index.isValid() || index.row() < 0 || index.row() >= m_TableData.size())
    return {};
  const auto &row = m_TableData.at(index.row());
  switch(role) {
    case TableDataRole:
      return row.at(index.column() + 1);
    case PairingIdRole:
      return row.at(0);
    case NameRole:
      return row.at(1);
    case UserRole:
      return row.at(2);
    case MethodRole:
      return row.at(3);
    default:
      return {};
  }
}

QHash<int, QByteArray> DevicesTableModel::roleNames() const {
  QHash<int, QByteArray> roles{};
  roles[TableDataRole] = "tableData";
  roles[PairingIdRole] = "pairingId";
  roles[NameRole] = "deviceName";
  roles[UserRole] = "userName";
  roles[MethodRole] = "method";
  return roles;
}
