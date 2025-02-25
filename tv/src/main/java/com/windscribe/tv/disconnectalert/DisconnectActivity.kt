/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.disconnectalert

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.windscribe.tv.R
import com.windscribe.tv.databinding.ActivityDisconnectBinding
import java.util.Timer
import java.util.TimerTask

class DisconnectActivity : AppCompatActivity() {

    private var timer: Timer? = null

    private lateinit var binding: ActivityDisconnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_disconnect)
        binding.disconnectAlertContent.text = intent.getStringExtra("message")
        binding.title.text = intent.getStringExtra("title")
        val mHandler = Handler()
        timer = Timer()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                mHandler.post { finish() }
            }
        }
        timer?.schedule(timerTask, 7000)
        binding.disconnectAlertOk.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
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
