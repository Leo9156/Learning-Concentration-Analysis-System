package com.example.learningassistance.historypage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learningassistance.R
import com.example.learningassistance.database.Task
import com.google.android.material.card.MaterialCardView

class HistoryTaskItemAdapter(
    val rootOnClickListener: (task: Task) -> Unit
) : RecyclerView.Adapter<HistoryTaskItemAdapter.HistoryTaskItemViewHolder>() {
    var data = listOf<Task>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryTaskItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_task_item, parent, false)
        return HistoryTaskItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: HistoryTaskItemViewHolder, position: Int) {
        val item = data[position]

        holder.tvTitle.text = item.taskName
        holder.root.setOnClickListener {
            rootOnClickListener(item)
        }
    }

    class HistoryTaskItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: MaterialCardView
        val tvTitle: TextView

        init {
            root = view.findViewById(R.id.history_task_card)
            tvTitle = view.findViewById(R.id.hist_task_title)
        }
    }
}