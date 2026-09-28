import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import JarvisAuth
import 'qrc:/ui/base'

StepForm {
    description: QI18n.Get('pairing_form_qr_desc')
    nextTitle: QI18n.Get('main_menu')
    RowLayout {
        Layout.fillWidth: true
        Layout.fillHeight: true
        spacing: 24
        Image {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.alignment: Qt.AlignCenter
            fillMode: Image.PreserveAspectFit
            source: PairingForm.GetQRImage()
            sourceSize: Qt.size(qrImage.sourceSize.width * 5, qrImage.sourceSize.height * 5)
            Image {
                id: qrImage
                source: parent.source
                width: 0
                height: 0
            }
        }
        ColumnLayout {
            Layout.preferredWidth: 280
            Layout.fillHeight: true
            Label {
                text: QI18n.Get('pairing_code')
                color: Theme.mute
                font.pixelSize: 12
            }
            ScrollView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                TextArea {
                    readOnly: true
                    wrapMode: Text.Wrap
                    color: Theme.paper
                    font.family: Theme.monoFont
                    font.pixelSize: 14
                    text: PairingForm.GetPairingCode()
                    background: Rectangle { color: Theme.steel }
                }
            }
        }
    }
}
