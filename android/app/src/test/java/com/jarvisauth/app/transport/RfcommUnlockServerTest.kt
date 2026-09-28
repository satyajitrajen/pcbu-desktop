package com.jarvisauth.app.transport

import org.junit.Assert.assertEquals
import org.junit.Test

class RfcommUnlockServerTest {
    @Test
    fun serviceUuidMatchesDesktopBtUnlockClient() {
        assertEquals(
            "62182bf7-97c8-45f9-aa2c-53c5f2008bdf",
            RfcommUnlockServer.SERVICE_UUID.toString(),
        )
    }

    @Test
    fun listenErrorsAreSpecific() {
        assertEquals("Turn Bluetooth on.", BluetoothListenError.Disabled.userMessage)
        assertEquals("Allow Bluetooth to listen for this PC.", BluetoothListenError.Permission.userMessage)
        assertEquals("This phone has no Bluetooth.", BluetoothListenError.Unavailable.userMessage)
        assertEquals("Bluetooth listen failed.", BluetoothListenError.Failed.userMessage)
    }
}
