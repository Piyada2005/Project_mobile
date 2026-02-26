package com.example.project

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class TaskAdapter(
    private val taskList: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val btnStar: ImageView = itemView.findViewById(R.id.btnStar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        // ถ้า title เป็นค่าว่างหรือ null ให้แสดงข้อความว่า "ไม่มีชื่อ"
        holder.txtTitle.text = task.title?.takeIf { it.isNotBlank() } ?: "ไม่มีชื่องาน"
        holder.txtDate.text = task.dueDate ?: "ไม่ระบุวัน"

        holder.btnStar.setImageResource(
            if (task.isStarred) R.drawable.ic_star_filled
            else R.drawable.ic_star
        )

        holder.btnStar.setOnClickListener {

            task.isStarred = !task.isStarred
            notifyItemChanged(position)

            db.collection("tasks")
                .document(task.id)
                .set(
                    mapOf("isStarred" to task.isStarred),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        }



    }

    override fun getItemCount(): Int = taskList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Task>) {
        taskList.clear()
        taskList.addAll(newList)
        notifyDataSetChanged()
    }
}