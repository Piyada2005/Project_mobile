package com.example.project

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubtaskAdapter(private val list: MutableList<String>) :
    RecyclerView.Adapter<SubtaskAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) :
        RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tv = TextView(parent.context)
        tv.setPadding(16,16,16,16)
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = "• " + list[position]
    }

    override fun getItemCount() = list.size
}