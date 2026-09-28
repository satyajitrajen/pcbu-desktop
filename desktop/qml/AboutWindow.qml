import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window
import JarvisAuth
import 'qrc:/ui/base'

BenchWindow {
    id: aboutWindow
    width: 800
    height: 600
    title: QI18n.Get('about')
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
                text: MainWindow.GetAppName()
                color: Theme.mute
                font.pixelSize: 12
            }
            Label {
                text: '%1 %2'.arg(QI18n.Get('version')).arg(UpdaterWindow.GetAppVersion())
                color: Theme.paper
                font.pixelSize: 22
                font.weight: Font.DemiBold
            }
            Label {
                Layout.topMargin: 16
                text: QI18n.Get('licenses')
                color: Theme.mute
                font.pixelSize: 12
            }
            ScrollView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                TextArea {
                    readOnly: true
                    color: Theme.paper
                    font.family: Theme.monoFont
                    font.pixelSize: 12
                    text: MainWindow.GetLicenseText()
                    background: Rectangle { color: Theme.steel }
                }
            }
        }
    }
}
