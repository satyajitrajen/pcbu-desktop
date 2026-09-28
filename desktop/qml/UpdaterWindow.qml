import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window
import QtQuick.Dialogs
import JarvisAuth
import 'qrc:/ui/base'

BenchWindow {
    id: updaterWindow
    width: 400
    height: 250
    minimumWidth: width
    minimumHeight: height
    maximumWidth: width
    maximumHeight: height
    title: QI18n.Get('updater')
    flags: Qt.Dialog
    modality: Qt.ApplicationModal

    property bool canClose: false
    onClosing: function(close) { close.accepted = updaterWindow.canClose }

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
            Label {
                text: QI18n.Get('update_available')
                color: Theme.paper
                font.pixelSize: 18
                font.weight: Font.DemiBold
            }
            Label {
                text: '%1: %2'.arg(QI18n.Get('your_version')).arg(UpdaterWindow.GetAppVersion())
                color: Theme.mute
            }
            Label {
                text: '%1: %2'.arg(QI18n.Get('latest_version')).arg(UpdaterWindow.GetLatestVersion())
                color: Theme.mute
            }
            Label {
                Layout.topMargin: 15
                id: downloadProgressLabel
                color: Theme.paper
                font.family: Theme.monoFont
                text: '0%'
            }
            ProgressBar {
                Layout.bottomMargin: 15
                Layout.fillWidth: true
                id: downloadProgressBar
                to: 100
            }
            RowLayout {
                Layout.fillWidth: true
                Item { Layout.fillWidth: true }
                Button {
                    id: ignoreButton
                    flat: true
                    text: QI18n.Get('ignore')
                    onClicked: {
                        updaterWindow.canClose = true;
                        updaterWindow.close();
                    }
                }
                Button {
                    id: downloadButton
                    highlighted: true
                    text: QI18n.Get('download')
                    onClicked: {
                        downloadButton.enabled = false;
                        ignoreButton.enabled = false;
                        UpdaterWindow.OnDownloadClicked(updaterWindow)
                    }
                }
            }
        }
    }

    MessageDialog {
        id: updaterMessageDialog
        title: QI18n.Get('error')
        text: 'Text'
    }

    function updateDownloadProgress(percent, progressText) {
        downloadProgressBar.value = percent;
        downloadProgressLabel.text = progressText;
    }
    function closeUpdaterWindow(errorText) {
        updaterWindow.canClose = true;
        updaterWindow.close();
        messageDialog.text = errorText;
        messageDialog.visible = true;
    }
}
