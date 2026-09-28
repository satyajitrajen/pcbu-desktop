import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window
import JarvisAuth
import 'qrc:/ui/base'

BenchWindow {
    id: logsWindow
    width: 800
    height: 600
    title: QI18n.Get('logs')
    ColumnLayout {
        anchors.fill: parent
        spacing: 0
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 2
            color: Theme.hairline
        }
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 20
            spacing: 8
            Label {
                text: QI18n.Get('desktop_logs')
                color: Theme.mute
                font.pixelSize: 12
            }
            ScrollView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                TextArea {
                    id: desktopLogTextArea
                    readOnly: true
                    wrapMode: Text.WordWrap
                    color: Theme.paper
                    font.family: Theme.monoFont
                    font.pixelSize: 12
                    text: QI18n.Get('loading')
                    background: Rectangle { color: Theme.steel }
                }
            }
            Label {
                Layout.topMargin: 12
                text: QI18n.Get('module_logs')
                color: Theme.mute
                font.pixelSize: 12
            }
            ScrollView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                TextArea {
                    id: moduleLogTextArea
                    readOnly: true
                    wrapMode: Text.WordWrap
                    color: Theme.paper
                    font.family: Theme.monoFont
                    font.pixelSize: 12
                    text: QI18n.Get('loading')
                    background: Rectangle { color: Theme.steel }
                }
            }
        }
    }
    Component.onCompleted: {
        LogsWindow.LoadLogs(logsWindow);
    }

    function setLogs(desktopLogs, moduleLogs) {
        desktopLogTextArea.text = desktopLogs;
        moduleLogTextArea.text = moduleLogs;
        desktopLogTextArea.cursorPosition = desktopLogTextArea.length;
        moduleLogTextArea.cursorPosition = moduleLogTextArea.length;
    }
}
