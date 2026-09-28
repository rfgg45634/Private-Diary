package com.example.mydiary.ui.knowledge

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mydiary.data.NoteEntity
import com.example.mydiary.databinding.ItemKnowledgeCardBinding
import com.example.mydiary.util.DateUtils

class KnowledgeAdapter(
    private val onItemClick: (NoteEntity) -> Unit,
    private val onDragFinished: (List<NoteEntity>) -> Unit
) : RecyclerView.Adapter<KnowledgeAdapter.ViewHolder>() {

    var itemTouchHelper: ItemTouchHelper? = null
    private val items = mutableListOf<NoteEntity>()

    /**
     * 用 DiffUtil 只刷新真正变化的条目，而不是每次都 notifyDataSetChanged()。
     * 每次自动保存触发 Room 的 Flow 重新发射，都会导致整个列表重新绑定——
     * 条目一多、或者正在编辑时滑动就会感觉卡顿，这里是真正的病灶。
     */
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKnowledgeCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val holder = ViewHolder(binding)
        binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onItemClick(items[pos])
        }
        binding.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = items[position]
        holder.binding.titleText.text = DateUtils.displayTitle(note.title, note.createdTime)
        holder.binding.dateText.text = DateUtils.formatMonthDay(note.createdTime)
        // 正文预览：去掉居中标记、换行，只留一行；正文为空时隐藏预览行
        val preview = note.content.replace("[c]", "").replace("\n", " ").trim()
        if (preview.isBlank()) {
            holder.binding.previewText.visibility = View.GONE
        } else {
            holder.binding.previewText.visibility = View.VISIBLE
            holder.binding.previewText.text = preview
        }
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

    /** 拖拽过程中实时调整内存中的顺序（尚未落库） */
    fun moveItem(from: Int, to: Int) {
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }

    /** 拖拽手指松开后调用，落库保存新顺序 */
    fun onDragEnd() {
        onDragFinished(items.toList())
    }

    class ViewHolder(val binding: ItemKnowledgeCardBinding) : RecyclerView.ViewHolder(binding.root)
}
