package com.example.app_dat_xe.data.remote

data class StompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String
)

internal fun parseStompFrame(rawFrame: String): StompFrame? {
    val frame = rawFrame.trim('\r', '\n')
    if (frame.isBlank()) return null
    val lines = frame.split('\n').map { it.trimEnd('\r') }
    val command = lines.firstOrNull()?.trim().orEmpty()
    val headerEnd = lines.indexOfFirst { it.isBlank() }
    val headers = if (headerEnd > 0) {
        lines.subList(1, headerEnd).mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
        }.toMap()
    } else emptyMap()
    val body = if (headerEnd >= 0 && headerEnd + 1 < lines.size) {
        lines.subList(headerEnd + 1, lines.size).joinToString("\n")
    } else ""
    return StompFrame(command, headers, body)
}
