package com.example.mydiary.ui.diary

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mydiary.R

/** 日记模块不需要拖拽排序，只需要支持左滑移入回收站 */
class DiarySwipeCallback(
    private val onSwipeDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwipeDelete(viewHolder.bindingAdapterPosition)
    }

    // 左滑过程中，在卡片底下露出一块渐变的红色背景 + 垃圾桶图标，提示"松手就会删除"。
    // 用从透明到实色的渐变代替一整块死板的纯色，看起来是"柔和地露出来"而不是硬生生一块色块。
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
            val itemView = viewHolder.itemView
            val context = itemView.context
            val background = RectF(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            c.drawColor(Color.TRANSPARENT)

            val dangerColor = ContextCompat.getColor(context, R.color.danger)
            val fadedColor = ColorUtils.setAlphaComponent(dangerColor, 40)
            val paint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    background.left, 0f, background.right, 0f,
                    fadedColor, dangerColor,
                    Shader.TileMode.CLAMP
                )
            }
            val cornerRadiusPx = 20f * context.resources.displayMetrics.density
            c.drawRoundRect(background, cornerRadiusPx, cornerRadiusPx, paint)

            val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
            icon?.let {
                val iconSize = 22 * context.resources.displayMetrics.density
                val iconTop = itemView.top + (itemView.height - iconSize) / 2
                val iconRight = itemView.right - 22 * context.resources.displayMetrics.density
                val iconLeft = iconRight - iconSize
                val progress = (-dX / (itemView.width * 0.3f)).coerceIn(0f, 1f)
                it.alpha = (progress * 255).toInt()
                it.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), (iconTop + iconSize).toInt())
                it.draw(c)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
