package org.yameida.worktool.service.weixin

import android.annotation.SuppressLint
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
import java.io.IOException
import java.time.LocalTime


/**
 * 微信操作类
 */
@SuppressLint("NewApi")
object NetWorking {


    ///登录-上传图片-签到-save
    ///获取Other app  token
    fun getOtherToken() {
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
                        otherUploadPic(token)
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }


    ///上传图片
    @SuppressLint("ResourceType")
    fun otherUploadPic(token: String) {
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
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        println("responseBody: $jsonObject")
                        val fileNane = jsonObject.get("data").toString()
                        println("fileNane: $fileNane")
                        //进行签到
                        signOther(token, fileNane)
                    }
                }
            }
        })
    }

    ///进行签到
    fun signOther(token: String, fileName: String) {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()
        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()
        mapOf(
            "filename" to fileName,
            "dwdz" to "中国四川省成都市成华区猛追湾街道望平美食区一环路东3段-56号",
            "latitude" to "30.653438",
            "longitude" to "104.097993",
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
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        println("responseBody: $jsonObject")
                        val msg = jsonObject.get("msg").toString()
                        println("msg: $msg")
                        //保存
                        saveOther(token)
                    }
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })

    }

    ///进行saveÏ
    fun saveOther(token: String) {
        // 创建 OkHttpClient
        val client = OkHttpClient()
        // 构建请求体（JSON 格式）
        val gson = Gson()
        // 构建 FormBody
        val formBodyBuilder = FormBody.Builder()
        val pararm = mapOf(
            "sjly" to "3",
            "address" to "中国四川省成都市成华区猛追湾街道望平美食区一环路东3段-56号",
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

}

