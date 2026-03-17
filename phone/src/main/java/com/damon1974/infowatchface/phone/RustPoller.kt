package com.damon1974.infowatchface.phone

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

object RustPoller {

    data class RustStatus(val online: Int, val max: Int)

    fun fetchStatus(host: String, port: Int = 28017, timeoutMs: Int = 5000): RustStatus? {
        return try {
            val address = InetAddress.getByName(host)
            Log.d("RustPoller", "Connecting to ${address.hostAddress}:$port")
            val socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            // Step 1: Send initial A2S_INFO request
            var response = sendAndReceive(socket, address, port, buildA2SInfo(null))
                ?: return null

            // Step 2: If challenge response (0x41), resend with challenge token
            if (response.size >= 9 && response[4] == 0x41.toByte()) {
                Log.d("RustPoller", "Got challenge, retrying with token")
                val challenge = response.copyOfRange(5, 9)
                response = sendAndReceive(socket, address, port, buildA2SInfo(challenge))
                    ?: return null
            }

            socket.close()
            Log.d("RustPoller", "Response type: 0x${response[4].toInt().and(0xFF).toString(16)}")
            parseA2SInfo(response)
        } catch (e: Exception) {
            Log.e("RustPoller", "fetchStatus failed: ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    private fun buildA2SInfo(challenge: ByteArray?): ByteArray {
        val payload = "Source Engine Query\u0000".toByteArray(Charsets.UTF_8)
        val size = 4 + 1 + payload.size + (challenge?.size ?: 0)
        val request = ByteArray(size)
        request[0] = 0xFF.toByte()
        request[1] = 0xFF.toByte()
        request[2] = 0xFF.toByte()
        request[3] = 0xFF.toByte()
        request[4] = 0x54.toByte()
        payload.copyInto(request, 5)
        challenge?.copyInto(request, 5 + payload.size)
        return request
    }

    private fun sendAndReceive(socket: DatagramSocket, address: InetAddress, port: Int, request: ByteArray): ByteArray? {
        val sendPacket = DatagramPacket(request, request.size, address, port)
        socket.send(sendPacket)
        val buffer = ByteArray(1400)
        val recvPacket = DatagramPacket(buffer, buffer.size)
        socket.receive(recvPacket)
        return buffer.copyOfRange(0, recvPacket.length)
    }

    private fun parseA2SInfo(data: ByteArray): RustStatus? {
        if (data.size < 6) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(4)
        val type = buf.get()
        if (type != 0x49.toByte()) {
            Log.e("RustPoller", "Unexpected type: 0x${type.toInt().and(0xFF).toString(16)}")
            return null
        }
        buf.get() // protocol
        skipString(buf) // name
        skipString(buf) // map
        skipString(buf) // folder
        skipString(buf) // game
        buf.short        // id
        val players = buf.get().toInt() and 0xFF
        val maxPlayers = buf.get().toInt() and 0xFF
        Log.d("RustPoller", "Parsed: players=$players max=$maxPlayers")
        return RustStatus(players, maxPlayers)
    }

    private fun skipString(buf: ByteBuffer) {
        while (buf.hasRemaining() && buf.get() != 0.toByte()) { }
    }
}
