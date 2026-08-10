package com.base.find_phone_clap_detector.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.LayoutHomeSoundItemBinding
import com.base.find_phone_clap_detector.databinding.LayoutSoundsRvBinding
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.SoundInterface
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.bumptech.glide.Glide

class SoundsAdapter(
    private val context: Context,
    private var soundsList: ArrayList<SoundsDataClass>,
    private var selectorValue: Int,
    private val soundInterface: SoundInterface,
    private val useHomeCircularStyle: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DEFAULT = 0
        private const val VIEW_TYPE_HOME_CIRCULAR = 1
    }

    class DefaultHolder(val binding: LayoutSoundsRvBinding) :
        RecyclerView.ViewHolder(binding.root)

    class HomeCircularHolder(val binding: LayoutHomeSoundItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (useHomeCircularStyle) VIEW_TYPE_HOME_CIRCULAR else VIEW_TYPE_DEFAULT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HOME_CIRCULAR) {
            HomeCircularHolder(
                LayoutHomeSoundItemBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        } else {
            DefaultHolder(
                LayoutSoundsRvBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = soundsList[position]
        val selected = position == selectorValue

        when (holder) {
            is HomeCircularHolder -> bindHomeCircularItem(holder, item, selected)
            is DefaultHolder -> bindDefaultItem(holder, item, selected)
        }

        holder.itemView.setOnClickListener {
            it.disableMultipleClicking(1000)

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

    private fun bindHomeCircularItem(
        holder: HomeCircularHolder,
        item: SoundsDataClass,
        selected: Boolean
    ) {
        holder.binding.soundTv.text = item.title
        loadImage(item, holder.binding.soundImage)

        holder.binding.circleContainer.setBackgroundResource(
            if (selected) {
                R.drawable.home_sound_circle_selected
            } else {
                R.drawable.home_sound_circle_unselected
            }
        )
        holder.binding.ivSelectedDot.visibility = if (selected) View.VISIBLE else View.GONE
        holder.binding.soundTv.setTextColor(
            context.getColor(if (selected) R.color.primary else R.color.colorTextDark)
        )
    }

    private fun bindDefaultItem(
        holder: DefaultHolder,
        item: SoundsDataClass,
        selected: Boolean
    ) {
        holder.binding.soundTv.text = item.title
        loadImage(item, holder.binding.soundImage)

        if (selected) {
            holder.binding.clMain.setBackgroundResource(R.drawable.bg_sound_selected)
            holder.binding.ivSelectedDot.visibility = View.VISIBLE
        } else {
            holder.binding.clMain.setBackgroundResource(R.drawable.bg_sound_unselected)
            holder.binding.ivSelectedDot.visibility = View.GONE
        }
    }

    private fun loadImage(item: SoundsDataClass, imageView: android.widget.ImageView) {
        try {
            Glide.with(context)
                .asBitmap()
                .load(item.img)
                .into(imageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = soundsList.size

    fun updateSelection(newPosition: Int) {
        if (newPosition == selectorValue) return

        val oldPosition = selectorValue
        selectorValue = newPosition

        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition)
        }
        if (selectorValue >= 0) {
            notifyItemChanged(selectorValue)
        }
    }
}
