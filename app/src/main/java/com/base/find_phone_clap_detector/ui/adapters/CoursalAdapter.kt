package com.base.find_phone_clap_detector.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.base.find_phone_clap_detector.databinding.ItemCoursalBinding
import com.base.find_phone_clap_detector.ui.dataClasses.CoursalItem

class CoursalAdapter(
    private val templateList: List<CoursalItem>,
) : RecyclerView.Adapter<CoursalAdapter.TemplateViewHolder>() {

    private lateinit var activity: Activity

    inner class TemplateViewHolder(private val binding: ItemCoursalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(templateItem: CoursalItem) {
            binding.image.setImageResource(templateItem.image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemCoursalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(templateList[position])
    }

    override fun getItemCount(): Int = templateList.size

    fun setActivity(mActivity: Activity) {
        activity = mActivity
    }
}