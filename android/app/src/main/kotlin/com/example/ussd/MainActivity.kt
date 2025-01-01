package com.example.ussd

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.StringCodec
import java.util.concurrent.CompletableFuture

class MainActivity: FlutterActivity() {

    private lateinit var channel : MethodChannel
    private lateinit var basicMessageChannel: BasicMessageChannel<String>
    private val ussdApi: USSDApi = USSDController
    private var event: AccessibilityEvent? = null
    private var context: Context? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        /*
        val channelName = "method.com.example/ussd_advanced"
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger,channelName).setMethodCallHandler{ call, result ->
            when (call.method) {
                "normal_ussd" -> {
                    result.success("normal_ussd_result")
                }
                "single_ussd" -> {
                    result.success("single_ussd_result")
                }
                "multiple_ussd" -> {
                    result.success("multiple_ussd_result")
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
        */
        this.context = this.applicationContext
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "method.com.example.ussd/ussd_advance")
        channel.setMethodCallHandler { call, result -> onMethodCall(call, result) }
        basicMessageChannel  = BasicMessageChannel(flutterEngine.dartExecutor.binaryMessenger, "message.com.example.ussd/ussd_advance", StringCodec.INSTANCE)
        basicMessageChannel.setMessageHandler { message, reply -> onMessage(message, reply) }
    }

    private fun onMessage(message: String?, reply: BasicMessageChannel.Reply<String?>) {
        Log.e("TAG", "onMessage: $message")
        if(message != null){
            if (event!=null){
                Log.e("TAG", "onMessage: inside event if", )

                USSDController.send2(message, event!!){
                    Log.e("TAG", "onMessage: inside USSDController.send2", )
                    event = AccessibilityEvent.obtain(it)
                    try {
                        Log.e("TAG", "onMessage: it.text = "+it.text )
                        if(it.text.isNotEmpty()) {
                            reply.reply(it.text.first().toString())
                        }else{
                            reply.reply(null)
                        }
                    } catch (e: Exception){
                        Log.e("TAG", "onMessage: 1exception "+e.message )
                        Log.e("TAG", "onMessage: 2exception "+e )
                    }

                }
            }
        }
    }

    private fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        var subscriptionId:Int = 1
        var code:String? = ""

        if(call.method == "sendUssd" ||call.method == "sendAdvancedUssd" ||call.method == "multisessionUssd"){
            val subscriptionIdInteger = call.argument<Int>("subscriptionId")
                ?: throw RequestParamsException(
                    "Incorrect parameter type: `subscriptionId` must be an int"
                )
            subscriptionId = subscriptionIdInteger
            if (subscriptionId < -1 ) {
                throw RequestParamsException(
                    "Incorrect parameter value: `subscriptionId` must be >= -1"
                )
            }
            code = call.argument<String>("code")
            if (code == null) {
                throw RequestParamsException("Incorrect parameter type: `code` must be a String")
            }
            if (code.isEmpty()) {
                throw RequestParamsException(
                    "Incorrect parameter value: `code` must not be an empty string"
                )
            }
        }

        when (call.method) {
            "hasPermissions" -> {
                result.success(hasPermissions())

            }
            "requestPermissions" -> {
                requestPermissions()
                result.success(null)

            }
            "sendUssd" -> {
                result.success(defaultUssdService(code!!, subscriptionId))

            }
            "sendAdvancedUssd" -> {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    val res = singleSessionUssd(code!!, subscriptionId)
                    if(res != null){

                        res.exceptionally { e: Throwable? ->
                            if (e is RequestExecutionException) {
                                result.error(
                                    RequestExecutionException.type, e.message, null
                                )
                            } else {
                                result.error(RequestExecutionException.type, e?.message, null)
                            }
                            null
                        }.thenAccept(result::success);

                    }else{
                        result.success(res);
                    }
                }else{
                    result.success(defaultUssdService(code!!, subscriptionId))
                }
            }
            "multisessionUssd" -> {
                // check permissions
                if(
                    !hasPermissions()
                ){
                    requestPermissions()
                    result.success(null)


                }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    multisessionUssd(code!!, subscriptionId, result)

                }else{
                    result.success(defaultUssdService(code!!, subscriptionId))
                }

            }
            "multisessionUssdCancel" ->{
                multisessionUssdCancel()
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun hasPermissions() : Boolean{
        return !(
                ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                        !isAccessibilityServiceEnabled()
                )
    }

    private fun requestPermissions(){
        if(!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this.context!!.startActivity(intent)

        }

        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 2)
            }
        }

        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_PHONE_STATE)) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 2)
            }
        }

    }

    private class RequestExecutionException internal constructor(override var message: String) :
        Exception() {
        companion object {
            var type = "ussd_plugin_ussd_execution_failure"
        }
    }

    private class RequestParamsException internal constructor(override var message: String) :
        Exception() {
        companion object {
            var type = "ussd_plugin_incorrect__parameters"
        }
    }


    // for android 8+
    private fun singleSessionUssd(ussdCode:String, subscriptionId:Int) : CompletableFuture<String>?{
        // use defaulft sim
        val _useDefault: Boolean = subscriptionId == -1

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            var res: CompletableFuture<String> = CompletableFuture<String>()
            // check permissions
            if (ContextCompat.checkSelfPermission(this.context!!, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)) {
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 2)
                }
            }

            // get TelephonyManager
            val tm = this.context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val simManager: TelephonyManager = tm.createForSubscriptionId(subscriptionId)

            // callback
            val callback =
                object : UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager, request: String, response: CharSequence
                    ) {
                        res.complete(response.toString())
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager, request: String, failureCode: Int
                    ) {
                        when (failureCode) {
                            TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> {
                                res.completeExceptionally(RequestExecutionException("USSD_ERROR_SERVICE_UNAVAIL"))
                            }
                            TelephonyManager.USSD_RETURN_FAILURE -> {
                                res.completeExceptionally(RequestExecutionException("USSD_RETURN_FAILURE"))
                            }
                            else -> {
                                res.completeExceptionally(RequestExecutionException("unknown error"))
                            }
                        }
                    }
                }

            if(_useDefault){
                tm.sendUssdRequest(
                    ussdCode,
                    callback,
                    Handler(Looper.getMainLooper())
                )

            }else{
                simManager.sendUssdRequest(
                    ussdCode,
                    callback,
                    Handler(Looper.getMainLooper())
                )
            }


            return res
        }else{
            // if sdk is less than 26
            defaultUssdService(ussdCode, subscriptionId)
            return  null
        }

    }

    private fun multisessionUssd(ussdCode:String, subscriptionId:Int, @NonNull result: Result){
        Log.e("TAG", "multisessionUssd: ussdCode = $ussdCode")
        var slot = subscriptionId
        if(subscriptionId == -1){
            slot = 0
        }

        ussdApi.callUSSDInvoke(this, ussdCode, slot, object : USSDController.CallbackInvoke {

            override fun responseInvoke(ev: AccessibilityEvent) {
                event = AccessibilityEvent.obtain(ev)

                try {
                    if(ev.text.isNotEmpty()) {
                        result.success(java.lang.String.join("\n", ev.text))
//            result.success(ev.text.first().toString())
                    }else{
                        result.success(null)
                    }
                }catch (e: Exception){
                    Log.e("TAG", "responseInvoke: exception = ${e.message}")
                }
            }

            override fun over(message: String) {
                try {
                    basicMessageChannel.send(message)
                    result.success(message)
                    basicMessageChannel.setMessageHandler(null)
                }catch (e: Exception){}

            }
        })
    }

    private fun multisessionUssdCancel(){
        if(event != null){
            ussdApi.cancel2(event!!);
            basicMessageChannel.setMessageHandler(null)
        }
    }

    private val simSlotName = arrayOf(
        "extra_asus_dial_use_dualsim",
        "com.android.phone.extra.slot",
        "slot",
        "simslot",
        "sim_slot",
        "subscription",
        "Subscription",
        "phone",
        "com.android.phone.DialingMode",
        "simSlot",
        "slot_id",
        "simId",
        "simnum",
        "phone_type",
        "slotId",
        "slotIdx"
    )

    // multiple for all
    private fun defaultUssdService(ussdCode:String, subscriptionId:Int){
        if (ContextCompat.checkSelfPermission(this.context!!, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)) {
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 2)
            }
        }
        try {
            // use defaulft sim
            val _useDefault: Boolean = subscriptionId == -1

            val sim:Int = subscriptionId -1
            var number:String = ussdCode;
            number = number.replace("#", "%23");
            if (!number.startsWith("tel:")) {
                number = String.format("tel:%s", number);
            }
            val intent =
                Intent(if (isTelephonyEnabled()) Intent.ACTION_CALL else Intent.ACTION_VIEW)
            intent.data = Uri.parse(number)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


            if(!_useDefault){
                intent.putExtra("com.android.phone.force.slot", true);
                intent.putExtra("Cdma_Supp", true);

                for (s in simSlotName) intent.putExtra(s, sim)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (ContextCompat.checkSelfPermission(this.context!!, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_PHONE_STATE)) {
                        } else {
                            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 2)
                        }
                    }
                    val telecomManager = this.context!!.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

                    val phoneAccountHandleList = telecomManager.callCapablePhoneAccounts
                    if (phoneAccountHandleList != null && phoneAccountHandleList.isNotEmpty())
                        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE",
                            phoneAccountHandleList[sim]
                        );
                }
            }


            this.context!!.startActivity(intent)

        } catch (e: Exception) {
            throw e
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean{
        var accessibilityEnabled = false
        val service = "USSDService"
        val am = context!!.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)
        for (enabledService in enabledServices) {
            val id = enabledService.id
            if (id.contains(service)) {
                accessibilityEnabled = true
                break
            }
        }
        return accessibilityEnabled

    }

    private fun isTelephonyEnabled(): Boolean {
        val tm = this.context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.phoneType != TelephonyManager.PHONE_TYPE_NONE

    }
}
