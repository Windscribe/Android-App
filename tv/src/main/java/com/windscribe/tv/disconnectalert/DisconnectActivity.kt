/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.disconnectalert

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.tv.R
import java.util.Timer
import java.util.TimerTask

class DisconnectActivity : AppCompatActivity() {
    @JvmField
    @BindView(R.id.disconnect_alert_content)
    var disconnectAlert: TextView? = null

    @JvmField
    @BindView(R.id.title)
    var titleView: TextView? = null

    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disconnect)
        ButterKnife.bind(this)
        disconnectAlert?.text = intent.getStringExtra("message")
        titleView?.text = intent.getStringExtra("title")
        val mHandler = Handler()
        timer = Timer()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                mHandler.post { finish() }
            }
        }
        timer?.schedule(timerTask, 7000)
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    @OnClick(R.id.disconnect_alert_ok)
    fun onOkClicked() {
        finish()
    }

    companion object {
        @JvmStatic
        fun getIntent(context: Context?, message: String?, title: String?): Intent {
            val intent = Intent(context, DisconnectActivity::class.java)
            intent.putExtra("message", message)
            intent.putExtra("title", title)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}
