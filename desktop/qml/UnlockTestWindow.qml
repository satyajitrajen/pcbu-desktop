import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window
import QtQuick.Dialogs
import JarvisAuth
import 'qrc:/ui/base'

BenchWindow {
    property string deviceId: ""

    id: unlockTestWindow
    width: 600
    height: 320
    minimumWidth: width
    minimumHeight: height
    maximumWidth: width
    maximumHeight: height
    title: QI18n.Get('unlock_test')
    flags: Qt.Dialog
    modality: Qt.ApplicationModal

    ColumnLayout {
        anchors.fill: parent
        spacing: 0
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 2
            color: Theme.brass
        }
        Item {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Label {
                id: lblUnlockMsg
                anchors.centerIn: parent
                width: parent.width - 40
                wrapMode: Text.WordWrap
                horizontalAlignment: Text.AlignHCenter
                font.pixelSize: 22
                font.weight: Font.DemiBold
                color: Theme.paper
                text: QI18n.Get('initializing')
            }
        }
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: Theme.hairline
        }
        Button {
            id: btnUnlockAction
            Layout.fillWidth: true
            Layout.leftMargin: 20
            Layout.rightMargin: 20
            Layout.topMargin: 16
            Layout.bottomMargin: 20
            Layout.preferredHeight: 48
            highlighted: true
            text: QI18n.Get('cancel')
            onClicked: {
                if(UnlockTestWindow.IsRunning()) {
                    UnlockTestWindow.StopUnlock(unlockTestWindow)
                } else {
                    UnlockTestWindow.StartUnlock(unlockTestWindow, deviceId)
                }
            }
        }
    }

    Component.onCompleted: {
        UnlockTestWindow.StartUnlock(unlockTestWindow, deviceId)
    }
    onClosing: {
        UnlockTestWindow.StopUnlock(unlockTestWindow)
    }

    function setUnlockMessage(text) {
        lblUnlockMsg.text = text
    }
    function setUnlockButtonText(text) {
        btnUnlockAction.text = text
    }
}
