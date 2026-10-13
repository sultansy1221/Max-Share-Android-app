package com.portablefileserver

import android.app.Application
import java.io.File

/** يسجّل أي انهيار في ملف crash.log حتى يمكن معرفة سبب الخروج المفاجئ. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                File(filesDir, "crash.log").writeText(
                    "thread=" + thread.name + "\n" + android.util.Log.getStackTraceString(error)
                )
            } catch (ignored: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
