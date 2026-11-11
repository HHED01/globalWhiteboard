🎨 GlobalWhiteboard (全局透明批注白板)
一个基于 Android 原生 API 开发的极简全屏透明批注悬浮窗，灵感来源于华为平板的全局书写功能。

无论你是在看视频、上网课还是浏览网页，它都能随时化身为你的草稿纸，不打断当前应用的播放，随写随记。

✨ 核心特性
🖼️ 全屏透明无痕：完美的 TRANSLUCENT 渲染，悬浮于所有应用之上，不影响底层内容播放。

✍️ 流畅的书写体验：基于 Path 重构的底层绘制逻辑，支持画笔与橡皮擦独立记忆粗细。

🎨 16色极简调色盘：精心挑选的马卡龙/深色系配色，配合圆形 UI 设计。

🔄 画笔 / 穿透模式：一键切换。穿透模式下，手指直接穿透屏幕，完全不耽误底层应用操作。

↩️ 完整的撤销 / 重做：基于状态栈的笔划记录，再也不怕画错。

🫧 智能收纳与隐藏：点击“减号”一键缩成小圆球；5秒无操作自动变淡，不遮挡视线。

📐 横竖屏无缝切换：跟随设置菜单，工具栏可平滑切换为横向或纵向排列。

🔒 （预留）手写笔防误触：精准识别 STYLUS 输入，屏蔽手指误触。

📸 界面预览
(在这里插入一张你平板上的截图，可以直接用你之前发我的那张带有粉色动漫壁纸的截图)

🛠️ 技术栈
语言：Kotlin

核心控件：WindowManager + TYPE_APPLICATION_OVERLAY

渲染引擎：View 自定义 
onDraw
 + Canvas

动画效果：TransitionManager (AutoTransition) + ViewPropertyAnimator

🚀 如何编译
Clone 本项目到本地：

bash
git clone https://github.com/你的用户名/你的仓库名.git
使用 Android Studio 打开项目。

授予悬浮窗权限（SYSTEM_ALERT_WINDOW）。

点击 Run 即可安装到你的 Android 设备（建议 Android 12 及以上）。

🥚 开发者彩蛋
细心的朋友可以在“应用设置 -> 版本号”里发现一点小惊喜，（悄悄告诉你，是我最喜欢的动物）。

Designed and developed with ❤️ by 灰黄耳朵
