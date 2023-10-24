package com.example.graduateproject

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView

class ImageAdapter(private val context: Context, private val photos: List<Int>) : BaseAdapter() {
    override fun getCount(): Int {
        return photos.size
    }

    override fun getItem(position: Int): Any {
        return photos[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView
        if (convertView == null) {
            imageView = ImageView(context)
            imageView.layoutParams = ViewGroup.LayoutParams(150, 150)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.setPadding(8, 8, 8, 8)
        } else {
            imageView = convertView as ImageView
        }

        imageView.setImageResource(photos[position])
        return imageView
    }
}