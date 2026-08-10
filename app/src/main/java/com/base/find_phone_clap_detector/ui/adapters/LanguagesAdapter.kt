package com.base.find_phone_clap_detector.ui.adapters

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.LayoutLanguagesBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.dataClasses.ModelLanguage

@Keep
class LanguagesAdapter(
    private val context: Context?,
    private val mList: List<ModelLanguage>,
    private val onLanguageSelected: (ModelLanguage) -> Unit
) : RecyclerView.Adapter<LanguagesAdapter.ViewHolder>() {

    private val initialData: List<ModelLanguage> = mList.toList()

    private var selectedPosition: Int = -1
    var selectedLanguage: ModelLanguage? = null
        private set

    init {
        val savedLanguage = MyApplication.mInstance.preferenceManager
            .getString(PreferenceManager.Key.APP_LANGUAGE, "English") ?: "English"

        selectedPosition = initialData.indexOfFirst { it.language == savedLanguage }

        if (selectedPosition != -1) {
            selectedLanguage = initialData[selectedPosition]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutLanguagesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(initialData[position], position)
    }

    override fun getItemCount(): Int = initialData.size

    inner class ViewHolder(private val binding: LayoutLanguagesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(modelLanguage: ModelLanguage, position: Int) {
            binding.languageName.text = modelLanguage.language
            binding.flagImg.setImageResource(modelLanguage.image)

            binding.checkBtn.setOnCheckedChangeListener(null)
            binding.checkBtn.isChecked = (position == selectedPosition)

            val activeColor = ContextCompat.getColor(itemView.context, R.color.appClr)
            val inactiveColor = ContextCompat.getColor(itemView.context, R.color.cardClr)
            val strokeWidthPx = itemView.context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
            val cardBgColor = ContextCompat.getColor(itemView.context, R.color.cardClr)

            // Simulate MaterialCardView's strokeColor by mutating the drawable
            val bgDrawable = binding.cardInner.background.mutate() as GradientDrawable
            bgDrawable.setStroke(
                strokeWidthPx,
                if (position == selectedPosition) activeColor else inactiveColor
            )
            bgDrawable.setColor(cardBgColor)

            binding.checkBtn.setOnClickListener {
                updateSelection(position, modelLanguage)
            }

            binding.cardLanguage.setOnClickListener {
                binding.checkBtn.performClick()
            }
        }
    }

    private fun updateSelection(position: Int, modelLanguage: ModelLanguage) {
        if (selectedPosition != position) {
            val previous = selectedPosition
            selectedPosition = position
            selectedLanguage = modelLanguage

            if (previous != -1) notifyItemChanged(previous)
            notifyItemChanged(position)

            onLanguageSelected(modelLanguage)
        }
    }
}