package com.base.find_phone_clap_detector.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.base.find_phone_clap_detector.databinding.LayoutSoundsCreatedBinding
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass

class CreatedSoundsAdapter(
    private val context: Context,
    private var soundsList: ArrayList<SoundsDataClass>,
    private val onDeleteSound: (position: Int, sound: SoundsDataClass) -> Unit,
    private val onApplySound: (position: Int, sound: SoundsDataClass) -> Unit
) : RecyclerView.Adapter<CreatedSoundsAdapter.MyHolder>() {

    inner class MyHolder(val binding: LayoutSoundsCreatedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = LayoutSoundsCreatedBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val sound = soundsList[position]
        val binding = holder.binding

        // Bind sound data
        binding.soundTv.text = sound.title
        binding.soundTime.text = getAudioDuration(context, sound.audioUri)

        // Load image
        Glide.with(context)
            .load(sound.img)
            .into(binding.soundImage)

        binding.applySoundBtn.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onApplySound(pos, soundsList[pos])
            }
        }

        binding.deleteSound.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteSound(pos, soundsList[pos])
            }
        }
    }

    override fun getItemCount(): Int = soundsList.size


    private fun getAudioDuration(context: Context, uriString: String): String {
        return try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(context, Uri.parse(uriString))
            mediaPlayer.prepare()
            val durationInMillis = mediaPlayer.duration
            mediaPlayer.release()
            val minutes = (durationInMillis / 1000) / 60
            val seconds = (durationInMillis / 1000) % 60
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }
}
