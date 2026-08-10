package com.base.find_phone_clap_detector.ui.adapters

import android.app.Activity
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.ui.dataClasses.AudioModel
import com.base.find_phone_clap_detector.ui.interfaces.CallBackAudioSelection

class AudiosAdapter(
    val context: Activity,
    private val mList: List<AudioModel>,
    private val callback: CallBackAudioSelection,
    private var mediaPlayer: MediaPlayer? = null) : RecyclerView.Adapter<AudiosAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.audios_items, parent, false)
        view.isFocusable = true
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Log.d("recyclerHistoryr", "onBindViewHolder: ${mList.size}")
        if (mList.isEmpty()) {

        } else {
            val audio = mList[position]
            holder.fileName.text = audio.title
            holder.size.text = "${audio.fileSize} Mb"
            holder.duration.text = audio.duration
            holder.itemView.setOnClickListener {
                it.isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    it.isEnabled = true
                }, 750)
                callback.getItem(position, holder, audio)
            }
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val duration: TextView = itemView.findViewById(R.id.duration)
        val size: TextView = itemView.findViewById(R.id.size)
    }
}
