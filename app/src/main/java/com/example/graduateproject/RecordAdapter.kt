package com.example.graduateproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

data class Record(val date: String, val distance: String, val time: String)

class RecordAdapter(
    private val context: Context,
    private val recordList: List<Record>
) : BaseAdapter() {

    override fun getCount(): Int = recordList.size

    override fun getItem(position: Int): Any = recordList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_exercise_record, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val record = getItem(position) as Record
        holder.txv_date.text = "日期：${record.date}"
        holder.txv_distance.text = "運動距離：${record.distance}"
        holder.txv_time.text = "運動時長：${record.time}"

        return view
    }

    private class ViewHolder(view: View) {
        val txv_date: TextView = view.findViewById(R.id.txv_date)
        val txv_distance: TextView = view.findViewById(R.id.txv_distance)
        val txv_time: TextView = view.findViewById(R.id.txv_time)
    }
}