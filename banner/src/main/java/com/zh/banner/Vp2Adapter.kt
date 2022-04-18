package com.zh.banner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.zh.banner.databinding.ItemBinding

/***
 * 2022/04/15 10:03
 */
class Vp2Adapter : BannerAdapter<String, BannerVH<ItemBinding>>(DIFFERENT) {

    class Holder(itemView: View) : BannerVH<ItemBinding>(itemView) {
        override val binding = ItemBinding.bind(itemView)
    }

    object DIFFERENT : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun onBindViewHolder(holder: BannerVH<ItemBinding>, position: Int) {
        holder.binding.root.id = position
        Glide.with(holder.itemView).load(getItem(position))
            .into(holder.binding.image)
    }


}