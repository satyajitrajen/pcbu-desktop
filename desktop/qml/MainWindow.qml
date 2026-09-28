import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window
import QtQuick.Dialogs
import JarvisAuth
import 'qrc:/ui/base'

BenchWindow {
    id: window
    width: 1024
    height: 768
    minimumWidth: 900
    minimumHeight: 640
    visible: true
    title: MainWindow.GetAppName()

    property bool canClose: true
    property bool quitting: false
    property bool trayAvailable: false
    property bool serviceInstalled: false
    property bool hasPairing: false
    onClosing: function(close) {
        if (!window.canClose) {
            close.accepted = false
            return
        }
        if (window.quitting || !window.trayAvailable) {
            close.accepted = true
            return
        }
        close.accepted = false
        window.hide()
    }

    function refreshStatus() {
        serviceInstalled = MainWindow.IsInstalled();
        hasPairing = MainWindow.IsPaired();
    }

    ColumnLayout {
        anchors.fill: parent
        spacing: 0
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 2
            color: window.serviceInstalled ? Theme.brass : Theme.hairline
        }
        RowLayout {
            Layout.fillWidth: true
            Layout.leftMargin: 20
            Layout.rightMargin: 20
            Layout.topMargin: 16
            Layout.bottomMargin: 8
            ColumnLayout {
                spacing: 4
                Label {
                    text: MainWindow.GetAppName()
                    color: Theme.mute
                    font.pixelSize: 12
                }
                Label {
                    text: !window.serviceInstalled
                          ? QI18n.Get('not_installed')
                          : (window.hasPairing ? QI18n.Get('paired') : QI18n.Get('not_paired'))
                    color: window.serviceInstalled ? Theme.brass : Theme.paper
                    font.pixelSize: 22
                    font.weight: Font.DemiBold
                }
            }
            Item { Layout.fillWidth: true }
            Label {
                text: 'Version %1'.arg(UpdaterWindow.GetAppVersion())
                color: Theme.mute
                font.pixelSize: 12
                font.family: Theme.monoFont
            }
        }
        Loader {
            id: viewLoader
            Layout.fillWidth: true
            Layout.fillHeight: true
            source: "qrc:/ui/forms/MainForm.qml"
            onLoaded: {
                if (item) {
                    item.anchors.fill = viewLoader
                }
            }
            onStatusChanged: {
                if (status === Loader.Error)
                    console.warn("Failed to load", source)
            }
            onSourceChanged: window.refreshStatus()
        }
    }

    property var onMessageDialogAccept: function () {};
    property var onMessageDialogConfirmAccept: function () {};
    MessageDialog {
        id: messageDialog
        title: 'Title'
        text: 'Text'
        onAccepted: onMessageDialogAccept()
        onRejected: onMessageDialogAccept()
    }
    MessageDialog {
        id: confirmMessageDialog
        title: QI18n.Get('confirm');
        text: 'Text'
        buttons: MessageDialog.Ok | MessageDialog.Cancel
        onAccepted: onMessageDialogConfirmAccept()
    }
    Dialog {
        id: skipPasswordCheckDialog
        title: QI18n.Get('error')
        anchors.centerIn: Overlay.overlay
        width: Math.min(window.width - 200, 600)
        modal: true
        standardButtons: Dialog.Ok
        property string message: ''
        onOpened: skipPasswordCheckBox.checked = false
        ColumnLayout {
            width: parent.width
            Label {
                Layout.fillWidth: true
                wrapMode: Text.WordWrap
                text: skipPasswordCheckDialog.message
            }
            CheckBox {
                id: skipPasswordCheckBox
                text: QI18n.Get('skip_password_check')
            }
        }
        onAccepted: {
            if(!skipPasswordCheckBox.checked)
                return;
            PairingForm.SetSkipPasswordCheck(true);
            PairingForm.OnNextClicked(viewLoader, window);
        }
    }

    function showFatalErrorMessage(text) {
        messageDialog.title = QI18n.Get('error');
        messageDialog.text = text;
        messageDialog.visible = true;
        onMessageDialogAccept = function() { Qt.exit(1); }
    }
    function showErrorMessage(text) {
        messageDialog.title = QI18n.Get('error');
        messageDialog.text = text;
        messageDialog.visible = true;
    }
    function showConfirmMessage(text, onAccept) {
        confirmMessageDialog.text = text;
        confirmMessageDialog.visible = true;
        onMessageDialogConfirmAccept = onAccept;
    }
    function showSkipPasswordCheckMessage(text) {
        skipPasswordCheckDialog.message = text;
        skipPasswordCheckDialog.open();
    }

    function showLoadingScreen(text) {
        viewLoader.source = "qrc:/ui/forms/LoadingForm.qml";
        viewLoader.item.okBtn.enabled = false;
        viewLoader.item.progressLbl.text = text;
        window.canClose = false;
    }
    function appendLoadingOutput(text) {
        viewLoader.item.outputTxtArea.text += text + '\n';
    }
    function finishLoadingScreen(text) {
        window.canClose = true;
        viewLoader.item.okBtn.enabled = true;
        viewLoader.item.progressBar.indeterminate = false;
        viewLoader.item.progressBar.value = 100;
        viewLoader.item.progressLbl.text = text;
        window.refreshStatus();
    }

    function updateBluetoothDeviceList(devices) {
        let selItemName = undefined;
        try {
            selItemName = viewLoader.item.selectBTListModel.get(viewLoader.item.selectBTList.currentIndex).name;
        } catch (e) {}
        let selIdx = -1;
        viewLoader.item.selectBTListModel.clear();
        for(let i = 0; i < devices.length; i++) {
            viewLoader.item.selectBTListModel.append({name: devices[i].name, address: devices[i].address});
            if(devices[i].name === selItemName)
                selIdx = i;
        }
        viewLoader.item.selectBTList.currentIndex = selIdx;
    }
    function finishBluetoothPairing(isSuccess) {
        if(!isSuccess) {
            showErrorMessage(QI18n.Get('error_bluetooth_pairing'));
            PairingForm.OnBackClicked(viewLoader, window);
            return;
        }
        PairingForm.OnNextClicked(viewLoader, window);
    }

    function showUpdaterWindow() {
        let updaterWin = Qt.createComponent("qrc:/ui/UpdaterWindow.qml").createObject(window);
        updaterWin.show();
    }

    Component.onCompleted: {
        window.refreshStatus();
        MainWindow.PerformStartupChecks(viewLoader, window);
    }
}
