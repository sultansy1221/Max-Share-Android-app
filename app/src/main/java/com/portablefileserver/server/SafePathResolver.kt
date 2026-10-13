package com.portablefileserver.server

import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Resolves browser-supplied paths beneath a shared root without traversing
 * hidden entries, dot segments, or symbolic links.
 */
internal class SafePathResolver(root: File) {
    val root: File = root.canonicalFile

    fun resolve(rel: String): File? {
        val parts = rel.trimStart('/').split('/').filter { it.isNotEmpty() }
        if (parts.any { it == "." || it == ".." || it.startsWith(".") || '\u0000' in it }) return null

        val resolved = try {
            var current = root
            for (part in parts) {
                current = File(current, part)
                if (Files.isSymbolicLink(current.toPath())) return null
            }
            current.canonicalFile
        } catch (e: IOException) {
            return null
        } catch (e: SecurityException) {
            return null
        }

        val path = resolved.path
        return if (path == root.path || path.startsWith(root.path + File.separator)) resolved else null
    }
}
