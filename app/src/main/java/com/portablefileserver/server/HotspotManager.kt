package com.portablefileserver.server

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * ينشئ شبكة خاصة بالتطبيق بغض النظر عن اتصال الهاتف بالإنترنت أو بشبكة واي فاي أخرى.
 *
 * الطريقة الأساسية: مجموعة Wi-Fi Direct (الهاتف يصبح Group Owner) باسم وكلمة مرور
 * يحددهما المستخدم، ويتصل بها أي جهاز كشبكة واي فاي عادية.
 * اسم الشبكة يجب أن يبدأ بـ DIRECT-xy- بحسب مواصفة Wi-Fi Direct، لذلك يصبح DIRECT-PF-<الاسم>.
 *
 * الاحتياط: LocalOnlyHotspot (أندرويد لا يسمح فيه بتحديد الاسم/الكلمة، يُنشئها النظام).
 */
@SuppressLint("MissingPermission")
class HotspotManager(context: Context) {

    data class Info(val ssid: String, val password: String, val ip: String, val mode: String)

    private val ctx = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var p2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    @Volatile private var finished = false

    companion object {
        fun buildSsid(name: String): String {
            val clean = name.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' }
                .ifEmpty { "Share" }
            return ("DIRECT-PF-" + clean).take(32)
        }
    }

    /** يستدعي cb مرة واحدة: إما (Info, null) عند النجاح أو (null, رسالة خطأ). */
    fun start(name: String, password: String, cb: (Info?, String?) -> Unit) {
        finished = false
        if (Build.VERSION.SDK_INT >= 29) startP2p(buildSsid(name), password, cb) else startLocalOnly(cb)
    }

    private fun deliver(cb: (Info?, String?) -> Unit, info: Info?, err: String?) {
        if (finished) return
        finished = true
        cb(info, err)
    }

    @RequiresApi(29)
    private fun startP2p(ssid: String, pass: String, cb: (Info?, String?) -> Unit) {
        val mgr = ctx.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        val ch = mgr?.initialize(ctx, Looper.getMainLooper(), null)
        if (mgr == null || ch == null) {
            startLocalOnly(cb)
            return
        }
        p2pManager = mgr
        channel = ch

        // نطاق 5 GHz أسرع لنقل الملفات الكبيرة؛ إذا لم يدعمه الهاتف نرجع تلقائياً إلى 2.4 GHz.
        createGroupAtBand(mgr, ch, ssid, pass, cb, 0)
    }

    @RequiresApi(29)
    private fun createGroupAtBand(
        mgr: WifiP2pManager,
        ch: WifiP2pManager.Channel,
        ssid: String,
        pass: String,
        cb: (Info?, String?) -> Unit,
        bandIndex: Int
    ) {
        if (finished) return
        val bands = intArrayOf(WifiP2pConfig.GROUP_OWNER_BAND_5GHZ, WifiP2pConfig.GROUP_OWNER_BAND_2GHZ)
        if (bandIndex >= bands.size) {
            removeGroupThen(ch) { startLocalOnly(cb) }
            return
        }
        val create = {
            if (!finished) {
                try {
                    val cfg = WifiP2pConfig.Builder()
                        .setNetworkName(ssid)
                        .setPassphrase(pass)
                        .enablePersistentMode(false)
                        .setGroupOperatingBand(bands[bandIndex])
                        .build()
                    mgr.createGroup(ch, cfg, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            handler.postDelayed({ readGroup(mgr, ch, ssid, pass, cb, bandIndex, 0) }, 800)
                        }

                        override fun onFailure(reason: Int) {
                            createGroupAtBand(mgr, ch, ssid, pass, cb, bandIndex + 1)
                        }
                    })
                } catch (e: Exception) {
                    createGroupAtBand(mgr, ch, ssid, pass, cb, bandIndex + 1)
                }
            }
        }
        removeGroupThen(ch, create)
    }

    private fun removeGroupThen(ch: WifiP2pManager.Channel, next: () -> Unit) {
        try {
            p2pManager?.removeGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { next() }
                override fun onFailure(reason: Int) { next() }
            })
        } catch (e: Exception) {
            next()
        }
    }

    @RequiresApi(29)
    private fun readGroup(
        mgr: WifiP2pManager, ch: WifiP2pManager.Channel, ssid: String, pass: String,
        cb: (Info?, String?) -> Unit, bandIndex: Int, tries: Int
    ) {
        try {
            mgr.requestGroupInfo(ch) { g ->
                if (g != null && g.isGroupOwner) {
                    val ip = ipOf(g.`interface`) ?: "192.168.49.1"
                    val band = if (bandIndex == 0) "5 GHz" else "2.4 GHz"
                    deliver(cb, Info(g.networkName, g.passphrase ?: pass, ip, "Wi-Fi Direct — $band"), null)
                } else if (tries < 6) {
                    handler.postDelayed({ readGroup(mgr, ch, ssid, pass, cb, bandIndex, tries + 1) }, 700)
                } else {
                    createGroupAtBand(mgr, ch, ssid, pass, cb, bandIndex + 1)
                }
            }
        } catch (e: Exception) {
            createGroupAtBand(mgr, ch, ssid, pass, cb, bandIndex + 1)
        }
    }

    private fun startLocalOnly(cb: (Info?, String?) -> Unit) {
        if (finished) return
        try {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wm.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(r: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = r
                    val ssid: String
                    val pass: String
                    if (Build.VERSION.SDK_INT >= 30) {
                        val c = r.softApConfiguration
                        ssid = c.ssid ?: ""
                        pass = c.passphrase ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        val c = r.wifiConfiguration
                        ssid = c?.SSID ?: ""
                        pass = c?.preSharedKey ?: ""
                    }
                    deliver(cb, Info(ssid.trim('"'), pass.trim('"'), apIp() ?: "192.168.43.1", "Local-Only Hotspot"), null)
                }

                override fun onFailed(reason: Int) {
                    deliver(cb, null, "تعذّر إنشاء الشبكة (رمز " + reason + "). تأكد أن الواي فاي مفعّل وأن الموقع/الأجهزة القريبة مسموحة.")
                }
            }, handler)
        } catch (e: Exception) {
            deliver(cb, null, "تعذّر إنشاء الشبكة: " + (e.message ?: e.javaClass.simpleName))
        }
    }

    fun stop() {
        try { reservation?.close() } catch (e: Exception) { }
        reservation = null
        val m = p2pManager
        val c = channel
        if (m != null && c != null) {
            try { m.removeGroup(c, null) } catch (e: Exception) { }
            if (Build.VERSION.SDK_INT >= 27) {
                try { c.close() } catch (e: Exception) { }
            }
        }
        p2pManager = null
        channel = null
    }

    private fun ipOf(name: String?): String? {
        if (name == null) return null
        return try {
            NetworkInterface.getByName(name)?.inetAddresses?.toList()
                ?.firstOrNull { it is Inet4Address }?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    /** يبحث عن عنوان واجهة نقطة الاتصال (وليس بيانات الجوال). */
    private fun apIp(): String? {
        return try {
            val all = NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            val preferred = all.filter {
                val n = it.name.lowercase()
                n.startsWith("ap") || n.startsWith("swlan") || n.contains("p2p") || n == "wlan1"
            }
            (preferred + all.filter { !it.name.lowercase().let { n -> n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("v4-") } })
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
