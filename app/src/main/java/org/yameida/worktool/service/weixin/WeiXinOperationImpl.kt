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
    var istTest = false

    //token
    private val authorizationToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVaWQiOiI0OTgxIiwiTmFtZSI6IueOi-aZuiIsIkpnYm0iOiI1MTAxMTQwNTAwMDEwNDAwMDQiLCJNYWNoaW5lIjoiZWNiMGQ5MWNkMWE3OWQwYSIsIlJvbGUiOiIxIiwiSnpyeWJoIjoiNTEwMTE0MjAyMzA0MDA0NSIsIlB1c2hJZCI6Imp6LTQ5ODEiLCJTdXBwbGllciI6IjYiLCJleHAiOjE3NzY0ODIxODgsImlzcyI6ImhhbmRvbmdqd3QiLCJhdWQiOiJoYW5kb25nand0In0.ZiuVjRKY7oozdYU5BzMnKFIV9CaS8I_wQIlOPl5jMKo"
    private val name = "Wisdom"

    //同分钟内进行排重
    private var heartRemoveDuplicate = ""
    private var signRemoveDuplicate = ""
    private var otherSignRemoveDuplicate = ""

    //记录时间段打卡是否成功 进行排重处理
    private var signSuccess = ""
    private var otherSignSuccess = ""


    fun mainLoop() {
        while (true) {
            try {
                sleep(10000)

                if ((LocalTime.now().minute) % 60 == 0 || istTest) {
                    sendMsg("")/*发送心跳间隔时间*/
                }

                if (isCurrentTimeInRange(1) && isSignTime(1)) {
                    toSign(1)
                } else if (isCurrentTimeInRange(2) && isSignTime(2)) {
                    toSign(2)
                } else if (isCurrentTimeInRange(3) && isSignTime(3)) {
                    toSign(3)
                } else if (isCurrentTimeInRange(1) && isSignTime(1, true)) {
                    toOtherSign(1)//另一个
                } else if (isCurrentTimeInRange(2) && isSignTime(2, true)) {
                    toOtherSign(2)//另一个
                } else if (isCurrentTimeInRange(3) && isSignTime(3, true)) {
                    toOtherSign(3)//另一个
                }

            } catch (_: Exception) {
            } finally {
            }
        }
    }

    private fun sendMsg(txt: String) {

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        if (txt.isEmpty()) {
            val minute = LocalTime.now().minute.toString()
            val hour = LocalTime.now().hour.toString()
            val timeFlag = "$hour-$minute"
            if (heartRemoveDuplicate != timeFlag) {
                val isNotTimeRange =
                    !(!isCurrentTimeInRange(1) && !isCurrentTimeInRange(2) && !isCurrentTimeInRange(
                        3
                    ))
                ListViewManager.addItem(sdf.format(Date()) + isNotTimeRange.toString())
                heartRemoveDuplicate = timeFlag
                sleep(2000)
            }
        } else {
            ListViewManager.addItem(sdf.format(Date()) + "\n" + txt)
            sleep(2000)
        }
    }


    // 判断当前时间是否在指定时间段内
    fun isCurrentTimeInRange(type: Int): Boolean {
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
        } else if (type == 4) /*另一个的打卡时间*/ {
            return (currentTotalMinutes >= 8 * 60 && currentTotalMinutes <= 9 * 60) || (currentTotalMinutes >= 14 * 60 && currentTotalMinutes <= 15 * 60) || (currentTotalMinutes >= 20 * 60 && currentTotalMinutes <= 21 * 60)
        }
        return false // 不在范围内
    }


    ///另一个签到
    private fun toOtherSign(type: Int) {

        val day: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val successFlag = "$day-$type"
        val minute = LocalTime.now().minute.toString()
        val hour = LocalTime.now().hour.toString()
        val timeFlag = "$hour-$minute"

        if (otherSignRemoveDuplicate == timeFlag || otherSignSuccess == successFlag) return
        otherSignRemoveDuplicate = timeFlag
        NetWorking.getOtherToken { result, address ->
            if (result) {
                otherSignRemoveDuplicate =
                    LocalTime.now().hour.toString() + "-" + LocalTime.now().minute.toString()
                sendMsg("另一个成功：${address}")
                otherSignSuccess = "$day-$type"
            } else {
                otherSignRemoveDuplicate =
                    LocalTime.now().hour.toString() + "-" + LocalTime.now().minute.toString()
                sendMsg("另一个失败：${address}")
            }
        }
    }

    ///是否是此时段签到时间
    private fun isSignTime(type: Int, isOther: Boolean = false): Boolean {
        val calendar: Calendar = Calendar.getInstance()
        val currentMinute: Int = calendar.get(Calendar.MINUTE) // 获取当前分钟
        val dayOfWeek: Int = calendar.get(Calendar.DAY_OF_MONTH) % 8 // 获取周几

        var baseValue = dayOfWeek + 4
        if (type == 1) baseValue -= (1 + (dayOfWeek % 2))
        else if (type == 2) baseValue += (1 + (dayOfWeek % 2))
        else if (type == 3) baseValue -= (1 + (dayOfWeek % 3))

        if (isOther) baseValue += 2

        return currentMinute == baseValue || currentMinute == (30 + baseValue)
    }

    fun toSign(type: Int) {
        val day: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val successFlag = "$day-$type"
        val minute = LocalTime.now().minute.toString()
        val hour = LocalTime.now().hour.toString()
        val timeFlag = "$hour-$minute"
        if (signRemoveDuplicate == timeFlag || signSuccess == successFlag) return
        signRemoveDuplicate = timeFlag


        val dateDay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        var ruleId: String = ""
        var random: MyAddress = MyAddress("", "", "")
        if (type == 1) {
            ruleId = "147"
            random = homeAddressLists.random()
        } else if (type == 2) {
            ruleId = "148"
            val local = LocalDate.now()
            val isHoliday = holidayLists.contains("${local.month.value}-${local.dayOfMonth}")
            val isWorkDay = workDayLists.contains("${local.month.value}-${local.dayOfMonth}")
            val dayOfWeek = local.dayOfWeek
            if (!isWorkDay && (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY || isHoliday)) {
                random = homeAddressLists.random()
            } else {
                random = workAddressLists.random()
            }
        } else if (type == 3) {
            ruleId = "149"
            random = homeAddressLists.random()
        }

        if (ruleId.isEmpty() || random.longitude.isEmpty() || random.latitude.isEmpty() || random.address.isEmpty()) return

        postToSign(
            type,
            "http://1.14.111.130:9000/api/attendancemange",
            mapOf(
                "reportDate" to dateDay.format(Date()),//2025/01/10
                "reportTime" to dateTime.format(Date()),//"10:54:34
                "longitude" to random.longitude,
                "latitude" to random.latitude,
                "address" to random.address,
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
        type: Int, url: String, formData: Map<String, String>, headers: Map<String, String>
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
                        if (apiResponse.code == 100 && "success" == apiResponse.msg.toString()) {
                            val day: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            signSuccess = "$day-$type"
                        }
                    }
                } else {
                    sendMsg("@${name} 失败了 ${response.code}=${response.message}")
                    sleep(2000)
                    postToSignList()
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }


    //签到请求列表
    fun postToSignList(isToast: Boolean = false) {
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
                if (isToast) ToastUtils.showLong("${e.message}")
                else sendMsg("@${name} " + e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        try {
                            val apiResponse = gson.fromJson(responseBody, SignList::class.java)
                            println("Response: $apiResponse")
                            var msg: String = ""
                            apiResponse.data?.forEach { item ->
                                val subStr = item.address?.substring(item.address.length - 6)
                                msg += "\n${item.id}=${item.reportTime}--$subStr;"
                            }
                            if (isToast) ToastUtils.showLong(msg)
                            else sendMsg("@${name} 签到列表" + msg)
                        } catch (e: Exception) {
                        }
                    }
                } else {
                    if (isToast) ToastUtils.showLong("${response.code}")
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }
}

// 定义返回数据类，用于解析 JSON
data class ApiResponse(
    val code: Int, val data: Any, val msg: Any?
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
