package com.portablefileserver.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.portablefileserver.data.PrefsManager
import com.portablefileserver.ui.MainActivity
import java.io.File

class ServerService : Service() {

    companion object {
        const val ACTION_START = "com.portablefileserver.START"
        const val ACTION_STOP = "com.portablefileserver.STOP"
        private const val CHANNEL = "pfs_server"
        private const val NOTIF_ID = 1
        private const val WAKE_LOCK_TIMEOUT_MS = 24L * 60L * 60L * 1000L
    }

    private var http: MyHttpServer? = null
    private var hotspot: HotspotManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var starting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "الخادم", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // يجب استدعاء startForeground فوراً وإلا يُغلق التطبيق (السبب الأشهر للخروج المفاجئ)
        if (!goForeground("جارٍ التشغيل…")) {
            ServerRuntime.active = false
            ServerRuntime.set(ServerRuntime.State.ERROR, "تعذّر تشغيل الخدمة (صلاحية الإشعارات؟)")
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_STOP) {
            stopAll()
            stopSelf()
            return START_NOT_STICKY
        }
        if (http == null && !starting) startAll()
        return START_STICKY
    }

    private fun goForeground(text: String): Boolean {
        return try {
            val n = buildNotification(text)
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(NOTIF_ID, n)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ServerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Max Share")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "إيقاف", stop).build())
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification(text))
        } catch (ignored: Exception) {
        }
    }

    private fun startAll() {
        starting = true
        ServerRuntime.active = true
        ServerRuntime.info = null
        ServerRuntime.url = null
        ServerRuntime.resetStats()
        ServerRuntime.set(ServerRuntime.State.STARTING, "جارٍ إنشاء الشبكة…")

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pfs:server")
                .apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
        } catch (ignored: Exception) {
        }

        val prefs = PrefsManager(this)
        val hs = HotspotManager(this)
        hotspot = hs
        hs.start(prefs.networkName, prefs.password) { info, err ->
            if (info == null) {
                starting = false
                stopAll()
                ServerRuntime.set(ServerRuntime.State.ERROR, err ?: "فشل إنشاء الشبكة")
                stopSelf()
            } else {
                // فتح المقبس خارج الخيط الرئيسي
                Thread { startHttp(prefs, info) }.start()
            }
        }
    }

    private fun startHttp(prefs: PrefsManager, info: HotspotManager.Info) {
        try {
            val root = File(prefs.rootPath)
            if (!root.exists()) root.mkdirs()
            val logo = File(cacheDir, "pfs-brand.png")
            resources.openRawResource(com.portablefileserver.R.drawable.max_share_icon).use { input ->
                logo.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            }
            var server: MyHttpServer? = null
            var port = prefs.port
            var lastErr: Exception? = null
            for (i in 0 until 10) {
                try {
                    // الارتباط بعنوان الشبكة الخاصة فقط
                    val s = MyHttpServer(
                        info.ip, port + i, root, prefs.allowUpload, prefs.allowDelete, logo,
                        assets, ApkIconInspector(this)
                    )
                    s.start(30000, false)
                    server = s
                    port += i
                    break
                } catch (e: Exception) {
                    lastErr = e
                }
            }
            if (server == null) {
                throw lastErr ?: IllegalStateException("no port")
            }
            http = server
            val url = "http://" + info.ip + ":" + port
            ServerRuntime.info = info
            ServerRuntime.url = url
            ServerRuntime.set(ServerRuntime.State.RUNNING, "يعمل")
            updateNotification(info.ssid + " — " + url)
        } catch (e: Exception) {
            stopAll()
            ServerRuntime.set(ServerRuntime.State.ERROR, "فشل تشغيل الخادم: " + (e.message ?: e.javaClass.simpleName))
            stopSelf()
        } finally {
            starting = false
        }
    }

    private fun stopAll() {
        try { http?.stop() } catch (ignored: Exception) { }
        http = null
        try { hotspot?.stop() } catch (ignored: Exception) { }
        hotspot = null
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (ignored: Exception) { }
        wakeLock = null
        val wasActive = ServerRuntime.active
        ServerRuntime.active = false
        ServerRuntime.info = null
        ServerRuntime.url = null
        if (wasActive) ServerRuntime.set(ServerRuntime.State.STOPPED, "متوقف") else ServerRuntime.changed()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }
}
