package com.minichain.minichainsvideostreaming

import android.util.Log

object Log {
    const val LOG_TAG = "MinichainsVideoStreamingLog"

    fun l(string: String?) {
        Log.v(LOG_TAG, string!!)
    }

    fun e(string: String?) {
        Log.e(LOG_TAG, string!!)
    }
}