package com.embeded.miniprinter.base

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.embeded.miniprinter.device.DeviceState
import com.inuker.bluetooth.library.BluetoothClient
import java.util.*

class BaseApplication: Application() {

    companion object{
        lateinit var context: Context
        lateinit var mClient:BluetoothClient
        var baseApp: BaseApplication? = null

        val CUSTOM_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CUSTOM_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        val CUSTOM_SERVICE_UUID_STM32 = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CUSTOM_CHARACTERISTIC_WRITE_UUID_STM32 = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val CUSTOM_CHARACTERISTIC_NOTIFY_UUID_STM32 = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        var nowConnectMac = ""

        val deviceState: MutableLiveData<DeviceState> = MutableLiveData(DeviceState())

    }

    override fun onCreate() {
        super.onCreate()
        baseApp = this
        context = applicationContext
        mClient = BluetoothClient(context)

    }
}