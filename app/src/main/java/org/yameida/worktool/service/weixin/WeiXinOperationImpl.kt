package org.yameida.worktool.service.weixin

import android.annotation.SuppressLint
import com.blankj.utilcode.util.*
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.yameida.worktool.Constant
import org.yameida.worktool.service.WeworkController
import org.yameida.worktool.service.getRoot
import org.yameida.worktool.service.sleep
import org.yameida.worktool.utils.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * 微信操作类
 */
@SuppressLint("NewApi")
object WeiXinOperationImpl {
    //token
    private val authorizationToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVaWQiOiI0OTgxIiwiTmFtZSI6IueOi-aZuiIsIkpnYm0iOiI1MTAxMTQwNTAwMDEwNDAwMDQiLCJNYWNoaW5lIjoiZWNiMGQ5MWNkMWE3OWQwYSIsIlJvbGUiOiIxIiwiSnpyeWJoIjoiNTEwMTE0MjAyMzA0MDA0NSIsIlB1c2hJZCI6Imp6LTQ5ODEiLCJTdXBwbGllciI6IjYiLCJleHAiOjE3NzY0ODIxODgsImlzcyI6ImhhbmRvbmdqd3QiLCJhdWQiOiJoYW5kb25nand0In0.ZiuVjRKY7oozdYU5BzMnKFIV9CaS8I_wQIlOPl5jMKo"
    private val roomName = "A同行"
    private val name = "Wisdom"
    private var heartRemoveDuplicate = ""
    private var signRemoveDuplicate = ""
    private var otherSignRemoveDuplicate = ""//另一个排重
    private val holidayLists = listOf(
        "1-28",
        "1-29",
        "1-30",
        "1-31",
        "2-3",
        "2-4",
        "4-5",
        "5-1",
        "5-2",
        "5-5",
        "6-2",
        "10-1",
        "10-2",
        "10-3",
        "10-6",
        "10-7",
        "10-8"
    )


    fun mainLoop() {
        while (true) {
            try {
                sleep(10000)
                if (!isWeiXin()) {
                    goWeiXin()
                } else if (!isRoom()) {
                    goRoom()
                } else {
                    if ((LocalTime.now().minute) % 10 == 0) {
                        sendMsg("")/*发送心跳间隔时间*/
                    }

                    if (isCurrentTimeInRange(1) && isSignTime(1)) {
                        toSign(1)
                    } else if (isCurrentTimeInRange(2) && isSignTime(2)) {
                        toSign(2)
                    } else if (isCurrentTimeInRange(3) && isSignTime(3)) {
                        toSign(3)
                    } else if (isOtherSignTime()) {
                        toOtherSign()//另一个
                    }
                }
            } catch (e: Exception) {
            } finally {
            }
        }
    }

    private fun isWeiXin(): Boolean {
        while (true) {
            val tempRoot = WeworkController.weworkService.rootInActiveWindow
            val root = WeworkController.weworkService.rootInActiveWindow
            if (tempRoot != root) {
                LogUtils.e("tempRoot != root")
            } else if (root != null) {
                if (root.packageName == Constant.PACKAGE_NAMES) {
                    return true
                } else {
                    LogUtils.e("当前微信: ${root.packageName}")
                    return false
                }
            }
            sleep(1000)
        }
    }

    ///是否在指定房间
    private fun isRoom(): Boolean {
        while (true) {
            val tempRoot = WeworkController.weworkService.rootInActiveWindow
            val root = WeworkController.weworkService.rootInActiveWindow
            if (tempRoot != root) {
                LogUtils.e("tempRoot != root")
            } else if (root != null) {
                if (AccessibilityUtil.findOneByText(root, "$roomName(10)") != null) {
                    return true
                } else {
                    LogUtils.e("当前在指定room: ${root.packageName}")
                    return false
                }
            }
            sleep(1000)
        }
    }

    private fun goRoom() {
        sleep(5000)
        val result = AccessibilityUtil.findTextAndClick(getRoot(true), roomName)
        LogUtils.e("进入指定room: $result")
    }

    private fun goWeiXin() {
        AccessibilityUtil.globalGoHome(WeworkController.weworkService)
        sleep(5000)
        val result = AccessibilityUtil.findTextAndClick(getRoot(true), "微信")
        if (!result) {
            sleep(5000)
            AccessibilityUtil.performXYClick(WeworkController.weworkService, 110f, 200f)
        }
    }

    private fun sendMsg(txt: String) {
        if (!isRoom()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        if (txt.isEmpty()) {
            val currentMinute = LocalTime.now().minute.toString()
            if (heartRemoveDuplicate != currentMinute) {
                val isNotTimeRange =
                    !(!isCurrentTimeInRange(1) && !isCurrentTimeInRange(2) && !isCurrentTimeInRange(
                        3
                    ))
                val inputResult = AccessibilityUtil.findTextInput(
                    getRoot(), sdf.format(Date()) + isNotTimeRange.toString()
                )
                sleep(2000)
                val result = AccessibilityUtil.findTextAndClick(getRoot(), "发送")
                if (result) {
                    heartRemoveDuplicate = currentMinute
                } else if (inputResult) {
                    AccessibilityUtil.performXYClick(WeworkController.weworkService, 650f, 1230f)
                    LogUtils.e("发送点击指定坐标: 650，1230")
                    heartRemoveDuplicate = currentMinute
                }
            }
        } else {
            val inputResult =
                AccessibilityUtil.findTextInput(getRoot(), txt + "\n" + sdf.format(Date()))
            sleep(2000)
            val result = AccessibilityUtil.findTextAndClick(getRoot(), "发送")
            if (!result && inputResult) {
                AccessibilityUtil.performXYClick(WeworkController.weworkService, 650f, 1230f)
                LogUtils.e("发送点击指定坐标: 650，1230")
            }
        }
    }


    // 判断当前时间是否在指定时间段内
    private fun isCurrentTimeInRange(type: Int): Boolean {
        // 获取当前时间
        val calendar: Calendar = Calendar.getInstance()
        val currentHour: Int = calendar.get(Calendar.HOUR_OF_DAY) // 获取当前小时（24小时制）
        val currentMinute: Int = calendar.get(Calendar.MINUTE) // 获取当前分钟
        // 将当前时间转换为总分钟数
        val currentTotalMinutes = currentHour * 60 + currentMinute

        // 定义时间段（转换为总分钟数）
        if (type == 1) {
            return currentTotalMinutes >= 8 * 60 && currentTotalMinutes <= 10 * 60 // 8:00 - 10:00
        } else if (type == 2) {
            return currentTotalMinutes >= 14 * 60 && currentTotalMinutes <= 16 * 60 // 14:00 - 16:00
        } else if (type == 3) {
            return currentTotalMinutes >= 20 * 60 && currentTotalMinutes <= 22 * 60 // 20:00 - 22:00
        } else if (type == 4) {
            return currentTotalMinutes >= 7 * 60 && currentTotalMinutes <= 8 * 60 // 20:00 - 22:00
        }
        return false // 不在范围内
    }

    ///第二个app是否在签到时间段
    private fun isOtherSignTime(): Boolean {
        val calendar: Calendar = Calendar.getInstance()
        val hour: Int = calendar.get(Calendar.HOUR_OF_DAY) // 获取当前分钟
        val dayOfWeek: Int = calendar.get(Calendar.DAY_OF_WEEK) // 获取周几
        val minute: Int = calendar.get(Calendar.MINUTE) // 获取当前分钟
        val baseValue = dayOfWeek + 2

        return (hour == 7 && minute == baseValue) || (hour == 22 && minute == baseValue)
    }

    ///另一个签到
    fun toOtherSign() {
        val currentMinute = LocalTime.now().minute.toString()
        if (otherSignRemoveDuplicate == currentMinute) return
        otherSignRemoveDuplicate = currentMinute
        NetWorking.getOtherToken { result ->
            if (result) {
                otherSignRemoveDuplicate = LocalTime.now().minute.toString()
                sendMsg("另一个成功")
            } else {
                otherSignRemoveDuplicate = LocalTime.now().minute.toString()
                sendMsg("另一个失败")
            }
        }
    }

    ///是否是此时段签到时间
    private fun isSignTime(type: Int): Boolean {
        val calendar: Calendar = Calendar.getInstance()
        val currentMinute: Int = calendar.get(Calendar.MINUTE) // 获取当前分钟
        val dayOfWeek: Int = calendar.get(Calendar.DAY_OF_WEEK) // 获取周几

        val baseValue = dayOfWeek + 2
        return currentMinute == baseValue || currentMinute == (30 + baseValue)
    }

    fun toSign(type: Int) {
        val currentMinute = LocalTime.now().minute.toString()
        if (signRemoveDuplicate == currentMinute) return
        signRemoveDuplicate = currentMinute


        val dateDay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        var ruleId: String = ""
        var longitude: String = ""
        var latitude: String = ""
        var address: String = ""
        if (type == 1) {
            ruleId = "147"
            longitude = "104.066444"
            latitude = "30.769059"
            address = "中国四川省成都市新都区仁爱路152号欣茂·大峰景"
        } else if (type == 2) {
            ruleId = "148"
            longitude = "104.09778"
            latitude = "30.653439"
            val local = LocalDate.now()
            val isHoliday = holidayLists.contains("${local.month.value}-${local.dayOfMonth}")
            val dayOfWeek = local.dayOfWeek
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY || isHoliday) {
                address = "中国四川省成都市新都区仁爱路152号欣茂·大峰景"
            } else {
                address = "中国四川省成都市成华区一环路东三段2-8号玉双路(地铁站)"
            }
        } else if (type == 3) {
            ruleId = "149"
            longitude = "104.066444"
            latitude = "30.769059"
            address = "中国四川省成都市新都区仁爱路152号欣茂·大峰景"
        }

        if (ruleId.isEmpty() || longitude.isEmpty() || latitude.isEmpty() || address.isEmpty()) return

        postToSign(
            "http://1.14.111.130:9000/api/attendancemange",
            mapOf(
                "reportDate" to dateDay.format(Date()),//2025/01/10
                "reportTime" to dateTime.format(Date()),//"10:54:34
                "longitude" to longitude,
                "latitude" to latitude,
                "address" to address,
                "orgId" to "49",
                "jgbm" to "510114050001040004",
                "personNum" to "5101142023040045",
                "pmId" to "0",
                "reportType" to "0",
                "ruleId" to ruleId,
                "addressType" to "0",
            ),
            mapOf(
                "Authorization" to authorizationToken,
            ),
        )
    }

    //签到请求
    fun postToSign(
        url: String, formData: Map<String, String>, headers: Map<String, String>
    ) {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()

        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()
        formData.forEach { (key, value) ->
            formBodyBuilder.add(key, value)
        }
        val formBody = formBodyBuilder.build()

        // 构建请求
        val requestBuilder = Request.Builder().url(url).post(formBody)
        // 添加 Headers
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Request failed: ${e.message}")
                sendMsg("@${name} " + e.message.toString())
                sleep(2000)
                postToSignList()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        // 解析 JSON
                        val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                        println("Response: $apiResponse")
                        sendMsg("@${name} " + apiResponse.msg.toString())
                        sleep(2000)
                        postToSignList()
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }


    //签到请求列表
    fun postToSignList() {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()

        // 构建请求
        val requestBuilder =
            Request.Builder().url("http://1.14.111.130:9000/api/attendancemange/list").get()
        // 添加 Headers
        mapOf(
            "Authorization" to authorizationToken,
        ).forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Request failed: ${e.message}")
                sendMsg("@${name} " + e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        try {
                            val apiResponse = gson.fromJson(responseBody, SignList::class.java)
                            println("Response: $apiResponse")
                            var msg: String = ""
                            apiResponse.data?.forEach { item ->
                                msg += "\n${item.id}=${item.reportTime};"
                            }
                            sendMsg("@${name} 签到列表" + msg)
                        } catch (e: Exception) {
                        }
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }
}

// 定义返回数据类，用于解析 JSON
data class ApiResponse(
    val code: Any, val data: Any, val msg: Any?
)

//签到列表
data class SignList(
    val code: Int?, val data: List<DataItem>?, val count: Int?, val msg: String?
)

data class DataItem(
    val id: Int?,
    val sTime: String?,
    val eTime: String?,
    val endTime: String?,
    val jgbm: String?,
    val orgId: Int?,
    val reportDate: String?,
    val reportTime: String?,
    val longitude: Double?,
    val latitude: Double?,
    val address: String?,
    val personNum: String?,
    val uuid: String?,
    val reportType: Int?,
    val ruleId: Int?,
    val addressType: Int?,
    val remark: String?
)
