import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import JarvisAuth

Rectangle {
    id: stepFormRect
    anchors.fill: parent
    color: Theme.night

    property string description: "No description."
    property alias backTitle: stepButtons.backTitle
    property alias nextTitle: stepButtons.nextTitle
    property alias backBtn: stepButtons.backBtn
    property alias nextBtn: stepButtons.nextBtn

    ColumnLayout {
        anchors.fill: parent
        anchors.leftMargin: 20
        anchors.rightMargin: 20
        Label {
            Layout.fillWidth: true
            Layout.topMargin: 8
            Layout.bottomMargin: 16
            id: stepDescLabel
            text: stepFormRect.description
            wrapMode: Label.WordWrap
            color: Theme.paper
            font.pixelSize: 16
        }
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            id: stepFormContent
        }
        StepButtons {
            id: stepButtons
            onBackClicked: PairingForm.OnBackClicked(viewLoader, window)
            onNextClicked: PairingForm.OnNextClicked(viewLoader, window)
        }
    }

    default property alias content: stepFormContent.children
}
