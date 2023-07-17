package com.example.learningassistance.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learningassistance.R
import com.example.learningassistance.database.Task
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator

class TaskItemAdapter(
    private val taskViewModel: TaskViewModel,
    val editContentClickListener: (task: Task) -> Unit,
    val startDetectionClickListener: (task: Task) -> Unit
) : RecyclerView.Adapter<TaskItemAdapter.TaskItemViewHolder>() {
    var data = listOf<Task>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskItemViewHolder, position: Int) {
        val item = data[position]

        holder.textViewTitle.text = item.taskName
        holder.textViewDescription.text = item.taskDescription
        holder.timeChip.text = String.format(
            holder.itemView.context.getString(R.string.task_duration),
            item.taskDurationMin)
        holder.dateChip.text = item.taskDate
        holder.completenessIndicator.progress = item.taskCompletePercentage
        holder.textViewCompleteness.text = String.format(
            holder.itemView.context.getString(R.string.percent),
            item.taskCompletePercentage)
        holder.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(holder.itemView.context)
                .setTitle(holder.itemView.context.getString(R.string.delete_task))
                .setMessage(holder.itemView.context.getString(R.string.delete_task_detail))
                .setPositiveButton(holder.itemView.context.getString(R.string.delete)) { dialog, _ ->
                    taskViewModel.deleteTask(item)
                    dialog.dismiss()
                }
                .setNegativeButton(holder.itemView.context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        holder.root.setOnClickListener {
            editContentClickListener(item)
        }
        holder.startButton.setOnClickListener {
            startDetectionClickListener(item)
        }
    }

    class TaskItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: MaterialCardView
        val textViewTitle: TextView
        val textViewDescription: TextView
        val timeChip: TextView
        val dateChip: TextView
        val completenessIndicator: CircularProgressIndicator
        val textViewCompleteness: TextView
        val startButton: Button
        val deleteButton: Button

        init {
            root = view.findViewById(R.id.root)
            textViewTitle = view.findViewById(R.id.task_title)
            textViewDescription = view.findViewById(R.id.task_description)
            timeChip = view.findViewById(R.id.task_duration)
            dateChip = view.findViewById(R.id.task_date)
            completenessIndicator = view.findViewById(R.id.task_progress)
            textViewCompleteness = view.findViewById(R.id.text_progress)
            startButton = view.findViewById(R.id.task_start_button)
            deleteButton = view.findViewById(R.id.taskDeleteButton)
        }
    }
}