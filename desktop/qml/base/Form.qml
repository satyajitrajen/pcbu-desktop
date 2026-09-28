import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import JarvisAuth

Rectangle {
    id: formRect
    anchors.fill: parent
    color: Theme.night
    default property alias content: formRect.data
}
