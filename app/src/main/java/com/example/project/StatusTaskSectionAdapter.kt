package com.example.project

import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusTaskSectionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val rows = mutableListOf<Row>()
    private val dueDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    sealed class Row {
        data class Header(val dateTitle: String) : Row()
        data class TaskItem(val task: Task, val strikeThrough: Boolean) : Row()
    }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtDateTitle: TextView = itemView.findViewById(R.id.txtDateTitle)
    }

    private class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val btnStar: ImageButton = itemView.findViewById(R.id.btnStar)
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> VIEW_TYPE_HEADER
        is Row.TaskItem -> VIEW_TYPE_TASK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_task_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_status_timeline_task, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderViewHolder).txtDateTitle.text = row.dateTitle
            is Row.TaskItem -> bindTask(holder as TaskViewHolder, row.task, row.strikeThrough)
        }
    }

    private fun bindTask(holder: TaskViewHolder, task: Task, strikeThrough: Boolean) {
        holder.txtTitle.text = task.title?.takeIf { it.isNotBlank() } ?: "ไม่มีชื่องาน"
        holder.txtDate.visibility = View.GONE
        holder.txtTitle.paintFlags = if (strikeThrough) {
            holder.txtTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.txtTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        holder.txtTitle.paint.strokeWidth = if (strikeThrough) 2f else 0f
        holder.txtTitle.paint.style = Paint.Style.FILL

        holder.btnStar.setImageResource(if (task.isStarred) R.drawable.ic_star_filled else R.drawable.ic_star)
        holder.btnStar.setOnClickListener {
            task.isStarred = !task.isStarred
            notifyItemChanged(holder.bindingAdapterPosition)
            db.collection("tasks")
                .document(task.id)
                .set(mapOf("isStarred" to task.isStarred), SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("StatusTaskSectionAdapter", "อัปเดตสถานะดาวไม่สำเร็จ", e)
                }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, TaskDetailActivity::class.java)
            intent.putExtra("taskId", task.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateTasks(tasks: List<Task>, strikeThrough: Boolean) {
        rows.clear()

        val grouped = tasks.groupBy { task ->
            task.dueDate?.takeIf { it.isNotBlank() } ?: "ไม่ระบุวัน"
        }

        val orderedHeaders = grouped.keys.sortedWith(compareBy<String> { key ->
            parseDateOrNull(key) ?: Date(Long.MAX_VALUE)
        }.thenBy { it })

        for (header in orderedHeaders) {
            rows.add(Row.Header(header))
            grouped[header].orEmpty().forEach { task ->
                rows.add(Row.TaskItem(task, strikeThrough))
            }
        }

        notifyDataSetChanged()
    }

    private fun parseDateOrNull(value: String): Date? {
        return try {
            dueDateFormatter.isLenient = false
            dueDateFormatter.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }
}
