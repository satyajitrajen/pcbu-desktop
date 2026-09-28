package com.jarvisauth.app.transport

import java.net.InetAddress
import java.net.InetSocketAddress

/** Unlock TCP/UDP listeners bind all IPv4 interfaces, including Tailscale 100.x. */
internal fun allIpv4Interfaces(port: Int): InetSocketAddress =
    InetSocketAddress(InetAddress.getByName("0.0.0.0"), port)
