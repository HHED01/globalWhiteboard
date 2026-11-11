package com.example.globalwhiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class ToolbarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onTouchDown: (() -> Unit)? = null
    var onTouchUp: (() -> Unit)? = null
    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchDown?.invoke() // 手指按下，通知外部恢复亮度
                initialTouchX = ev.rawX
                initialTouchY = ev.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - initialTouchX
                val dy = ev.rawY - initialTouchY

                // 如果移动超过 10 像素，判定为拖拽
                if (!isDragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                    isDragging = true
                }
                if (isDragging) {
                    onDrag?.invoke(dx.toInt(), dy.toInt())
                    return true // 拦截事件，不再传给里面的图标，防止拖拽时误触点击
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTouchUp?.invoke() // 手指抬起，通知外部重新开始倒计时
                if (isDragging) {
                    isDragging = false
                    return true // 拖拽结束，拦截 UP 事件，防止触发图标的点击
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}