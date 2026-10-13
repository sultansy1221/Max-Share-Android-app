package com.portablefileserver.data

import android.content.Context
import java.io.File
import java.security.SecureRandom

class PrefsManager(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("pfs", Context.MODE_PRIVATE)

    /** اسم الشبكة كما يكتبه المستخدم (يُضاف له بادئة DIRECT-PF- تلقائياً). */
    var networkName: String
        get() = sp.getString("name", "Share") ?: "Share"
        set(v) = sp.edit().putString("name", v).apply()

    /** كلمة مرور عشوائية آمنة تُنشأ مرة واحدة عند أول استخدام وتبقى محفوظة على الجهاز. */
    var password: String
        get() = synchronized(passwordLock) {
            val saved = sp.getString("pass", null)
            if (saved != null && saved != legacyDefaultPassword) {
                saved
            } else {
                generatePassword().also { sp.edit().putString("pass", it).apply() }
            }
        }
        set(v) = sp.edit().putString("pass", v).apply()

    var port: Int
        get() = sp.getInt("port", 8080)
        set(v) = sp.edit().putInt("port", v).apply()

    var rootPath: String
        get() = sp.getString("root", null) ?: defaultRoot()
        set(v) = sp.edit().putString("root", v).apply()

    /** السماح للأجهزة المتصلة برفع الملفات وإنشاء المجلدات. */
    var allowUpload: Boolean
        get() = sp.getBoolean("allow_upload", true)
        set(v) = sp.edit().putBoolean("allow_upload", v).apply()

    /** السماح للأجهزة المتصلة بحذف الملفات وإعادة تسميتها. */
    var allowDelete: Boolean
        get() = sp.getBoolean("allow_delete", false)
        set(v) = sp.edit().putBoolean("allow_delete", v).apply()

    var keepScreenOn: Boolean
        get() = sp.getBoolean("keep_screen", false)
        set(v) = sp.edit().putBoolean("keep_screen", v).apply()

    fun defaultRoot(): String {
        val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        return File(base, "Share").absolutePath
    }

    private fun generatePassword(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        return buildString(16) {
            repeat(16) { append(alphabet[secureRandom.nextInt(alphabet.length)]) }
        }
    }

    private companion object {
        const val legacyDefaultPassword = "12345678"
        val passwordLock = Any()
        val secureRandom = SecureRandom()
    }
}
