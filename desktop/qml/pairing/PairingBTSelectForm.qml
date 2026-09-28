import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts
import JarvisAuth
import 'qrc:/ui/base'

StepForm {
    property alias selectBTList: selectBTDeviceList
    property alias selectBTListModel: selectBTDeviceListModel
    description: QI18n.Get('pairing_form_bt_select_desc')
    nextBtn.enabled: false
    Rectangle {
        Layout.fillWidth: true
        Layout.fillHeight: true
        color: Theme.night
        ColumnLayout {
            anchors.fill: parent
            spacing: 16
            RowLayout {
                BusyIndicator {
                    running: true
                    scale: 0.75
                    Material.accent: Theme.brass
                }
                Label {
                    text: QI18n.Get('pairing_form_bt_scanning')
                    color: Theme.paper
                    font.pixelSize: 14
                }
            }
            ListView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                id: selectBTDeviceList
                model: ListModel {
                    id: selectBTDeviceListModel
                }
                spacing: 0
                currentIndex: -1
                delegate: ItemDelegate {
                    width: selectBTDeviceList.width
                    height: 64
                    background: Rectangle {
                        anchors.fill: parent
                        color: selectBTDeviceList.currentIndex === index ? Theme.steel : "transparent"
                    }
                    contentItem: Row {
                        anchors.fill: parent
                        Label {
                            anchors.verticalCenter: parent.verticalCenter
                            text: name
                            color: Theme.paper
                            font.pixelSize: 16
                        }
                        MouseArea {
                            anchors.fill: parent
                            onClicked: selectBTDeviceList.currentIndex = index
                        }
                    }
                }
                focus: true
                onCurrentItemChanged: {
                    let item = model.get(selectBTDeviceList.currentIndex);
                    let data = PairingForm.GetData();
                    data.bluetoothAddress = item.address;
                    PairingForm.SetData(data);
                    nextBtn.enabled = true;
                }
            }
        }
    }
}
