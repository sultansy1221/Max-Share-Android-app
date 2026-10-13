package com.portablefileserver.server

import android.webkit.MimeTypeMap
import android.content.res.AssetManager
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URLEncoder
import java.nio.file.Files
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * خادم HTTP للملفات. يرتبط بعنوان الشبكة الخاصة فقط (وليس بكل الواجهات)،
 * فلا تمر الملفات عبر بيانات الجوال أو راوتر الإنترنت.
 */
class MyHttpServer(
    host: String?,
    port: Int,
    root: File,
    private val allowUpload: Boolean,
    private val allowDelete: Boolean,
    private val logoFile: File,
    private val appAssets: AssetManager,
    private val apkIconInspector: ApkIconInspector
) : NanoHTTPD(host, port) {

    private val pathResolver = SafePathResolver(root)
    private val rootC: File = pathResolver.root
    private val transferBufferSize = 256 * 1024

    private fun resolve(rel: String): File? = pathResolver.resolve(rel)

    override fun serve(session: IHTTPSession): Response {
        ServerRuntime.touch(session.remoteIpAddress)
        return try {
            route(session)
        } catch (t: Throwable) {
            json(Response.Status.INTERNAL_ERROR, JSONObject().put("error", t.message ?: "error"))
        }
    }

    private fun route(session: IHTTPSession): Response {
        val uri = session.uri
        val post = session.method == Method.POST
        return when {
            uri == "/" -> newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", WebUi.HTML)
                .apply { addHeader("Cache-Control", "no-store") }
            uri == "/brand.png" -> brandLogo()
            uri.startsWith("/icons/") -> appIcon(uri.removePrefix("/icons/"))
            uri == "/api/info" -> info()
            uri == "/api/list" -> list(session)
            uri == "/api/zip" -> zip(session)
            uri.startsWith("/f/") -> download(session, uri.removePrefix("/f"))
            uri == "/api/upload" && post -> if (allowUpload) upload(session) else denied()
            uri == "/api/mkdir" && post -> if (allowUpload) mkdir(session) else denied()
            uri == "/api/delete" && post -> if (allowDelete) delete(session) else denied()
            uri == "/api/rename" && post -> if (allowDelete) rename(session) else denied()
            else -> json(Response.Status.NOT_FOUND, JSONObject().put("error", "not found"))
        }
    }

    private fun json(status: Response.IStatus, o: Any): Response {
        val r = newFixedLengthResponse(status, "application/json; charset=utf-8", o.toString())
        r.addHeader("Cache-Control", "no-store")
        return r
    }

    private fun forbidden() = json(Response.Status.FORBIDDEN, JSONObject().put("error", "forbidden"))
    private fun denied() = json(Response.Status.FORBIDDEN, JSONObject().put("error", "هذه العملية معطّلة من إعدادات التطبيق"))
    private fun bad(msg: String) = json(Response.Status.BAD_REQUEST, JSONObject().put("error", msg))
    private fun notFound() = json(Response.Status.NOT_FOUND, JSONObject().put("error", "not found"))

    private fun info(): Response {
        return json(
            Response.Status.OK,
            JSONObject().put("upload", allowUpload).put("delete", allowDelete)
                .put("free", rootC.usableSpace).put("total", rootC.totalSpace)
                .put("root", rootC.name)
        )
    }

    private fun brandLogo(): Response {
        if (!logoFile.isFile) return notFound()
        val response = newFixedLengthResponse(
            Response.Status.OK, "image/png",
            BufferedInputStream(FileInputStream(logoFile), 64 * 1024), logoFile.length()
        )
        response.addHeader("Cache-Control", "public, max-age=86400")
        return response
    }

    private fun appIcon(name: String): Response {
        if (!name.matches(Regex("[a-z0-9-]+\\.svg"))) return notFound()
        return try {
            val bytes = appAssets.open("app-icons/$name").use { it.readBytes() }
            if (bytes.size > 64 * 1024) return notFound()
            newFixedLengthResponse(
                Response.Status.OK,
                "image/svg+xml; charset=utf-8",
                bytes.inputStream(),
                bytes.size.toLong()
            ).apply { addHeader("Cache-Control", "public, max-age=604800, immutable") }
        } catch (_: IOException) {
            notFound()
        }
    }

    private fun list(session: IHTTPSession): Response {
        val rel = session.parms["path"] ?: ""
        val dir = resolve(rel) ?: return forbidden()
        if (!dir.isDirectory) return notFound()
        val arr = JSONArray()
        val items = (dir.listFiles() ?: emptyArray()).filter {
            !it.name.startsWith(".") && !Files.isSymbolicLink(it.toPath())
        }.sortedWith(
            compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })
        )
        for (f in items) {
            val item = JSONObject().put("name", f.name).put("dir", f.isDirectory)
                .put("size", if (f.isFile) f.length() else 0L).put("mtime", f.lastModified())
            if (f.isFile && f.extension.equals("apk", ignoreCase = true)) {
                apkIconInspector.inspect(f)?.let { app ->
                    item.put("applicationId", app.packageName).put("applicationLabel", app.label)
                    app.iconDataUrl?.let { item.put("appIconDataUrl", it) }
                }
            }
            arr.put(item)
        }
        return json(Response.Status.OK, JSONObject().put("path", rel).put("items", arr))
    }

    private fun mimeOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun download(session: IHTTPSession, rel: String): Response {
        val f = resolve(rel) ?: return forbidden()
        if (!f.isFile) return notFound()
        val len = f.length()
        val mime = mimeOf(f.name)
        if (len == 0L) return newFixedLengthResponse(Response.Status.OK, mime, "")

        var start = 0L
        var end = len - 1
        var partial = false
        val range = session.headers["range"]
        if (range != null && range.startsWith("bytes=")) {
            val spec = range.substring(6).split(",")[0].trim()
            val a = spec.substringBefore('-')
            val b = spec.substringAfter('-', "")
            try {
                if (a.isEmpty()) {
                    start = maxOf(0L, len - b.toLong())
                } else {
                    start = a.toLong()
                    if (b.isNotEmpty()) end = minOf(b.toLong(), len - 1)
                }
                partial = true
            } catch (e: NumberFormatException) {
                partial = false
                start = 0L
                end = len - 1
            }
            if (partial && (start > end || start >= len)) {
                val r = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "")
                r.addHeader("Content-Range", "bytes */$len")
                return r
            }
        }
        val fis = FileInputStream(f)
        if (start > 0) fis.channel.position(start)
        val count = end - start + 1
        val resp = newFixedLengthResponse(
            if (partial) Response.Status.PARTIAL_CONTENT else Response.Status.OK,
            mime, LimitedStream(BufferedInputStream(fis, transferBufferSize), count), count
        )
        resp.addHeader("Accept-Ranges", "bytes")
        if (partial) resp.addHeader("Content-Range", "bytes $start-$end/$len")
        if (session.parms["dl"] == "1") resp.addHeader("Content-Disposition", disposition(f.name))
        return resp
    }

    private fun disposition(name: String): String {
        val enc = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
        return "attachment; filename*=UTF-8''$enc"
    }

    /** تحميل مجلد أو عدة عناصر كملف zip يُبث مباشرة دون تخزين مؤقت. */
    private fun zip(session: IHTTPSession): Response {
        val paths = session.parameters["p"] ?: emptyList()
        if (paths.isEmpty()) return bad("no paths")
        val targets = ArrayList<File>()
        for (p in paths) {
            val f = resolve(p) ?: return forbidden()
            if (!f.exists()) return notFound()
            targets.add(f)
        }
        val zipName = (if (targets.size == 1) targets[0].let { if (it == rootC) rootC.name else it.name } else "files") + ".zip"
        val pin = PipedInputStream(512 * 1024)
        val pout = PipedOutputStream(pin)
        Thread {
            try {
                val z = ZipOutputStream(CountingOutput(pout))
                z.setLevel(Deflater.BEST_SPEED)
                for (t in targets) addToZip(z, t, if (t == rootC) "" else t.name)
                z.finish()
                z.close()
            } catch (e: Exception) {
                try { pout.close() } catch (ignored: Exception) { }
            }
        }.start()
        val resp = newChunkedResponse(Response.Status.OK, "application/zip", pin)
        resp.addHeader("Content-Disposition", disposition(zipName))
        return resp
    }

    private fun addToZip(z: ZipOutputStream, f: File, entry: String) {
        // Keep ZIP output consistent with the browser listing and never follow
        // links that can escape the shared directory or point back into a loop.
        if (f != rootC && f.name.startsWith(".")) return
        if (Files.isSymbolicLink(f.toPath())) return
        if (f.isDirectory) {
            val kids = f.listFiles() ?: return
            if (kids.isEmpty() && entry.isNotEmpty()) {
                z.putNextEntry(ZipEntry("$entry/"))
                z.closeEntry()
            }
            for (k in kids) addToZip(z, k, if (entry.isEmpty()) k.name else entry + "/" + k.name)
        } else if (f.isFile) {
            z.putNextEntry(ZipEntry(entry))
            BufferedInputStream(FileInputStream(f), transferBufferSize).use {
                it.copyTo(z, transferBufferSize)
            }
            z.closeEntry()
        }
    }

    private fun safeName(n: String): String? {
        val name = File(n).name.trim()
        return if (name.isEmpty() || name == "." || name == ".." || name.startsWith(".")) null else name
    }

    private fun uniqueIn(dir: File, orig: String): File {
        var dest = File(dir, orig)
        var i = 1
        while (dest.exists()) {
            val base = orig.substringBeforeLast('.', orig)
            val ext = if (orig.contains('.')) "." + orig.substringAfterLast('.') else ""
            dest = File(dir, "$base ($i)$ext")
            i++
        }
        return dest
    }

    private fun upload(session: IHTTPSession): Response {
        val contentType = session.headers["content-type"] ?: ""
        if (contentType.startsWith("multipart/form-data", ignoreCase = true)) {
            return uploadMultipartLegacy(session)
        }

        val rel = session.parms["path"] ?: ""
        val dir = resolve(rel) ?: return forbidden()
        if (!dir.isDirectory) return notFound()
        val orig = safeName(session.parms["name"] ?: session.headers["x-file-name"] ?: "")
            ?: return bad("اسم الملف غير صالح")
        val size = session.headers["content-length"]?.toLongOrNull()
            ?: return bad("حجم الملف غير محدد")
        if (size < 0L) return bad("حجم الملف غير صالح")
        val dest = uniqueIn(dir, orig)
        val staging = File(dir, ".${UUID.randomUUID()}.uploading")
        try {
            BufferedOutputStream(FileOutputStream(staging), transferBufferSize).use { out ->
                val input = session.inputStream
                val buffer = ByteArray(transferBufferSize)
                var remaining = size
                while (remaining > 0L) {
                    val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (count < 0) throw EOFException("انقطع الاتصال قبل اكتمال الملف")
                    if (count == 0) continue
                    out.write(buffer, 0, count)
                    remaining -= count
                    ServerRuntime.bytesReceived.addAndGet(count.toLong())
                }
                out.flush()
            }
            // الملف المؤقت داخل المجلد نفسه، لذا ننقله عند الاكتمال دون نسخه مرة ثانية.
            Files.move(staging.toPath(), dest.toPath())
            return json(Response.Status.OK, JSONObject().put("ok", true).put("name", dest.name))
        } finally {
            if (staging.exists()) staging.delete()
        }
    }

    /** دعم الصفحات القديمة التي ما زالت ترسل multipart بدلاً من التدفق المباشر. */
    private fun uploadMultipartLegacy(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)
        val rel = session.parms["path"] ?: ""
        val dir = resolve(rel) ?: return forbidden()
        if (!dir.isDirectory) return notFound()
        val tmp = files["file"] ?: return bad("no file")
        val orig = safeName(session.parms["file"] ?: "") ?: return bad("bad name")
        val dest = uniqueIn(dir, orig)
        val t = File(tmp)
        if (!t.renameTo(dest)) {
            t.copyTo(dest, overwrite = false, bufferSize = transferBufferSize)
        }
        ServerRuntime.bytesReceived.addAndGet(dest.length())
        return json(Response.Status.OK, JSONObject().put("ok", true).put("name", dest.name))
    }

    private fun mkdir(session: IHTTPSession): Response {
        session.parseBody(HashMap())
        val dir = resolve(session.parms["path"] ?: "") ?: return forbidden()
        val name = safeName(session.parms["name"] ?: "") ?: return bad("bad name")
        val target = File(dir, name)
        if (target.exists()) {
            return if (target.isDirectory) json(Response.Status.OK, JSONObject().put("ok", true))
            else bad("يوجد ملف بهذا الاسم")
        }
        return if (target.mkdirs()) json(Response.Status.OK, JSONObject().put("ok", true))
        else json(Response.Status.INTERNAL_ERROR, JSONObject().put("error", "mkdir failed"))
    }

    private fun delete(session: IHTTPSession): Response {
        session.parseBody(HashMap())
        val f = resolve(session.parms["path"] ?: "") ?: return forbidden()
        if (f == rootC) return forbidden()
        if (!f.exists()) return notFound()
        val ok = if (f.isDirectory) f.deleteRecursively() else f.delete()
        return json(if (ok) Response.Status.OK else Response.Status.INTERNAL_ERROR, JSONObject().put("ok", ok))
    }

    private fun rename(session: IHTTPSession): Response {
        session.parseBody(HashMap())
        val f = resolve(session.parms["path"] ?: "") ?: return forbidden()
        if (f == rootC) return forbidden()
        if (!f.exists()) return notFound()
        val name = safeName(session.parms["name"] ?: "") ?: return bad("bad name")
        val dest = File(f.parentFile, name)
        if (dest.exists()) return bad("الاسم موجود مسبقاً")
        return if (f.renameTo(dest)) json(Response.Status.OK, JSONObject().put("ok", true))
        else json(Response.Status.INTERNAL_ERROR, JSONObject().put("error", "rename failed"))
    }

    /** يعدّ البايتات المرسلة للإحصائيات. */
    private class LimitedStream(private val s: InputStream, private var left: Long) : InputStream() {
        override fun read(): Int {
            if (left <= 0) return -1
            val r = s.read()
            if (r >= 0) {
                left--
                ServerRuntime.bytesSent.incrementAndGet()
            }
            return r
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (left <= 0) return -1
            val n = s.read(b, off, minOf(len.toLong(), left).toInt())
            if (n > 0) {
                left -= n
                ServerRuntime.bytesSent.addAndGet(n.toLong())
            }
            return n
        }

        override fun available(): Int = minOf(s.available().toLong(), left).toInt()
        override fun close() { s.close() }
    }

    private class CountingOutput(private val o: OutputStream) : OutputStream() {
        override fun write(b: Int) {
            o.write(b)
            ServerRuntime.bytesSent.incrementAndGet()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            o.write(b, off, len)
            ServerRuntime.bytesSent.addAndGet(len.toLong())
        }

        override fun flush() { o.flush() }
        override fun close() { o.close() }
    }
}
