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
        spacing: 12
        Label {
            id: loadProgressLabel
            color: Theme.paper
            font.pixelSize: 16
            text: QI18n.Get('please_wait')
        }
        ProgressBar {
            Layout.fillWidth: true
            id: loadProgressBar
            indeterminate: true
            to: 100
        }
        TextArea {
            Layout.fillWidth: true
            Layout.fillHeight: true
            id: loadOutputTextArea
            readOnly: true
            color: Theme.paper
            font.family: Theme.monoFont
            font.pixelSize: 12
            background: Rectangle { color: Theme.steel }
        }
        Button {
            id: loadOkButton
            Layout.fillWidth: true
            Layout.preferredHeight: 48
            highlighted: true
            text: "Ok"
            enabled: false
            onClicked: MainWindow.Show(viewLoader)
        }
    }

    property alias progressLbl: loadProgressLabel
    property alias progressBar: loadProgressBar
    property alias okBtn: loadOkButton
    property alias outputTxtArea: loadOutputTextArea
}
