package org.yameida.worktool.service.weixin

import android.annotation.SuppressLint
import com.blankj.utilcode.util.ToastUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.yameida.worktool.MyApplication
import org.yameida.worktool.R
import org.yameida.worktool.service.weixin.WeiXinOperationImpl.holidayLists
import org.yameida.worktool.service.weixin.WeiXinOperationImpl.isCurrentTimeInRange
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale


/**
 * 微信操作类
 */
@SuppressLint("NewApi")
object NetWorking {


    //token
    private val authorizationToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVaWQiOiI0OTgxIiwiTmFtZSI6IueOi-aZuiIsIkpnYm0iOiI1MTAxMTQwNTAwMDEwNDAwMDQiLCJNYWNoaW5lIjoiZWNiMGQ5MWNkMWE3OWQwYSIsIlJvbGUiOiIxIiwiSnpyeWJoIjoiNTEwMTE0MjAyMzA0MDA0NSIsIlB1c2hJZCI6Imp6LTQ5ODEiLCJTdXBwbGllciI6IjYiLCJleHAiOjE3NzY0ODIxODgsImlzcyI6ImhhbmRvbmdqd3QiLCJhdWQiOiJoYW5kb25nand0In0.ZiuVjRKY7oozdYU5BzMnKFIV9CaS8I_wQIlOPl5jMKo"


    ///登录-上传图片-签到-save
    ///获取Other app  token
    fun getOtherToken(callback: (Boolean, String) -> Unit) {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()
        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()
        mapOf(
            "password" to "7afa5b5d363ab40f",
            "imei" to "b5cb2e2e1cd4c21a647f19d49522ed793b476c596ab156fe",
            "id" to "77781f9d7ef8583565a2f5e84cea9879c680af2511efdd8b",
            "type" to "1",
        ).forEach { (key, value) ->
            formBodyBuilder.add(key, value)
        }
        val formBody = formBodyBuilder.build()

        // 构建请求
        val requestBuilder =
            Request.Builder().url("http://202.61.88.14:8093/jzdxapp/v2/login").post(formBody)
        val request = requestBuilder.build()
        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Request failed: ${e.message}")
                callback(false, "getOtherToken-无地址")//回调
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        println("responseBody: $jsonObject")
                        val dataObject = jsonObject.getAsJsonObject("data")
                        val token = dataObject.get("token").asString
                        println("Token: $token")
                        //登录成功 上传图片
                        otherUploadPic(token, callback)
                    }
                } else {
                    callback(false, "getOtherToken-无地址")//回调
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }

    ///上传图片
    @SuppressLint("ResourceType")
    fun otherUploadPic(token: String, callback: (Boolean, String) -> Unit) {
        val gson = Gson()
        val imageByteArray =
            MyApplication.getContext().resources.openRawResource(R.drawable.upload).readBytes()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
            "file", // 表单字段名
            "blank_image.jpg", // 文件名
            imageByteArray.toRequestBody("image/png".toMediaType()) // 设置 MIME 类型
        ).build()

        val request = Request.Builder().addHeader("token", token)
            .url("http://202.61.88.14:8093/jzdxapp/v2/upQdfile") // 上传地址
            .post(requestBody).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, "otherUploadPic-无地址")//回调
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        println("responseBody: $jsonObject")
                        val fileNane = jsonObject.get("data").toString()
                        println("fileNane: $fileNane")
                        //进行签到
                        signOther(token, fileNane, callback)
                    }
                } else {
                    callback(false, "otherUploadPic-无地址")//回调
                }
            }
        })
    }

    ///进行签到
    fun signOther(token: String, fileName: String, callback: (Boolean, String) -> Unit) {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()
        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()

        var longitude: String = ""
        var latitude: String = ""
        var address: String = ""
        if (isCurrentTimeInRange(2)) {
            val local = LocalDate.now()
            val isHoliday = holidayLists.contains("${local.month.value}-${local.dayOfMonth}")
            val dayOfWeek = local.dayOfWeek
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY || isHoliday) {
                longitude = "104.066444"
                latitude = "30.769059"
                address = "中国四川省成都市新都区仁爱路152号欣茂·大峰景"
            } else {
                longitude = "104.09778"
                latitude = "30.653439"
                address = "中国四川省成都市成华区一环路东三段2-8号玉双路(地铁站)"
            }
        } else {
            longitude = "104.066444"
            latitude = "30.769059"
            address = "中国四川省成都市新都区仁爱路152号欣茂·大峰景"
        }
        mapOf(
            "filename" to fileName,
            "dwdz" to address,
            "latitude" to latitude,
            "longitude" to longitude,
        ).forEach { (key, value) ->
            formBodyBuilder.add(key, value)
        }
        val formBody = formBodyBuilder.build()

        // 构建请求
        val requestBuilder = Request.Builder().addHeader("token", token)
            .url("http://202.61.88.14:8093/jzdxapp/v2/ryqd").post(formBody)
        val request = requestBuilder.build()
        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Request failed: ${e.message}")
                callback(false, address.substring(address.length - 6))//回调
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        println("responseBody: $jsonObject")
                        val msg = jsonObject.get("msg").toString()
                        println("msg: $msg")
                        callback(true, address.substring(address.length - 6))//回调
                        //保存
//                        saveOther(token)
                    }
                } else {
                    callback(false, address.substring(address.length - 6))//回调
                    println("Request failed with code: ${response.code}")
                }
            }
        })

    }

    ///进行saveÏ
    fun saveOther(token: String, callback: (Boolean) -> Unit) {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()
        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()
        val pararm = mapOf(
            "sjly" to "3",
            "address" to "中国四川省成都市新都区仁爱路152号欣茂·大峰景",
            "imei" to "b5cb2e2e1cd4c21a647f19d49522ed793b476c596ab156fe",
            "lon" to "f2039b97bd41785e69b5e5c4919af0de",
            "time" to System.currentTimeMillis().toString(),
            "lat" to "77b3090cdd3e307eba1283801827879f",
        )
        pararm.forEach { (key, value) ->
            formBodyBuilder.add(key, value)
        }
        val formBody = formBodyBuilder.build()

        // 构建请求
        val requestBuilder = Request.Builder().addHeader("token", token)
            .url("http://202.61.88.14:8093/jzdxdwdata/save").post(formBody)
        val request = requestBuilder.build()
        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Request failed: ${e.message}")
                callback(false)//回调
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        println("formBody: $pararm")
                        println("responseBody: $jsonObject")
                        val msg = jsonObject.get("msg").toString()
                        println("msg: $msg")
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })

    }

    ///md5加密
    fun md5Encryption(input: String): String {
        val digest = MessageDigest.getInstance("MD5") // 获取 MD5 加密实例
        val bytes = digest.digest(input.toByteArray()) // 获取输入字符串的字节数组，并进行加密

        // 将加密后的字节数组转化为十六进制字符串
        val stringBuilder = StringBuilder()
        for (byte in bytes) {
            stringBuilder.append(String.format("%02x", byte))
        }

        return stringBuilder.toString() // 返回 MD5 加密结果
    }


    private var signRemoveDuplicate: String = ""//排重

    //主动签到
    fun activeToSign(isHome: Boolean) {
        val currentMinute = LocalTime.now().minute.toString()

        if (signRemoveDuplicate.isNotEmpty()) {
            ToastUtils.showLong("已操作过了,请不要重复操作，如需再次操作请重启应用")
            return
        }
        signRemoveDuplicate = currentMinute

        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式
        val gson = Gson()

        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()

        val dateDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        var longitude: String = ""
        var latitude: String = ""
        var address: String = ""
        if (isHome) {
            longitude = "104.066444"
            latitude = "30.769059"
            address = "中国四川省成都市新都区仁爱路152号欣茂·大峰景"
        } else {
            longitude = "104.09778"
            latitude = "30.653439"
            address = "中国四川省成都市成华区一环路东三段2-8号玉双路(地铁站)"
        }
        val mapOf = mapOf(
            "signDate" to dateDay.format(Date()),
            "signTime" to dateTime.format(Date()),
            "longitude" to longitude,
            "latitude" to latitude,
            "address" to address,
            "remark" to ".",
            "addressType" to "0",
        )

        mapOf.forEach { (key, value) ->
            formBodyBuilder.add(key, value)
        }
        val formBody = formBodyBuilder.build()

        // 构建请求
        val requestBuilder =
            Request.Builder().url("http://1.14.111.130:9000/api/activeSign/add").post(formBody)
        // 添加 Headers
        requestBuilder.addHeader("Authorization", authorizationToken)
        val request = requestBuilder.build()
        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Request failed: ${e.message}")
                ToastUtils.showShort("失败了")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        // 解析 JSON
                        val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                        println("Response: $apiResponse")
                        ToastUtils.showShort("成功了")
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                    ToastUtils.showShort("失败了")
                }
            }
        })
    }

}

