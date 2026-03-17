package com.damon1974.infowatchface.phone

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import org.json.JSONObject

object MinecraftPoller {

    data class McStatus(val online: Int, val max: Int)

    fun fetchStatus(host: String, port: Int = 25565, timeoutMs: Int = 5000): McStatus? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.soTimeout = timeoutMs

            val out = DataOutputStream(socket.getOutputStream())
            val inp = DataInputStream(socket.getInputStream())

            // SLP Handshake packet
            val handshake = buildHandshake(host, port)
            writeVarInt(out, handshake.size)
            out.write(handshake)

            // Status request packet (0x00 with just packet ID)
            writeVarInt(out, 1)
            writeVarInt(out, 0x00)
            out.flush()

            // Read response length, then packet ID, then JSON length, then JSON
            readVarInt(inp) // packet length
            readVarInt(inp) // packet id (0x00)
            val jsonLen = readVarInt(inp)
            val jsonBytes = ByteArray(jsonLen)
            inp.readFully(jsonBytes)
            socket.close()

            val json = JSONObject(String(jsonBytes))
            val players = json.getJSONObject("players")
            McStatus(players.getInt("online"), players.getInt("max"))
        } catch (e: Exception) {
            null
        }
    }

    private fun buildHandshake(host: String, port: Int): ByteArray {
        val buf = java.io.ByteArrayOutputStream()
        val out = DataOutputStream(buf)
        writeVarInt(out, 0x00)         // Packet ID
        writeVarInt(out, 767)          // Protocol version (1.21)
        writeVarInt(out, host.length)  // Host length
        out.writeBytes(host)           // Host
        out.writeShort(port)           // Port
        writeVarInt(out, 1)            // Next state: status
        return buf.toByteArray()
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while (true) {
            if (v and 0x7F.inv() == 0) { out.writeByte(v); return }
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun readVarInt(inp: DataInputStream): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = inp.readByte().toInt()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
    }
}
