package com.example.globalwhiteboard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 不加载任何布局

        if (!Settings.canDrawOverlays(this)) {
            // 如果没有悬浮窗权限，跳转去设置授权
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            // 授权返回后会重新进入 onCreate，届时会启动 Service
        } else {
            // 已有权限，启动悬浮窗服务
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        val serviceIntent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        // 启动后立刻关闭这个透明的 Activity，实现“无主界面”效果
        finish()
    }
}