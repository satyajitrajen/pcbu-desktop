package com.jarvisauth.app.protocol

object PacketIds {
    const val PAIR_INIT: Int = 0x50
    const val PAIR_RESPONSE: Int = 0x51
    const val DEVICE_ID: Int = 0xB0
    const val UNLOCK_REQUEST: Int = 0xB1
    const val UNLOCK_RESPONSE: Int = 0xB2
    const val LOCK_REQUEST: Int = 0xB3
    const val LOCK_RESPONSE: Int = 0xB4
}
