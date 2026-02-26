package com.example.project

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // เปลี่ยนจาก ImageView เป็น ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class TaskAdapter(
    private val taskList: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ID ตรงกับใน CardView (item_task.xml)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val btnStar: ImageButton = itemView.findViewById(R.id.btnStar) // ปรับเป็น ImageButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        // 1. จัดการข้อความ Title และ Date
        holder.txtTitle.text = task.title?.takeIf { it.isNotBlank() } ?: "ไม่มีชื่องาน"
        holder.txtDate.text = task.dueDate ?: "ไม่ระบุวัน"

        // 2. จัดการรูปภาพปุ่มดาว
        holder.btnStar.setImageResource(
            if (task.isStarred) R.drawable.ic_star_filled // ดาวทึบ (เมื่อกด Favorite)
            else R.drawable.ic_star // ดาวโปร่ง (ค่าเริ่มต้น)
        )

        // 3. จัดการเหตุการณ์เมื่อกดปุ่มดาว
        holder.btnStar.setOnClickListener {
            // สลับสถานะ (True เป็น False / False เป็น True)
            task.isStarred = !task.isStarred

            // อัปเดต UI ทันทีไม่ต้องรอ Firebase
            notifyItemChanged(position)

            // อัปเดตข้อมูลขึ้น Firebase Firestore
            db.collection("tasks")
                .document(task.id)
                .set(
                    mapOf("isStarred" to task.isStarred),
                    SetOptions.merge()
                )
                .addOnFailureListener { e ->
                    Log.e("TaskAdapter", "อัปเดตสถานะดาวไม่สำเร็จ", e)
                    // ถ้าอัปเดตไม่สำเร็จ อาจจะเขียนโค้ดสลับสถานะกลับ หรือแจ้งเตือนผู้ใช้ตรงนี้ได้ครับ
                }
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