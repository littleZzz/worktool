package org.yameida.worktool.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import org.yameida.worktool.Constant
import org.yameida.worktool.model.WeworkMessageBean
import org.yameida.worktool.model.WeworkMessageListBean
import org.yameida.worktool.utils.AccessibilityUtil
import org.yameida.worktool.utils.Views
import java.lang.Exception

/**
 * 进入首页-消息页
 */
fun goHome() {
    goHomeTab("消息")
}

/**
 * 进入首页tab
 * 1.检查是否有底部tab
 * 2.回退到首页
 * @param title 消息/文档/通讯录/工作台/我
 * 可能因为管理员排版首页Tab而导致找不到匹配title
 */
fun goHomeTab(title: String): Boolean {
    var atHome = false
    var find = false
    while (!atHome) {
        val list = AccessibilityUtil.findAllOnceByText(getRoot(), "消息", exact = true)
        for (item in list) {
            if (item.parent.parent.parent.childCount == 5) {
                //处理侧边栏抽屉打开
                if (title == "消息") {
                    val rect = Rect()
                    item.getBoundsInScreen(rect)
                    if (rect.left > ScreenUtils.getScreenWidth() / 2) {
                        goHomeTab("工作台")
                    }
                }
                atHome = true
                val tempList = AccessibilityUtil.findAllOnceByText(getRoot(), title, exact = true)
                for (tempItem in tempList) {
                    if (tempItem.parent.parent.parent.childCount == 5) {
                        AccessibilityUtil.performClick(tempItem)
                        sleep(300)
                        find = true
                    }
                }
            }
        }
        if (!atHome) {
            backPress()
        }
    }
    LogUtils.v("进入首页-${title}页")
    return find
}

/**
 * 当前是否在首页
 */
fun isAtHome(): Boolean {
    val list = AccessibilityUtil.findAllOnceByText(getRoot(), "消息", exact = true)
    return list.count { it.parent.parent.parent.childCount == 5 } > 0
}

/**
 * 获取企业微信窗口
 */
fun getRoot(): AccessibilityNodeInfo {
    return getRoot(false)
}

/**
 * 获取前台窗口
 * @param ignoreCheck false 必须等待前台为企业微信 true 直接返回当前前台窗口
 */
fun getRoot(ignoreCheck: Boolean): AccessibilityNodeInfo {
    while (true) {
        val tempRoot = WeworkController.weworkService.rootInActiveWindow
        val root = WeworkController.weworkService.rootInActiveWindow
        if (tempRoot != root) {
            LogUtils.e("tempRoot != root")
        } else if (root != null) {
            if (root.packageName == Constant.PACKAGE_NAMES) {
                return root
            } else {
                LogUtils.e("当前不在企业微信: ${root.packageName}")
                if (System.currentTimeMillis() % 30 == 0L) {
                    error("当前不在企业微信: ${root.packageName}")
                }
                if (ignoreCheck) {
                    return root
                }
                if(WeworkLoopImpl.awakeQiWei())sleep(3500)
            }
        }
        sleep(1000)
    }
}

/**
 * 后退
 */
fun backPress() {
    val textView = AccessibilityUtil.findOnceByClazz(getRoot(), Views.TextView)
    if (textView != null && textView.text.isNullOrBlank() && AccessibilityUtil.performClick(textView)) {
        LogUtils.v("找到回退按钮")
    } else {
        val ivButton = AccessibilityUtil.findOnceByClazz(getRoot(), Views.ImageView)
        if (ivButton != null && ivButton.isClickable && AccessibilityUtil.findFrontNode(ivButton) == null) {
            LogUtils.d("未找到回退按钮 点击第一个IV按钮")
            AccessibilityUtil.performClick(ivButton)
        } else {
            LogUtils.d("未找到回退按钮 点击第一个BT按钮")
            val button = AccessibilityUtil.findOnceByClazz(getRoot(), Views.Button)
            if (button != null && button.childCount > 0) {
                AccessibilityUtil.performClick(button.getChild(0))
            } else if (button != null) {
                AccessibilityUtil.performClick(button)
            } else {
                LogUtils.d("未找到BT按钮")
                val confirm = AccessibilityUtil.findOnceByText(getRoot(), "确定", "我知道了", "暂不进入", "不用了", "取消")
                if (confirm != null) {
                    LogUtils.d("尝试点击确定/我知道了/暂不进入")
                    AccessibilityUtil.performClick(confirm)
                }
            }
        }
    }
    sleep(500)
}

/**
 * 上传运行日志 简单封装 info log
 */
fun log(message: Any?, type: Int = WeworkMessageBean.ROBOT_LOG) {
    WeworkController.weworkService.webSocketManager.send(
        WeworkMessageListBean(
            WeworkMessageBean(
                null, null,
                type,
                null,
                null,
                null,
                if (message is String) message else GsonUtils.toJson(message)
            ),
            WeworkMessageListBean.SOCKET_TYPE_MESSAGE_LIST
        ), true
    )
}

/**
 * 上传运行日志 简单封装 error log
 */
fun error(message: Any?) {
    log(message, WeworkMessageBean.ROBOT_ERROR_LOG)
}

/**
 * 简单封装 sleep
 */
fun sleep(time: Long) {
    try {
        Thread.sleep(time)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}