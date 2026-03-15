package com.example.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttachmentAdapter(
    private val list: MutableList<Attachment>,
    private val onDelete: (Int) -> Unit,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<AttachmentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text = view.findViewById<TextView>(R.id.txtAttachment)
        val image = view.findViewById<ImageView>(R.id.imgAttachment)
        val delete = view.findViewById<ImageView>(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attachment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        if (item.type == "image") {

            holder.image.visibility = View.VISIBLE
            holder.text.visibility = View.GONE

            try {

                val uri = android.net.Uri.parse(item.value)

                holder.image.setImageURI(null)
                holder.image.setImageURI(uri)

                holder.image.setOnClickListener {
                    onImageClick(position)
                }

            } catch (e: Exception) {

                e.printStackTrace()

                holder.image.visibility = View.GONE
                holder.text.visibility = View.VISIBLE
                holder.text.text = "ไม่สามารถโหลดรูปได้"

            }

        } else {

                holder.image.visibility = View.GONE
                holder.text.visibility = View.VISIBLE
                holder.text.text = item.value

            holder.text.setOnClickListener {

                val context = holder.itemView.context

                android.app.AlertDialog.Builder(context)
                    .setTitle("เปิดลิงก์")
                    .setMessage("ต้องการเปิดลิงก์นี้หรือไม่?")
                    .setPositiveButton("เปิดลิงก์") { _, _ ->

                        try {

                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(item.value)
                            )

                            context.startActivity(intent)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast
                                .makeText(context, "ไม่สามารถเปิดลิงก์ได้", android.widget.Toast.LENGTH_SHORT)
                                .show()
                        }

                    }
                    .setNegativeButton("ยกเลิก", null)
                    .show()
            }
        }

        holder.delete.setOnClickListener {
            onDelete(position)
        }
    }
}