package org.yameida.worktool.utils

import android.view.accessibility.AccessibilityNodeInfo
import org.yameida.worktool.utils.AccessibilityUtil.findOneByClazz
import org.yameida.worktool.utils.AccessibilityUtil.findFrontNode
import org.yameida.worktool.model.WeworkMessageBean
import com.blankj.utilcode.util.LogUtils
import org.yameida.worktool.Constant
import org.yameida.worktool.service.backPress
import org.yameida.worktool.service.getRoot
import org.yameida.worktool.service.goHome
import org.yameida.worktool.service.sleep
import org.yameida.worktool.utils.AccessibilityUtil.findAllOnceByClazz

/**
 * 房间特征分析工具类
 */
object WeworkRoomUtil {

    /**
     * 房间类型 ROOM_TYPE
     * @see WeworkMessageBean.ROOM_TYPE
     */
    fun getRoomType(print: Boolean = true): Int {
        val roomTitle = getRoomTitle()
        when {
            isExternalSingleChat(roomTitle) -> {
                LogUtils.d("ROOM_TYPE: ROOM_TYPE_EXTERNAL_CONTACT")
                return WeworkMessageBean.ROOM_TYPE_EXTERNAL_CONTACT
            }
            isExternalGroup() -> {
                LogUtils.d("ROOM_TYPE: ROOM_TYPE_EXTERNAL_GROUP")
                return WeworkMessageBean.ROOM_TYPE_EXTERNAL_GROUP
            }
            isGroupChat(roomTitle) -> {
                LogUtils.d("ROOM_TYPE: ROOM_TYPE_INTERNAL_GROUP")
                return WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP
            }
            isSingleChat() -> {
                LogUtils.d("ROOM_TYPE: ROOM_TYPE_INTERNAL_CONTACT")
                return WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT
            }
            else -> {
                if (print) LogUtils.d("ROOM_TYPE: ROOM_TYPE_UNKNOWN")
                return WeworkMessageBean.ROOM_TYPE_UNKNOWN
            }
        }
    }

    /**
     * 房间类型
     * @see WeworkMessageBean.ROOM_TYPE_UNKNOWN
     * @see WeworkMessageBean.ROOM_TYPE_EXTERNAL_GROUP
     * @see WeworkMessageBean.ROOM_TYPE_EXTERNAL_CONTACT
     * @see WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP
     * @see WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT
     */
    fun getRoomTitle(print: Boolean = true): ArrayList<String> {
        val titleList = arrayListOf<String>()
        //聊天消息列表 1ListView 0RecycleView xViewGroup
        val list = AccessibilityUtil.findOnceByClazz(getRoot(), Views.ListView)
        if (list != null) {
            val frontNode = findFrontNode(list.parent.parent)
            val textViewList = findAllOnceByClazz(frontNode, Views.TextView)
            for (textView in textViewList) {
                if (!textView.text.isNullOrBlank()) {
                    val text = textView.text.toString()
                    titleList.add(text.replace("\\(\\d+\\)$".toRegex(), ""))
                    if (text.contains("\\(\\d+\\)$".toRegex())) {
                        titleList.add(text)
                    }
                }
            }
        }
        if (print) LogUtils.v("getRoomTitle: ", titleList)
        return titleList
    }

    /**
     * 进入房间（单聊或群聊）
     */
    fun intoRoom(title: String): Boolean {
        LogUtils.d("intoRoom(): $title")
        val titleList = getRoomTitle(false)
        val roomType = getRoomType()
        if (roomType != WeworkMessageBean.ROOM_TYPE_UNKNOWN
            && titleList.count {
                it.replace("…", "").replace("\\(.*?\\)".toRegex(), "") == title.replace("…", "")
                    .replace("\\(.*?\\)".toRegex(), "")
            } > 0
        ) {
            LogUtils.d("当前正在房间")
            return true
        }
        goHome()
        val list = findOneByClazz(getRoot(), Views.RecyclerView, Views.ListView, Views.ViewGroup)
        if (list != null) {
            val frontNode = findFrontNode(list)
            val textViewList = findAllOnceByClazz(frontNode, Views.TextView)
            if (textViewList.size >= 2) {
                val searchButton: AccessibilityNodeInfo = textViewList[textViewList.size - 2]
                val multiButton: AccessibilityNodeInfo = textViewList[textViewList.size - 1]
                AccessibilityUtil.performClick(searchButton)
                AccessibilityUtil.findTextInput(getRoot(), title.replace("…", "").replace("-.*$".toRegex(), ""))
                sleep(Constant.CHANGE_PAGE_INTERVAL)
                //消息页搜索结果列表
                val selectListView = findOneByClazz(getRoot(), Views.ListView)
                val imageView = AccessibilityUtil.findOnceByClazz(selectListView, Views.ImageView)
                if (imageView != null) {
                    AccessibilityUtil.performClick(imageView)
                    LogUtils.d("进入房间: $title")
                    sleep(Constant.CHANGE_PAGE_INTERVAL)
                    return true
                } else {
                    LogUtils.e("未搜索到结果")
                }
            } else {
                LogUtils.e("未找到搜索按钮")
            }
        } else {
            LogUtils.e("未找到聊天列表")
        }
        return false
    }

    /**
     * 进入群管理页
     * @return true 成功进入群管理页
     */
    fun intoGroupManager(): Boolean {
        if (AccessibilityUtil.findOneByText(getRoot(), "全部群成员", "微信用户创建", timeout = Constant.CHANGE_PAGE_INTERVAL) != null) {
            return true
        }
        //群详情列表
        val list = findOneByClazz(getRoot(), Views.ListView)
        if (list != null) {
            val frontNode = AccessibilityUtil.findFrontNode(list.parent.parent)
            val textViewList = findAllOnceByClazz(frontNode, Views.TextView)
            if (textViewList.size >= 2) {
                val multiButton = textViewList.lastOrNull()
                AccessibilityUtil.performClick(multiButton)
                sleep(Constant.CHANGE_PAGE_INTERVAL)
                return true
            } else {
                LogUtils.e("未找到群管理按钮")
            }
        }
        return false
    }

    /**
     * 进入好友详情页
     * @return true 成功进入好友详情页
     */
    fun intoFriendDetail(): Boolean {
        if (AccessibilityUtil.findOneByText(getRoot(), "设置聊天背景") != null) {
            return true
        }
        //同群详情列表
        val list = findOneByClazz(getRoot(), Views.ListView)
        if (list != null) {
            val frontNode = AccessibilityUtil.findFrontNode(list.parent.parent)
            val textViewList = findAllOnceByClazz(frontNode, Views.TextView)
            if (textViewList.size >= 2) {
                val multiButton = textViewList.lastOrNull()
                AccessibilityUtil.performClick(multiButton)
                return true
            } else {
                LogUtils.e("未找到好友详情按钮")
            }
        }
        return false
    }

    /**
     * 获取当前聊天人姓名并返回房间
     * 解决title为对方正在输入中问题
     * @return name 单聊对方姓名
     */
    fun getFriendName(): ArrayList<String> {
        val titleList = arrayListOf<String>()
        if (intoFriendDetail()) {
            val gridView = findOneByClazz(getRoot(), Views.GridView)
            if (gridView != null && gridView.childCount >= 2) {
                val tvList = findAllOnceByClazz(gridView.getChild(0), Views.TextView)
                for (textView in tvList) {
                    if (textView.text != null) {
                        titleList.add(textView.text.toString())
                        backPress()
                    }
                }
            }
        }
        return titleList
    }

    /**
     * 群名是否存在
     */
    fun isGroupExists(groupName: String): Boolean {
        return intoRoom(groupName)
    }

    /**
     * 是否是群聊
     * 群名最后有(\d)显示群人数
     */
    private fun isGroupChat(roomTitle: ArrayList<String>): Boolean {
        return roomTitle.size > 1 && roomTitle[1].contains("\\(\\d+\\)$".toRegex())
    }

    /**
     * 是否是外部群
     * listview前兄弟控件 && text包含外部群
     */
    private fun isExternalGroup(): Boolean {
        //聊天消息列表 1ListView 0RecycleView xViewGroup
        val listView = AccessibilityUtil.findOnceByClazz(getRoot(), Views.ListView, limitDepth = null, depth = 0)
        if (listView != null) {
            val frontNode = findFrontNode(listView)
            if (frontNode != null) {
                val nodeList = AccessibilityUtil.findAllOnceByText(frontNode, "外部群")
                return nodeList.isNotEmpty()
            }
        }
        return false
    }

    /**
     * 是否是单聊
     * 有列表和输入框
     */
    private fun isSingleChat(): Boolean {
        //聊天消息列表 1ListView 0RecycleView xViewGroup
        val list = AccessibilityUtil.findOnceByClazz(getRoot(), Views.ListView)
        val editText = AccessibilityUtil.findOnceByClazz(getRoot(), Views.EditText)
            ?: AccessibilityUtil.findOnceByText(getRoot(), "按住 说话", "按住说话", exact = true)
        if (list != null && editText != null) {
            return true
        }
        return false
    }

    /**
     * 是否是外部单聊
     * 姓名下面有@xx
     */
    private fun isExternalSingleChat(roomTitle: ArrayList<String>): Boolean {
        return roomTitle.size > 1 && roomTitle.count { it.matches("^[@＠].*?".toRegex()) } > 0
    }

}