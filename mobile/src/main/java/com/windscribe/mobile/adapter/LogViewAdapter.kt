package com.windscribe.mobile.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.mobile.R
import com.windscribe.mobile.holder.LogViewHolder

class LogViewAdapter(private val logs : List<String>): RecyclerView.Adapter<LogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.debug_item_layout, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[holder.adapterPosition]
        holder.bind(log)
        holder.itemView.setOnLongClickListener {
            if(logs.isNotEmpty()){
                copyToClipBoard(it.context, logs)
            }
            return@setOnLongClickListener true
        }
    }
    private fun copyToClipBoard(context: Context, logs : List<String> ){
        val clipboard: ClipboardManager = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("DebugLog", logs.joinToString(separator = "\n"))
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount(): Int {
       return logs.size
    }
}