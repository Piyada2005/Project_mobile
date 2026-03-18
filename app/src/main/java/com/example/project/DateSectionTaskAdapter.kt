package com.example.project

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class DateSectionTaskAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val rows = mutableListOf<Row>()

    sealed class Row {
        data class Header(val title: String) : Row()
        data class TaskItem(val task: Task) : Row()
    }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSectionTitle: TextView = itemView.findViewById(R.id.txtSectionTitle)
    }

    private class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val btnStar: ImageButton = itemView.findViewById(R.id.btnStar)
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is Row.Header -> VIEW_TYPE_HEADER
            is Row.TaskItem -> VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_task_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_task, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderViewHolder).txtSectionTitle.text = row.title
            is Row.TaskItem -> bindTaskRow(holder as TaskViewHolder, row.task)
        }
    }

    private fun bindTaskRow(holder: TaskViewHolder, task: Task) {
        holder.txtTitle.text = task.title?.takeIf { it.isNotBlank() } ?: "ไม่มีชื่องาน"
        holder.txtDate.text = task.dueDate ?: "ไม่ระบุวัน"

        holder.btnStar.setImageResource(
            if (task.isStarred) R.drawable.ic_star_filled else R.drawable.ic_star
        )

        holder.btnStar.setOnClickListener {
            task.isStarred = !task.isStarred
            notifyItemChanged(holder.bindingAdapterPosition)

            db.collection("tasks")
                .document(task.id)
                .set(mapOf("isStarred" to task.isStarred), SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("DateSectionTaskAdapter", "อัปเดตสถานะดาวไม่สำเร็จ", e)
                }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, TaskDetailActivity::class.java)
            intent.putExtra("taskId", task.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateSections(
        overdueTasks: List<Task>,
        todayTasks: List<Task>,
        upcomingTasks: List<Task>,
        futureTasks: List<Task>
    ) {
        rows.clear()
        appendSection("ก่อนหน้า", overdueTasks)
        appendSection("วันนี้", todayTasks)
        appendSection("กำลังจะถึง", upcomingTasks)
        appendSection("ในอนาคต", futureTasks)

        notifyDataSetChanged()
    }

    private fun appendSection(title: String, tasks: List<Task>) {
        if (tasks.isEmpty()) return
        rows.add(Row.Header(title))
        rows.addAll(tasks.map { Row.TaskItem(it) })
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }
}
