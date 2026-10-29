package com.realtimechat.core.model

import android.util.Log

object SafeLog {
    fun d(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] ERROR: $msg ${tr?.message ?: ""}")
        }
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.w(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] WARN: $msg ${tr?.message ?: ""}")
        }
    }
}
