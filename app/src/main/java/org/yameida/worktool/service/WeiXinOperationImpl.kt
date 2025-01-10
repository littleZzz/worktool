package org.yameida.worktool.service

import com.blankj.utilcode.util.*
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.yameida.worktool.Constant
import org.yameida.worktool.utils.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random


/**
 * 微信操作类
 */
object WeiXinOperationImpl {
    private val name = "Wisdom"
    private var heartRemoveDuplicate = ""
    private var signRemoveDuplicate = ""


    fun mainLoop() {
        while (true) {
            try {
                sleep(5000)
                if (!isWeiXin()) {
                    AccessibilityUtil.globalGoHome(WeworkController.weworkService)
                    sleep(2000)
                    AccessibilityUtil.findTextAndClick(getRoot(true), "微信")
                } else {
                    if (Calendar.getInstance().get(Calendar.MINUTE) % 2 == 0) {
                        sendMsg("")/*发送心跳间隔时间*/
                    }

                    if (isCurrentTimeInRange(1) && isSignTime(1)) {
                        toSign(1)
                    }
                    if (isCurrentTimeInRange(2) && isSignTime(2)) {
                        toSign(2)
                    }
                    if (isCurrentTimeInRange(3) && isSignTime(3)) {
                        toSign(3)
                    }
                }
            } catch (e: Exception) {
            } finally {
            }
        }
    }

    fun toSign(type: Int) {
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE).toString();
        if (signRemoveDuplicate == currentMinute) return
        signRemoveDuplicate = currentMinute

//        sendMsg("sign time===" + type)
//        return;

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
            address = "中国四川省成都市成华区一环路东三段2-8号玉双路(地铁站)"
        } else if (type == 3) {
            ruleId = "149"
            longitude = "104.067258"
            latitude = "30.769958"
            address = "中国四川省成都市新都区赵家寺路340号保利·春天花语"
        }

        if (ruleId.isEmpty() || longitude.isEmpty() || latitude.isEmpty() || address.isEmpty()) return

        postRequest(
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
                "Authorization" to "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVaWQiOiI0OTgxIiwiTmFtZSI6IueOi-aZuiIsIkpnYm0iOiI1MTAxMTQwNTAwMDEwNDAwMDQiLCJNYWNoaW5lIjoiZWNiMGQ5MWNkMWE3OWQwYSIsIlJvbGUiOiIxIiwiSnpyeWJoIjoiNTEwMTE0MjAyMzA0MDA0NSIsIlB1c2hJZCI6Imp6LTQ5ODEiLCJTdXBwbGllciI6IjYiLCJleHAiOjE3NzY0ODIxODgsImlzcyI6ImhhbmRvbmdqd3QiLCJhdWQiOiJoYW5kb25nand0In0.ZiuVjRKY7oozdYU5BzMnKFIV9CaS8I_wQIlOPl5jMKo",
            ),
        )
    }

    fun postRequest(
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
                sendMsg("@${name}" + " " + e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        // 解析 JSON
                        val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                        println("Response: $apiResponse")
                        sendMsg("@${name}" + " " + apiResponse.msg.toString())
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })
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

    private fun sendMsg(txt: String) {
        if (!isWeiXin()) {
            AccessibilityUtil.globalGoHome(WeworkController.weworkService)
            sleep(5000)
            AccessibilityUtil.findTextAndClick(getRoot(true), "微信")
            sleep(15000)
        }
        AccessibilityUtil.findTextAndClick(getRoot(), name)
        sleep(2000)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE).toString();
        if (txt.isEmpty()) {
            if (heartRemoveDuplicate != currentMinute) {
                val notTimeRange =
                    !(!isCurrentTimeInRange(1) && !isCurrentTimeInRange(2) && !isCurrentTimeInRange(
                        3
                    ))
                AccessibilityUtil.findTextInput(
                    getRoot(), sdf.format(Date()) + notTimeRange.toString()
                )
                sleep(2000)
                val result = AccessibilityUtil.findTextAndClick(getRoot(), "发送")
                if (result) {
                    heartRemoveDuplicate = currentMinute
                }
            }
        } else {
            AccessibilityUtil.findTextInput(getRoot(), sdf.format(Date()) + "\n\r" + txt)
            sleep(2000)
            val result = AccessibilityUtil.findTextAndClick(getRoot(), "发送")
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

    ///是否是此时段签到时间
    private fun isSignTime(type: Int): Boolean {
        val calendar: Calendar = Calendar.getInstance()
        val currentMinute: Int = calendar.get(Calendar.MINUTE) // 获取当前分钟
        val DAY_OF_WEEK: Int = calendar.get(Calendar.DAY_OF_WEEK) // 获取周几

        if (currentMinute % (15 + type * 2 + DAY_OF_WEEK) == 1) return true
        else return false

    }
}

// 定义返回数据类，用于解析 JSON
data class ApiResponse(
    val code: Any, val data: Any, val msg: Any?
)