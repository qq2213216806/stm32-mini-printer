package com.embeded.miniprinter.tools

object ByteArraryUtils {

    fun byteArr2HexString(data: ByteArray?, split: Char): String? {
        if (data == null) return "null"
        val sb = StringBuilder()
        sb.setLength(0)
        for (d in data) {
            val hex = Integer.toHexString(0xFF and d.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
            sb.append(split)
        }
        return sb.toString()
    }

}