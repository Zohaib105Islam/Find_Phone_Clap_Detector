package com.base.find_phone_clap_detector.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.LayoutSoundsRvBinding
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.SoundInterface
import com.base.find_phone_clap_detector.utils.disableMultipleClicking

class SoundsAdapter(
    private val context: Context,
    private var soundsList: ArrayList<SoundsDataClass>,
    private var selectorValue: Int,
    val soundInterface: SoundInterface
) : RecyclerView.Adapter<SoundsAdapter.MyHolder>() {

    class MyHolder(val binding: LayoutSoundsRvBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            LayoutSoundsRvBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val item = soundsList[position]

        holder.binding.soundTv.text = item.title

        // Load image
        try {
            Glide.with(context)
                .asBitmap()
                .load(item.img)
                .into(holder.binding.soundImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set background based on selection
        if (position == selectorValue) {
            holder.binding.clMain.setBackgroundResource(R.drawable.bg_sound_selected)
            holder.binding.ivSelectedDot.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.clMain.setBackgroundResource(R.drawable.bg_sound_unselected)
            holder.binding.ivSelectedDot.visibility = android.view.View.GONE
        }

        holder.itemView.setOnClickListener {

            it.disableMultipleClicking(1000)

            // Always get updated position safely
            val adapterPosition = holder.bindingAdapterPosition

            if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousSelection = selectorValue
            selectorValue = adapterPosition
            if (previousSelection >= 0) {
                notifyItemChanged(previousSelection)
            }
            notifyItemChanged(selectorValue)

            val clickedItem = soundsList[adapterPosition]

            soundInterface.clickOnSoundListener(
                adapterPosition,
                clickedItem.isPremium,
                clickedItem.img,
                clickedItem.title,
                clickedItem.audioUri
            )
        }
    }

    override fun getItemCount(): Int = soundsList.size

    fun updateSelection(newPosition: Int) {
        if (newPosition == selectorValue) return

        val oldPosition = selectorValue
        selectorValue = newPosition

        notifyItemChanged(oldPosition)
        notifyItemChanged(selectorValue)
    }
}
