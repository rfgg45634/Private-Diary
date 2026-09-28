package com.example.mydiary.ui.diary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.databinding.ItemDiaryCardBinding
import com.example.mydiary.util.DateUtils

class DiaryAdapter(
    private val onItemClick: (NoteEntity) -> Unit
) : RecyclerView.Adapter<DiaryAdapter.ViewHolder>() {

    private val items = mutableListOf<NoteEntity>()

    fun submitList(newItems: List<NoteEntity>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].id == newItems[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiaryCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val holder = ViewHolder(binding)
        binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onItemClick(items[pos])
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = items[position]
        holder.binding.titleText.text = DateUtils.displayTitle(note.title, note.createdTime)
        holder.binding.timeTag.text = DateUtils.formatTag(note.createdTime)
        holder.binding.dayNumber.text = DateUtils.formatDayNumber(note.createdTime)
        holder.binding.monthLabel.text = DateUtils.formatMonthWeekday(note.createdTime)
    }

    override fun getItemCount(): Int = items.size

    /** 滑动删除时用：拿到当前位置对应的条目 */
    fun getItemAt(position: Int): NoteEntity? = items.getOrNull(position)

    /** 滑动删除时用：先从本地列表里移除，让滑动动画立刻完成，不用等数据库回调 */
    fun removeAt(position: Int) {
        if (position < 0 || position >= items.size) return
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    class ViewHolder(val binding: ItemDiaryCardBinding) : RecyclerView.ViewHolder(binding.root)
}
