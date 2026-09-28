import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import JarvisAuth

Item {
    Layout.fillWidth: true
    Layout.preferredHeight: 72
    id: stepButtonsRect

    property alias backBtn: stepButtonBack
    property alias nextBtn: stepButtonNext

    property string backTitle: QI18n.Get('back')
    property string nextTitle: QI18n.Get('next')
    signal backClicked()
    signal nextClicked()

    ColumnLayout {
        anchors.fill: parent
        spacing: 0
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: Theme.hairline
        }
        RowLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.topMargin: 12
            Button {
                Layout.preferredHeight: 48
                Layout.minimumWidth: 120
                id: stepButtonBack
                flat: true
                text: stepButtonsRect.backTitle
                onClicked: stepButtonsRect.backClicked()
            }
            Item { Layout.fillWidth: true }
            Button {
                Layout.preferredHeight: 48
                Layout.minimumWidth: 160
                id: stepButtonNext
                highlighted: true
                text: stepButtonsRect.nextTitle
                onClicked: stepButtonsRect.nextClicked()
            }
        }
    }
}
