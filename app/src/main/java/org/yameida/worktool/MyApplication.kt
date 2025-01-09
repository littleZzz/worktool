package org.yameida.worktool

import android.app.Application
import android.content.Intent
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.Utils
import com.google.gson.Gson
import com.hjq.toast.ToastUtils
import com.tendcloud.tenddata.TalkingDataSDK
import org.yameida.worktool.config.GlobalException

class MyApplication : Application() {

    companion object {

        /**
         * 回到WorkTool首页 需要先授权显示悬浮窗
         */
        fun launchIntent() {
            LogUtils.e("进入WorkTool APP~")
            val app = Utils.getApp()
            app.packageManager.getLaunchIntentForPackage("")?.apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                app.startActivity(this)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        //初始化工具类
        Utils.init(this)
        GsonUtils.setGsonDelegate(Gson())
        //初始化 Toast 框架
        ToastUtils.init(this)
        //初始化友盟统计
        val key = "6284a3a3d024421570f97c3c"
        val channel = "main_channel"
        TalkingDataSDK.init(
            this,
            "80E9C84E39904DAFB28562910FF7C86C",
            "worktool_master",
            SPUtils.getInstance().getString(Constant.LISTEN_CHANNEL_ID)
        )
        //设置全局异常捕获重启
        Thread.setDefaultUncaughtExceptionHandler(GlobalException.getInstance())
    }

}