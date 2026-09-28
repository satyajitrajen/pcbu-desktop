import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import JarvisAuth
import 'qrc:/ui/base'

Form {
    ColumnLayout {
        anchors.fill: parent
        anchors.leftMargin: 20
        anchors.rightMargin: 20
        spacing: 0

        DevicesTableModel {
            id: devicesTableModel
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.preferredHeight: 48
            Label {
                text: QI18n.Get('service_module')
                color: Theme.mute
                font.pixelSize: 12
            }
            Item { Layout.fillWidth: true }
            Label {
                text: MainWindow.IsInstalled() ? QI18n.Get('installed') : QI18n.Get('not_installed')
                color: Theme.paper
                font.family: Theme.monoFont
                font.pixelSize: 13
            }
        }
        Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 1; color: Theme.hairline }
        RowLayout {
            Layout.fillWidth: true
            Layout.preferredHeight: 48
            Label {
                text: QI18n.Get('installed_version')
                color: Theme.mute
                font.pixelSize: 12
            }
            Item { Layout.fillWidth: true }
            Label {
                text: MainWindow.GetInstalledVersion()
                color: Theme.paper
                font.family: Theme.monoFont
                font.pixelSize: 13
            }
        }
        Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 1; color: Theme.hairline }

        Label {
            Layout.topMargin: 24
            Layout.bottomMargin: 8
            text: QI18n.Get('devices')
            color: Theme.mute
            font.pixelSize: 12
        }

        ListView {
            id: devicesList
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            boundsBehavior: Flickable.StopAtBounds
            model: devicesTableModel
            currentIndex: -1
            delegate: ItemDelegate {
                required property int index
                required property string pairingId
                required property string deviceName
                required property string userName
                required property string method
                width: devicesList.width
                height: 80
                highlighted: devicesList.currentIndex === index
                onClicked: {
                    devicesList.currentIndex = index;
                    deviceRemoveBtn.enabled = true;
                    deviceTestUnlockBtn.enabled = true;
                }
                background: Rectangle {
                    color: devicesList.currentIndex === index ? Theme.steel : "transparent"
                }
                contentItem: RowLayout {
                    anchors.fill: parent
                    anchors.rightMargin: 8
                    ColumnLayout {
                        spacing: 4
                        Label {
                            text: deviceName
                            color: Theme.paper
                            font.pixelSize: 22
                            font.weight: Font.DemiBold
                        }
                        Label {
                            text: userName
                            color: Theme.mute
                            font.pixelSize: 12
                        }
                    }
                    Item { Layout.fillWidth: true }
                    Label {
                        text: method
                        color: Theme.mute
                        font.family: Theme.monoFont
                        font.pixelSize: 12
                    }
                }
            }

            Label {
                visible: devicesTableModel.count() === 0
                anchors.centerIn: parent
                text: QI18n.Get('not_paired')
                color: Theme.mute
                font.pixelSize: 16
            }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 8
            Layout.bottomMargin: 8
            visible: devicesList.currentIndex !== -1
            Button {
                id: deviceRemoveBtn
                flat: true
                text: QI18n.Get('remove_device')
                enabled: false
                onClicked: {
                    showConfirmMessage(QI18n.Get('confirm_remove_device'), function () {
                        let selDevice = devicesTableModel.get(devicesList.currentIndex);
                        MainWindow.OnRemoveDeviceClicked(viewLoader, selDevice[0]);
                    });
                }
            }
            Button {
                id: deviceTestUnlockBtn
                flat: true
                text: QI18n.Get('unlock_test')
                enabled: false
                onClicked: {
                    let selDevice = devicesTableModel.get(devicesList.currentIndex);
                    let unlockWin = Qt.createComponent("qrc:/ui/UnlockTestWindow.qml").createObject(window, {deviceId: selDevice[0]});
                    unlockWin.show();
                }
            }
            Item { Layout.fillWidth: true }
        }

        Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 1; color: Theme.hairline }

        ColumnLayout {
            Layout.fillWidth: true
            Layout.topMargin: 16
            Layout.bottomMargin: 20
            spacing: 8

            Button {
                Layout.fillWidth: true
                Layout.preferredHeight: 48
                highlighted: true
                text: MainWindow.IsInstalled() ? QI18n.Get('pair_device') : QI18n.Get('install')
                onClicked: {
                    if (MainWindow.IsInstalled())
                        PairingForm.Show(viewLoader, window);
                    else
                        MainWindow.OnInstallClicked(window);
                }
            }
            RowLayout {
                Layout.fillWidth: true
                Layout.alignment: Qt.AlignHCenter
                visible: MainWindow.IsInstalled()
                Button {
                    flat: true
                    text: QI18n.Get('uninstall')
                    onClicked: MainWindow.OnInstallClicked(window)
                }
                Button {
                    flat: true
                    text: QI18n.Get('reinstall')
                    enabled: MainWindow.IsInstalled()
                    onClicked: MainWindow.OnReinstallClicked(window)
                }
                Button {
                    flat: true
                    text: QI18n.Get('settings')
                    enabled: MainWindow.IsInstalled()
                    onClicked: SettingsForm.Show(viewLoader)
                }
            }
            RowLayout {
                Layout.fillWidth: true
                Layout.alignment: Qt.AlignHCenter
                Button {
                    flat: true
                    text: QI18n.Get('about')
                    onClicked: {
                        let aboutWin = Qt.createComponent("qrc:/ui/AboutWindow.qml").createObject(window);
                        aboutWin.show();
                    }
                }
                Button {
                    flat: true
                    text: QI18n.Get('logs')
                    onClicked: {
                        let logsWin = Qt.createComponent("qrc:/ui/LogsWindow.qml").createObject(window);
                        logsWin.show();
                    }
                }
            }
        }
    }
}
