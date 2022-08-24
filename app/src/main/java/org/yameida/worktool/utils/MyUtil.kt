package org.yameida.worktool.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils

/**
 * 应用Util类
 */
class MyUtil {

    /**
     * 判断指定包名app是否运行(或者拉取当前运行的全部app)
     */
    fun isAppRunning(packageName: String = ""): Boolean {
        var isRun = false
        val localPackageManager: PackageManager = Utils.getApp().getPackageManager()
        val localList: List<*> = localPackageManager.getInstalledPackages(0)
        for (i in localList.indices) {
            val localPackageInfo = localList[i] as PackageInfo
            val packageStr = localPackageInfo.packageName.split(":".toRegex()).toTypedArray()[0]
            if (ApplicationInfo.FLAG_SYSTEM and localPackageInfo.applicationInfo.flags == 0
                && ApplicationInfo.FLAG_UPDATED_SYSTEM_APP and localPackageInfo.applicationInfo.flags == 0
                && ApplicationInfo.FLAG_STOPPED and localPackageInfo.applicationInfo.flags == 0
            ) {
//                LogUtils.e("TAG", packageStr)
                if (packageName == packageStr) {
                    isRun = true
                    break
                }
            }
        }
        return isRun
    }

}