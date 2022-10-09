package org.yameida.worktool.service

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.google.gson.reflect.TypeToken
import okhttp3.WebSocket
import org.yameida.worktool.Constant
import org.yameida.worktool.model.WeworkMessageBean
import org.yameida.worktool.model.WeworkMessageListBean
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

object MyLooper {

    private var threadHandler: Handler? = null

    val looper = thread {
        LogUtils.e("myLooper starting...")
        Looper.prepare()
        val myLooper = Looper.myLooper()
        if (myLooper != null) {
            threadHandler = object : Handler(myLooper) {
                override fun handleMessage(msg: Message) {
                    LogUtils.d("handle message: " + Thread.currentThread().name, msg)
                    try {
                        dealWithMessage(msg.obj as WeworkMessageBean)
                    } catch (e: Exception) {
                        LogUtils.e(e)
                    }
                }
            }
        } else {
            LogUtils.e("myLooper is null!")
        }
        Looper.loop()
    }

    fun init() {}

    fun getInstance(): Handler {
        while (true) {
            threadHandler?.let { return it }
            LogUtils.e("threadHandler is not ready...")
        }
    }

    fun onMessage(webSocket: WebSocket?, text: String) {
        val messageList =
            GsonUtils.fromJson<WeworkMessageListBean>(text, WeworkMessageListBean::class.java)
        if (messageList.socketType == WeworkMessageListBean.SOCKET_TYPE_HEARTBEAT) {
            return
        }
        if (messageList.socketType == WeworkMessageListBean.SOCKET_TYPE_MESSAGE_CONFIRM) {
            return
        }
        if (messageList.socketType == WeworkMessageListBean.SOCKET_TYPE_MESSAGE_LIST) {
            val confirm = WeworkController.weworkService.webSocketManager.confirm(
                messageList.messageId, messageList.list.firstOrNull()?.type ?: -1
            )
            if (!confirm) return
            if (messageList.encryptType == 1) {
                val decryptHexStringAES = EncryptUtils.decryptHexStringAES(
                    messageList.encryptedList,
                    Constant.key,
                    Constant.transformation,
                    Constant.iv
                )
                messageList.list =
                    GsonUtils.fromJson(
                        String(decryptHexStringAES, StandardCharsets.UTF_8),
                        object : TypeToken<ArrayList<WeworkMessageBean>>() {}.type
                    )
            }
            //去重处理 丢弃之前的重复指令 丢弃之前的获取新消息指令
            for (message in LinkedHashSet(messageList.list)) {
                message.objMessageId = messageList.messageId//每个obj对象也记录一下messageId
                if (message.type == WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE) {
                    WeworkController.enableLoopRunning = true
                } else {
                    WeworkController.mainLoopRunning = false
                    getInstance().removeMessages(message.type * message.hashCode())
                    getInstance().sendMessage(Message.obtain().apply {
                        what = message.type * message.hashCode()
                        obj = message
                    })
                }
                getInstance().removeMessages(WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE)
                getInstance().sendMessage(Message.obtain().apply {
                    what = WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE
                    obj = WeworkMessageBean().apply { type = WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE }
                })
            }
        }
    }

    private fun dealWithMessage(message: WeworkMessageBean) {
        when (message.type) {
            WeworkMessageBean.STOP_AND_GO_HOME -> {
                WeworkController.stopAndGoHome()
            }
            WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE -> {
                WeworkController.loopReceiveNewMessage()
            }
            WeworkMessageBean.SEND_MESSAGE -> {
                val result = WeworkController.sendMessage(message)
                confirmCmd(result, message)
            }
            WeworkMessageBean.REPLY_MESSAGE -> {
                WeworkController.replyMessage(message)
            }
            WeworkMessageBean.RELAY_MESSAGE -> {
                WeworkController.relayMessage(message)
            }
            WeworkMessageBean.INIT_GROUP -> {
                val result = WeworkController.initGroup(message)
                confirmCmd(result, message)
            }
            WeworkMessageBean.INTO_GROUP_AND_CONFIG -> {
                val result = WeworkController.intoGroupAndConfig(message)
                confirmCmd(result, message)
            }
            WeworkMessageBean.PUSH_MICRO_DISK_IMAGE -> {
                WeworkController.pushMicroDiskImage(message)
            }
            WeworkMessageBean.PUSH_MICRO_DISK_FILE -> {
                WeworkController.pushMicroDiskFile(message)
            }
            WeworkMessageBean.PUSH_MICROPROGRAM -> {
                WeworkController.pushMicroprogram(message)
            }
            WeworkMessageBean.PUSH_OFFICE -> {
                WeworkController.pushOffice(message)
            }
            WeworkMessageBean.PASS_ALL_FRIEND_REQUEST -> {
            }
            WeworkMessageBean.ADD_FRIEND_BY_PHONE -> {
                WeworkController.addFriendByPhone(message)
            }
            WeworkMessageBean.SHOW_GROUP_INFO -> {
                WeworkController.showGroupInfo(message)
            }
            WeworkMessageBean.GET_GROUP_INFO -> {
                WeworkController.getGroupInfo(message)
            }
            WeworkMessageBean.GET_FRIEND_INFO -> {
                WeworkController.getFriendInfo(message)
            }
            WeworkMessageBean.GET_MY_INFO -> {
                WeworkController.getMyInfo()
            }
            WeworkMessageBean.ROBOT_CONTROLLER_TEST -> {
                WeworkController.test(message)
            }
        }
    }

    private fun confirmCmd(result: Boolean, message: WeworkMessageBean) {
        if (result) {
            getInstance().removeMessages(message.type * message.hashCode())
            WeworkController.weworkService.webSocketManager.send(
                WeworkMessageListBean(message.objMessageId, WeworkMessageListBean.SOCKET_TYPE_MESSAGE_CONFIRM)
            )
        }
    }
}