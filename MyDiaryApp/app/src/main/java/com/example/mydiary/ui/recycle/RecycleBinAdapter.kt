package com.example.mydiary.ui.recycle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mydiary.R
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.data.NoteType
import com.example.mydiary.databinding.ItemRecycleCardBinding
import com.example.mydiary.util.DateUtils

class RecycleBinAdapter(
    private val onItemClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, RecycleBinAdapter.ViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecycleCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val holder = ViewHolder(binding)
        binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onItemClick(getItem(pos))
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = getItem(position)
        val context = holder.itemView.context
        
        holder.binding.titleText.text = DateUtils.displayTitle(note.title, note.createdTime)
        
        // 分类标签：知识 / 日记
        if (note.type == NoteType.DIARY) {
            holder.binding.typeTag.text = context.getString(R.string.note_type_diary)
        } else {
            holder.binding.typeTag.text = context.getString(R.string.note_type_knowledge)
        }
        
        // 剩余时间
        holder.binding.remainingTimeText.text = DateUtils.formatRemainingTime(note.deletedTime)
    }

    class ViewHolder(val binding: ItemRecycleCardBinding) : RecyclerView.ViewHolder(binding.root)

    class NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem == newItem
        }
    }
}
