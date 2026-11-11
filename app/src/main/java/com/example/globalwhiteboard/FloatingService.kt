package com.example.globalwhiteboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.ViewGroup


class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var toolbarView: View
    private lateinit var whiteboardView: WhiteboardView

    private lateinit var toolbarParams: WindowManager.LayoutParams
    private lateinit var whiteboardParams: WindowManager.LayoutParams

    private var isPassthroughMode = false
    // 🌟 新增：用于控制 5 秒变淡
    private var penMenuView: View? = null
    private var penMenuParams: WindowManager.LayoutParams? = null
    private var colorPickerView: View? = null
    private var colorPickerParams: WindowManager.LayoutParams? = null
    private var settingsMenuView: View? = null
    private var settingsMenuParams: WindowManager.LayoutParams? = null
    private var isToolbarExpanded = true
    private val handler = Handler(Looper.getMainLooper())
    private val fadeOutRunnable = Runnable {
        // 5秒后，工具栏变淡到 20% 透明度
        toolbarView.animate().alpha(0.2f).setDuration(300).start()
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        initWindows()
    }

    private fun startAsForeground() {
        // ... 保持原有通知代码不变 ...
        val channelId = "whiteboard_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "白板服务", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("全局白板运行中")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun initWindows() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 1. 初始化画板（保持不变）
        whiteboardView = WhiteboardView(this)
        whiteboardParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply { alpha = 1.0f }
        windowManager.addView(whiteboardView, whiteboardParams)

        // 2. 初始化工具栏
        toolbarView = LayoutInflater.from(this).inflate(R.layout.layout_toolbar, null)

        toolbarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        setupToolbarEvents()
        windowManager.addView(toolbarView, toolbarParams)

        resetFadeTimer()
    }

    private fun setupToolbarEvents() {
        val rootLayout = toolbarView as ToolbarLayout
        var initialX = 0
        var initialY = 0

        // ================= 1. 触摸与拖拽逻辑 =================
        rootLayout.onTouchDown = {
            handler.removeCallbacks(fadeOutRunnable)
            toolbarView.animate().alpha(1.0f).setDuration(150).start()
            initialX = toolbarParams.x
            initialY = toolbarParams.y
        }

        rootLayout.onDrag = { dx, dy ->
            toolbarParams.x = initialX + dx
            toolbarParams.y = initialY + dy
            windowManager.updateViewLayout(toolbarView, toolbarParams)
        }

        rootLayout.onTouchUp = {
            resetFadeTimer()
        }

        // ================= 2. 统一获取所有按钮 =================
        val btnMode = toolbarView.findViewById<ImageView>(R.id.btnMode)
        val btnClear = toolbarView.findViewById<ImageView>(R.id.btnClear)
        val btnUndo = toolbarView.findViewById<ImageView>(R.id.btnUndo)
        val btnEye = toolbarView.findViewById<ImageView>(R.id.btnEye)
        val btnColor = toolbarView.findViewById<ImageView>(R.id.btnColor)
        val btnPen = toolbarView.findViewById<ImageView>(R.id.btnPen)
        val btnSettings = toolbarView.findViewById<ImageView>(R.id.btnSettings)
        val btnRedo = toolbarView.findViewById<ImageView>(R.id.btnRedo) // 👈 统一在这里声明一次

        // ================= 3. 统一绑定点击事件 =================
        btnMode.setOnClickListener { toggleMode(btnMode) }

        btnClear.setOnClickListener {
            whiteboardView.clear()
            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
        }

        btnUndo.setOnClickListener { whiteboardView.undo() }
        btnRedo.setOnClickListener { whiteboardView.redo() }

        btnColor.setOnClickListener { toggleColorPicker() }
        btnPen.setOnClickListener { togglePenMenu() }
        btnSettings.setOnClickListener { toggleSettingsMenu() }

        // ================= 4. 核心：眼睛图标的收纳/展开动画 =================
        btnEye.setOnClickListener {
            val transition = AutoTransition()
            transition.duration = 250
            TransitionManager.beginDelayedTransition(rootLayout, transition)

            isToolbarExpanded = !isToolbarExpanded

            if (isToolbarExpanded) {
                // 展开：显示所有按钮
                btnUndo.visibility = View.VISIBLE
                btnRedo.visibility = View.VISIBLE
                btnClear.visibility = View.VISIBLE
                btnColor.visibility = View.VISIBLE
                btnPen.visibility = View.VISIBLE
                btnMode.visibility = View.VISIBLE
                btnSettings.visibility = View.VISIBLE

                btnEye.setImageResource(R.drawable.ic_minus_circle)
            } else {
                // 收纳：隐藏除了眼睛以外的所有按钮
                btnUndo.visibility = View.GONE
                btnRedo.visibility = View.GONE
                btnClear.visibility = View.GONE
                btnColor.visibility = View.GONE
                btnPen.visibility = View.GONE
                btnMode.visibility = View.GONE
                btnSettings.visibility = View.GONE

                btnEye.setImageResource(R.drawable.ic_add_circle)
            }

            resetFadeTimer()
        }
    }
    private fun resetFadeTimer() {
        handler.removeCallbacks(fadeOutRunnable)
        handler.postDelayed(fadeOutRunnable, 5000) // 5000 毫秒 = 5 秒
    }

    private fun togglePenMenu() {
        // 如果已经显示了，就关闭它
        if (penMenuView != null) {
            windowManager.removeView(penMenuView)
            penMenuView = null
            return
        }

        // 1. 加载布局
        penMenuView = LayoutInflater.from(this).inflate(R.layout.layout_pen_menu, null)

        // 2. 设置窗口参数（位置在工具栏下方）
        val widthInPx = (220 * resources.displayMetrics.density).toInt()

        penMenuParams = WindowManager.LayoutParams(
            widthInPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = toolbarParams.x
            y = toolbarParams.y + 160
        }

        // 3. 处理粗细拖动 (SeekBar) 和 数值显示
        val seekBar = penMenuView?.findViewById<android.widget.SeekBar>(R.id.seekBarWidth)
        val tvPen = penMenuView?.findViewById<android.widget.TextView>(R.id.tvPen)
        val tvEraser = penMenuView?.findViewById<android.widget.TextView>(R.id.tvEraser)
        val tvWidthValue = penMenuView?.findViewById<android.widget.TextView>(R.id.tvWidthValue)

        // 初始化状态
        val currentWidth = whiteboardView.getCurrentWidth().toInt()
        seekBar?.progress = currentWidth
        tvWidthValue?.text = "${currentWidth}px" // 初始化数值文本

        if (whiteboardView.isEraserMode()) {
            tvEraser?.setBackgroundColor(android.graphics.Color.parseColor("#D0D0D0"))
            tvPen?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            tvPen?.setBackgroundColor(android.graphics.Color.parseColor("#D0D0D0"))
            tvEraser?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        seekBar?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                whiteboardView.setPaintWidth(progress.toFloat())
                // 拖动时实时更新数值显示
                tvWidthValue?.text = "${progress}px"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })


        // 4. 处理画笔/橡皮切换
        tvPen?.setOnClickListener {
            whiteboardView.setEraser(false)
            tvPen?.setBackgroundColor(android.graphics.Color.parseColor("#D0D0D0"))
            tvEraser?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            // 切换时，滑块跳到画笔粗细，并且更新数值文本
            val width = whiteboardView.getCurrentWidth().toInt()
            seekBar?.progress = width
            tvWidthValue?.text = "${width}px"
        }

        tvEraser?.setOnClickListener {
            whiteboardView.setEraser(true)
            tvEraser?.setBackgroundColor(android.graphics.Color.parseColor("#D0D0D0"))
            tvPen?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            // 切换时，滑块跳到橡皮粗细，并且更新数值文本
            val width = whiteboardView.getCurrentWidth().toInt()
            seekBar?.progress = width
            tvWidthValue?.text = "${width}px"
        }


        // 5. 添加到屏幕
        windowManager.addView(penMenuView, penMenuParams)
    }
    private fun toggleSettingsMenu() {
        if (settingsMenuView != null) {
            windowManager.removeView(settingsMenuView)
            settingsMenuView = null
            return
        }

        settingsMenuView = LayoutInflater.from(this).inflate(R.layout.layout_settings_menu, null)

        val widthInPx = (140 * resources.displayMetrics.density).toInt()
        settingsMenuParams = WindowManager.LayoutParams(
            widthInPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = toolbarParams.x - 80 // 稍微调整一下 x 的位置，让它居中一点
            y = toolbarParams.y + 160
        }


        val tvOrientation = settingsMenuView?.findViewById<android.widget.TextView>(R.id.tvOrientation)
        val tvExit = settingsMenuView?.findViewById<android.widget.TextView>(R.id.tvExit)
        val rootLayout = toolbarView as ToolbarLayout

        tvOrientation?.setOnClickListener {
            toggleSettingsMenu()

            val transition = AutoTransition()
            transition.duration = 250
            TransitionManager.beginDelayedTransition(rootLayout, transition)

            // 计算一下 dp 对应的像素
            val dp8 = (8 * resources.displayMetrics.density).toInt()
            val dp12 = (12 * resources.displayMetrics.density).toInt()
            val dp20 = (20 * resources.displayMetrics.density).toInt()

            if (rootLayout.orientation == android.widget.LinearLayout.HORIZONTAL) {
                // 切换为纵向
                rootLayout.orientation = android.widget.LinearLayout.VERTICAL

                // 🌟 在这里调纵向的长宽：
                // 左右加 12dp（变宽），上下加 20dp（变长）
                rootLayout.setPadding(dp12, dp20, dp12, dp20)

            } else {
                // 切换为横向
                rootLayout.orientation = android.widget.LinearLayout.HORIZONTAL

                // 恢复横向原本的 Padding（左右 20dp，上下 8dp）
                rootLayout.setPadding(dp20, dp8, dp20, dp8)
            }
        }

        tvExit?.setOnClickListener {
            toggleSettingsMenu()
            stopSelf()
        }

        windowManager.addView(settingsMenuView, settingsMenuParams)
    }
    private fun toggleColorPicker() {
        if (colorPickerView != null) {
            // 如果已经显示了，就关闭它
            windowManager.removeView(colorPickerView)
            colorPickerView = null
            return
        }

        // 1. 加载布局 (确保你有 layout_color_picker.xml)
        colorPickerView = LayoutInflater.from(this).inflate(R.layout.layout_color_picker, null)

        // 2. 设置窗口参数
        colorPickerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = toolbarParams.x
            y = toolbarParams.y + 160 // 偏移量，让弹窗显示在工具栏下方
        }

        // 3. 给颜色方块绑定点击事件
        val colors = arrayOf(
            "cRed", "cOrange", "cYellow", "cGreen",
            "cCyan", "cBlue", "cPurple", "cPink",
            "cBlack", "cGray", "cBrown", "cNavy",
            "cWhite", "cLightGreen", "cLightBlue", "cRose"
        )

        for (colorId in colors) {
            val id = resources.getIdentifier(colorId, "id", packageName)
            colorPickerView?.findViewById<View>(id)?.setOnClickListener { view ->
                val colorStr = view.tag.toString()
                val colorInt = android.graphics.Color.parseColor(colorStr)
                whiteboardView.setPaintColor(colorInt)
                toggleColorPicker() // 选完自动关闭
            }
        }

        // 4. 添加到屏幕上
        windowManager.addView(colorPickerView, colorPickerParams)
    }
    private fun toggleMode(btnMode: ImageView) {
        try {
            isPassthroughMode = !isPassthroughMode

            var newFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            if (isPassthroughMode) {
                newFlags = newFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                // 换一个图表示穿透
               btnMode.setImageResource(R.drawable.ic_pen)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val im = getSystemService(android.hardware.input.InputManager::class.java)
                    whiteboardParams.alpha = im?.maximumObscuringOpacityForTouch ?: 0.8f
                } else {
                    whiteboardParams.alpha = 0.8f
                }
            } else {
                btnMode.setImageResource(R.drawable.ic_mode)
                whiteboardParams.alpha = 1.0f
            }

            whiteboardParams.flags = newFlags
            windowManager.updateViewLayout(whiteboardView, whiteboardParams)

            // 暴力置顶
            windowManager.removeView(toolbarView)
            windowManager.addView(toolbarView, toolbarParams)

        } catch (e: Exception) {
            Log.e("Whiteboard", "切换模式失败", e)
        }
    }

    override fun onDestroy() {
        penMenuView?.let { windowManager.removeView(it) }
        settingsMenuView?.let { windowManager.removeView(it) }
        super.onDestroy()
        handler.removeCallbacks(fadeOutRunnable) // 销毁时清空定时器，防内存泄漏
        try {
            if (::whiteboardView.isInitialized) windowManager.removeView(whiteboardView)
            if (::toolbarView.isInitialized) windowManager.removeView(toolbarView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}