package org.yameida.worktool.activity

import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.*
import org.yameida.worktool.*
import org.yameida.worktool.service.WeworkService
import org.yameida.worktool.utils.UpdateUtil
import android.app.Dialog
import android.content.*
import android.widget.Button
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.yameida.worktool.databinding.ActivityListenBinding
import org.yameida.worktool.service.weixin.NetWorking
import org.yameida.worktool.service.weixin.WeiXinOperationImpl
import org.yameida.worktool.service.weixin.WeiXinOperationImpl.istTest
import org.yameida.worktool.utils.HostTestHelper
import org.yameida.worktool.utils.PermissionHelper
import org.yameida.worktool.utils.PermissionPageManagement


class ListenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        title = "WorkTool"
        // 初始化绑定类
        binding = ActivityListenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initAccessibility()
        initOverlays()
        UpdateUtil.checkUpdate()
        PermissionUtils.permission("android.permission.READ_EXTERNAL_STORAGE").request()
        registerReceiver(openWsReceiver, IntentFilter(Constant.WEWORK_NOTIFY))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(openWsReceiver)
    }

    override fun onResume() {
        super.onResume()
        binding.swOverlay.isChecked = PermissionUtils.isGrantedDrawOverlays()
        freshOpenServiceSwitch(
            WeworkService::class.java, binding.swAccessibility
        )
        if (needToWork) {
            needToWork = false
            goToWork()
        }
    }

    private fun initView() {
        binding.etChannel.setText(SPUtils.getInstance().getString(Constant.LISTEN_CHANNEL_ID))
        binding.btSave.setOnClickListener {
            val channel = binding.etChannel.text.toString().trim()
            SPUtils.getInstance().put(Constant.LISTEN_CHANNEL_ID, channel)
            ToastUtils.showLong("保存成功")
            sendBroadcast(Intent(Constant.WEWORK_NOTIFY).apply {
                putExtra("type", "modify_channel")
            })
        }
        binding.btTestUrl.setOnClickListener {
            istTest = !istTest
            ToastUtils.showLong("$istTest")
        }
//        binding.btSignHome.setOnClickListener {
//            NetWorking.activeToSign(true)
//        }
//        binding.btSignBusy.setOnClickListener {
//            NetWorking.activeToSign(false)
//        }
        binding.btSignList.setOnClickListener {
            WeiXinOperationImpl.postToSignList(true)
        }
        binding.swEncrypt.isChecked = Constant.encryptType == 1
        binding.swEncrypt.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            LogUtils.i("sw_encrypt onCheckedChanged: $isChecked")
            Constant.encryptType = if (isChecked) 1 else 0
            SPUtils.getInstance().put("encryptType", Constant.encryptType)
        })
        binding.swAutoReply.isChecked = Constant.autoReply == 1
        binding.swAutoReply.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            LogUtils.i("sw_auto_reply onCheckedChanged: $isChecked")
            Constant.autoReply = if (isChecked) 1 else 0
            SPUtils.getInstance().put("autoReply", Constant.autoReply)
        })
        binding.tvHost.text = Constant.host
        binding.tvHost.setOnLongClickListener {
            showInputDialog()
            true
        }
        val version =
            "${AppUtils.getAppVersionName()}     Android ${DeviceUtils.getSDKVersionName()} ${DeviceUtils.getManufacturer()} ${DeviceUtils.getModel()}"
        binding.tvVersion.text = version
        val workVersionName = AppUtils.getAppInfo(Constant.PACKAGE_NAMES)?.versionName
        when (workVersionName) {
            null -> {
                LogUtils.e("系统检测到您尚未安装企业微信，请先安装企业微信")
                binding.tvWorkVersion.text = "检测到您尚未安装企业微信，请先安装登录!"
            }

            in Constant.AVAILABLE_VERSION -> {
                LogUtils.i("当前企业微信版本已适配: $workVersionName")
                val tip = "$workVersionName   已适配，可放心使用~"
                binding.tvWorkVersion.text = tip
            }

            else -> {
                LogUtils.e("当前企业微信版本未兼容: $workVersionName")
                val tip = "$workVersionName   可能存在部分兼容性问题!"
                binding.tvWorkVersion.text = tip
            }
        }
        SPUtils.getInstance().put("appVersion", version)
        SPUtils.getInstance().put("workVersion", workVersionName)
    }

    private fun initAccessibility() {
        binding.swAccessibility.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            LogUtils.i("sw_accessibility onCheckedChanged: $isChecked")
            if (isChecked) {
                if (SPUtils.getInstance().getString(Constant.LISTEN_CHANNEL_ID).isNullOrBlank()) {
                    binding.swAccessibility.isChecked = false
                    ToastUtils.showLong("请先填写并保存链接号~")
                } else if (!PermissionHelper.isAccessibilitySettingOn()) {
                    openAccessibility()
                }
            } else {
                if (PermissionHelper.isAccessibilitySettingOn()) {
                    binding.swAccessibility.isChecked = true
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
            }
        })
    }

    private fun initOverlays() {
        binding.swOverlay.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            LogUtils.i("sw_overlay onCheckedChanged: $isChecked")
            if (isChecked) {
                if (!PermissionUtils.isGrantedDrawOverlays()) {
                    PermissionUtils.requestDrawOverlays(object : PermissionUtils.SimpleCallback {
                        override fun onGranted() {
                            ToastUtils.showLong("请同时打开后台弹出界面权限~")
                            PermissionPageManagement.goToSetting(this@ListenActivity)
                        }

                        override fun onDenied() {
                            binding.swAccessibility.isChecked = false
                        }
                    })
                }
            } else {
                if (PermissionUtils.isGrantedDrawOverlays()) {
                    binding.swOverlay.isChecked = true
                    PermissionPageManagement.goToSetting(this)
                }
            }
        })
    }

    /**
     * 打开辅助
     */
    private fun openAccessibility() {
        val clickListener = DialogInterface.OnClickListener { dialog, which ->
            freshOpenServiceSwitch(
                WeworkService::class.java, binding.swAccessibility
            )
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
        val cancel = DialogInterface.OnCancelListener {
            freshOpenServiceSwitch(
                WeworkService::class.java, binding.swAccessibility
            )
        }
        val cancelListener = DialogInterface.OnClickListener { dialog, which ->
            freshOpenServiceSwitch(
                WeworkService::class.java, binding.swAccessibility
            )
        }
        val dialog: AlertDialog =
            AlertDialog.Builder(this).setMessage(R.string.tips).setOnCancelListener(cancel)
                .setNegativeButton("取消", cancelListener).setPositiveButton("确定", clickListener)
                .create()
        dialog.show()
    }

    private fun freshOpenServiceSwitch(clazz: Class<*>, s: Switch) {
        if (PermissionHelper.isAccessibilitySettingOn()) {
            s.isChecked = true
            when (s.id) {
                R.id.sw_accessibility -> {
                }
            }
        } else {
            s.isChecked = false
            when (s.id) {
                R.id.sw_accessibility -> {
                }
            }
        }
    }

    private fun showInputDialog() {
        ToastUtils.showLong("请输入专线网络")
        val commentDialog = Dialog(this)
        commentDialog.setContentView(R.layout.dialog_input)
        val et: EditText = commentDialog.findViewById(R.id.body) as EditText
        et.setText(binding.tvHost.text)
        val okBtn: Button = commentDialog.findViewById(R.id.ok) as Button
        okBtn.setOnClickListener {
            val text = et.text.toString()
            if (text.isNotBlank()) {
                if (text.matches("ws{1,2}://[^/]+.*".toRegex())) {
                    Constant.host = text
                    binding.tvHost.text = text
                    HostTestHelper.test()
                    commentDialog.dismiss()
                } else {
                    ToastUtils.showLong("格式异常！")
                }
            } else {
                ToastUtils.showLong("请勿为空！")
            }
        }
        val cancelBtn: Button = commentDialog.findViewById(R.id.cancel) as Button
        cancelBtn.setOnClickListener {
            commentDialog.dismiss()
        }
        commentDialog.show()
    }

    private var needToWork = false

    private fun goToWork() {
        val positiveButton = MaterialAlertDialogBuilder(
            this, R.style.Theme_MaterialComponents_DayNight_Dialog
        ).setTitle("设置成功").setMessage("请勿人工操作手机     \n5秒后自动跳转")
            .setNegativeButton("", null).setPositiveButton("", null)
        val show = positiveButton.show()
        binding.btSave.postDelayed({ show.dismiss() }, 5000)
        binding.btSave.postDelayed({
            packageManager.getLaunchIntentForPackage(Constant.PACKAGE_NAMES)?.apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(this)
            }
            com.hjq.toast.ToastUtils.show("机器人运行中 请勿人工操作手机~")
        }, 5000)
    }

    private val openWsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("type") == "openWs") {
                needToWork = intent.getBooleanExtra("switch", false)
            }
        }
    }

}