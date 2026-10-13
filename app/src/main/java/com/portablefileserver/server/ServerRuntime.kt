package com.portablefileserver.server

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** حالة التشغيل والإحصائيات المشتركة بين الخدمة والخادم والواجهة. */
object ServerRuntime {
    enum class State { STOPPED, STARTING, RUNNING, ERROR }

    @Volatile var state: State = State.STOPPED
    @Volatile var active = false
    @Volatile var info: HotspotManager.Info? = null
    @Volatile var url: String? = null
    @Volatile var status: String = "متوقف"

    val bytesSent = AtomicLong(0)
    val bytesReceived = AtomicLong(0)
    val requests = AtomicLong(0)
    private val clients = ConcurrentHashMap<String, Long>()

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun changed() { main.post { listeners.forEach { try { it() } catch (ignored: Throwable) { } } } }

    fun set(newState: State, text: String) {
        state = newState
        status = text
        changed()
    }

    fun touch(ip: String?) {
        requests.incrementAndGet()
        if (ip != null) clients[ip] = System.currentTimeMillis()
    }

    /** عدد الأجهزة التي تواصلت خلال آخر دقيقة. */
    fun activeClients(): Int {
        val now = System.currentTimeMillis()
        return clients.values.count { now - it < 60_000 }
    }

    fun resetStats() {
        bytesSent.set(0)
        bytesReceived.set(0)
        requests.set(0)
        clients.clear()
    }
}
