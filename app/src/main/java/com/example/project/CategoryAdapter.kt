package com.example.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categoryList: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtCategory: TextView = itemView.findViewById(R.id.txtCategory)

        init {
            itemView.setOnClickListener {

                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {

                    selectedPosition = position
                    notifyDataSetChanged()

                    onCategoryClick(categoryList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.txtCategory.text = categoryList[position]

        if (position == selectedPosition) {
            holder.txtCategory.setBackgroundResource(R.drawable.bg_category_selected)
            holder.txtCategory.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
        } else {
            holder.txtCategory.setBackgroundResource(R.drawable.bg_category)
            holder.txtCategory.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            )
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }
}