package com.example.globalwhiteboard

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class WhiteboardView(context: Context) : View(context) {

    // 1. 定义“一笔”的数据结构
    data class Stroke(val path: Path, val paint: Paint)

    // 历史记录栈
    private val undoStack = mutableListOf<Stroke>()
    private val redoStack = mutableListOf<Stroke>()

    // 当前正在画的路径
    private var currentPath = Path()
    private var currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // 画笔和橡皮状态记忆
    private var isEraserMode = false
    private var penWidth = 8f
    private var eraserWidth = 30f

    // 记录画笔颜色
    private var currentColor = Color.RED

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 2. 遍历并重绘所有历史笔迹
        for (stroke in undoStack) {
            canvas.drawPath(stroke.path, stroke.paint)
        }
        // 3. 绘制当前正在画的笔迹
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 4. 手指抬起，将当前路径存入历史记录
                // 注意：这里必须创建一个新的 Path 和 Paint，否则会被下一次绘制修改
                val newPath = Path(currentPath)
                val newPaint = Paint(currentPaint)
                undoStack.add(Stroke(newPath, newPaint))

                // 既然画了新的一笔，之前撤销的重做记录就作废了
                redoStack.clear()

                currentPath.reset()
                invalidate()
            }
        }
        return true
    }

    // =============== 撤销与重做 API ===============
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val lastStroke = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(lastStroke)
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val lastStroke = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(lastStroke)
            invalidate()
        }
    }

    // =============== 画笔设置 API ===============
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        currentPath.reset()
        invalidate()
    }

    fun setPaintColor(color: Int) {
        currentColor = color
        // 如果当前在橡皮模式，选颜色应该自动切回画笔
        if (isEraserMode) setEraser(false)
        currentPaint.color = color
    }

    fun setPaintWidth(width: Float) {
        if (isEraserMode) {
            eraserWidth = width
        } else {
            penWidth = width
        }
        currentPaint.strokeWidth = width
    }

    fun setEraser(isEraser: Boolean) {
        isEraserMode = isEraser
        if (isEraserMode) {
            currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            currentPaint.strokeWidth = eraserWidth
        } else {
            currentPaint.xfermode = null
            currentPaint.strokeWidth = penWidth
            currentPaint.color = currentColor // 恢复之前的颜色
        }
    }

    fun getCurrentWidth() = currentPaint.strokeWidth
    fun isEraserMode() = isEraserMode
}