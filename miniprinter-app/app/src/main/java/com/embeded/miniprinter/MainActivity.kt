package com.embeded.miniprinter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.embeded.miniprinter.adapter.DeviceAdapter
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_CHARACTERISTIC_NOTIFY_UUID_STM32
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_CHARACTERISTIC_UUID
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_SERVICE_UUID
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_SERVICE_UUID_STM32
import com.embeded.miniprinter.base.BaseApplication.Companion.deviceState
import com.embeded.miniprinter.base.BaseApplication.Companion.mClient
import com.embeded.miniprinter.base.BaseApplication.Companion.nowConnectMac
import com.embeded.miniprinter.device.BleDevice
import com.embeded.miniprinter.device.DeviceState
import com.embeded.miniprinter.device.PrinterByte
import com.embeded.miniprinter.tools.ByteArraryUtils.byteArr2HexString
import com.inuker.bluetooth.library.Code.REQUEST_SUCCESS
import com.inuker.bluetooth.library.Constants.STATUS_CONNECTED
import com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED
import com.inuker.bluetooth.library.beacon.Beacon
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.connect.response.BleMtuResponse
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.search.SearchRequest
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.inuker.bluetooth.library.utils.BluetoothLog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    private val deviceList = ArrayList<BleDevice>()

    private var listViewAdapter = DeviceAdapter(this, deviceList)

    private val deviceIOScope = CoroutineScope(Dispatchers.IO)

    private val PERMISSION_REQUEST_COARSE_LOCATION = 1

    private val CUSTOM_BLE_MTU = 250

    private var mDeviceState= DeviceState()

    private var deviceType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //权限获取
        checkPermission()

        Log.i(TAG,"Device Version: " + android.os.Build.MODEL + ","
                + android.os.Build.VERSION.SDK_INT + ","
                + android.os.Build.VERSION.RELEASE);

        //注册蓝牙开关状态回调
        mClient.registerBluetoothStateListener(mBluetoothStateListener)
        if (!mClient.isBluetoothOpened) {
            mClient.openBluetooth();
        }

        //按键触发打开蓝牙
        btn_scan.setOnClickListener {
            deviceList.clear()
            if(nowConnectMac != ""){
                mClient.disconnect(nowConnectMac);
            }
            scanBle()
        }

        lv_device.adapter = listViewAdapter
        lv_device.setOnItemClickListener { adapterView, view, position, l ->
            Log.i(TAG, "MAC = "+deviceList[position].mac)
            if(deviceList[position].name != "Mini-Printer")
                Toast.makeText(this@MainActivity,"请选择名称为Mini-Printer的设备",Toast.LENGTH_SHORT).show()
            else{
                if(nowConnectMac != ""){
                    if(deviceList[position].mac == nowConnectMac){
                        //已连接，直接跳转
                        val intent= Intent();
                        intent.setClass(this@MainActivity,EditImageActivity::class.java)
                        intent.putExtra("EXTRAS_DEVICE_ADDRESS", nowConnectMac)
                        startActivity(intent)
                    }else{
                        mClient.disconnect(nowConnectMac);
                    }
                }
                connectBle(deviceList[position].mac)
            }
        }

        PrinterByte.run().onEach { dataState ->
            Log.i(TAG, "Send pack = $dataState")
        }.launchIn(deviceIOScope)
        
        spinner_device_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            AdapterView.OnItemClickListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                deviceType = position
                Log.i(TAG, "deviceType = $deviceType")
                PrinterByte.deviceType = deviceType
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                TODO("Not yet implemented")
            }
        }

    }

    val mPermissionList: MutableList<String> = mutableListOf()

    fun checkPermission(): Boolean? {
        var isGranted = true
//        if (Build.VERSION.SDK_INT >= 23) {
//            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                isGranted = false
//            }
//            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                isGranted = false
//            }
//            Log.i("读写权限获取", " ： $isGranted")
//            if (!isGranted) {
//                requestPermissions(
//                    arrayOf(
//                        Manifest.permission.ACCESS_COARSE_LOCATION,
//                        Manifest.permission.ACCESS_FINE_LOCATION,
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.CAMERA
//                    ),
//                    102
//                )
//            }
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 版本大于等于 Android 12 时
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN)
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT)

            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            mPermissionList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            Handler().postDelayed({
                Toast.makeText(this@MainActivity,"> Android 12 设备", Toast.LENGTH_SHORT).show()
            }, 3000) // 延时2000毫秒，即2秒
        } else {
            // Android 版本小于 Android 12 及以下版本
            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            Handler().postDelayed({
                Toast.makeText(this@MainActivity,"< Android 12 设备", Toast.LENGTH_SHORT).show()
            }, 3000) // 延时2000毫秒，即2秒
        }

        if (mPermissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, mPermissionList.toTypedArray(), 1001)
        }
        return isGranted
    }

    /**
     * 权限获取结果回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out kotlin.String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "BLE PERMISSION GET SUCCESS")
            }
        }

        var hasPermissionDismiss = false
        if (1001 == requestCode) {
            for (i in grantResults.indices) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true
                    break
                }
            }
        }

        if (hasPermissionDismiss) {
            Toast.makeText(this@MainActivity,"有权限未通过的处理", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity,"权限全部已经通过", Toast.LENGTH_SHORT).show()
        }
    }

    fun isLocationEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        return (networkProvider || gpsProvider)
    }
    private val mBluetoothStateListener: BluetoothStateListener =
        object : BluetoothStateListener() {
            override fun onBluetoothStateChanged(openOrClosed: Boolean) {
                Log.i(TAG, "BLE State $openOrClosed")
            }
        }

    private val mBleConnectStatusListener: BleConnectStatusListener =
        object : BleConnectStatusListener() {
            override fun onConnectStatusChanged(mac: String, status: Int) {
                if (status == STATUS_CONNECTED) {
                    Log.i(TAG,"MAC $mac STATUS_CONNECTED")
                    nowConnectMac = mac
                    Toast.makeText(this@MainActivity,"连接成功，设置MTU为$CUSTOM_BLE_MTU",Toast.LENGTH_SHORT).show()
                    mClient.requestMtu(nowConnectMac,CUSTOM_BLE_MTU, BleMtuResponse { code, data ->
                        Log.i(TAG, "BleMtuResponse $code $data")
                    })

                    if(nowConnectMac != ""){
                        if(deviceType == 0){
                            openNotify(nowConnectMac, CUSTOM_SERVICE_UUID, CUSTOM_CHARACTERISTIC_UUID)
                        }else{
                            openNotify(nowConnectMac, CUSTOM_SERVICE_UUID_STM32, CUSTOM_CHARACTERISTIC_NOTIFY_UUID_STM32)
                        }
                    }
                    mDeviceState.connect_status = true
                    deviceState.postValue(mDeviceState)
                    val intent= Intent();
                    intent.setClass(this@MainActivity,EditImageActivity::class.java)
                    intent.putExtra("EXTRAS_DEVICE_ADDRESS", nowConnectMac)
                    startActivity(intent)
                } else if (status == STATUS_DISCONNECTED) {
                    Log.i(TAG,"MAC $mac STATUS_DISCONNECTED")
                    nowConnectMac = ""
                    Toast.makeText(this@MainActivity,"$mac 连接已断开",Toast.LENGTH_SHORT).show()
                    mDeviceState.connect_status = false
                    deviceState.postValue(mDeviceState)
                }
            }
        }

    /**
     * 扫描
     */
    private fun scanBle() {
        val request = SearchRequest.Builder()
            .searchBluetoothLeDevice(3000, 1) // 先扫BLE设备3次，每次3s
//            .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s
//            .searchBluetoothLeDevice(2000) // 再扫BLE设备2s
            .build()

        mClient.search(request, object : SearchResponse {
            override fun onSearchStarted() {}
            override fun onDeviceFounded(device: SearchResult) {
                val beacon = Beacon(device.scanRecord)
                Log.i(TAG, "device name ${device.name} ${device.address}")
                if (device.name != "NULL") {
                    if (deviceList.find { it.name == device.name } == null && deviceList.find { it.mac == device.address } == null) {
                        val bleDevice = BleDevice(device.name, device.address, device.rssi)
                        deviceList.add(bleDevice)
                        listViewAdapter.notifyDataSetChanged()
                    }
                }
            }
            override fun onSearchStopped() {}
            override fun onSearchCanceled() {}
        })
    }

    private fun connectBle(MAC: String) {
        //注册蓝牙连接状态回调
        mClient.registerConnectStatusListener(MAC, mBleConnectStatusListener);
        val options: BleConnectOptions = BleConnectOptions.Builder()
            .setConnectRetry(3)  // 连接如果失败重试3次
            .setConnectTimeout(30000)  // 连接超时30s
            .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
            .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
            .build();
        mClient.connect(MAC, options, BleConnectResponse { code, profile ->
            BluetoothLog.v(String.format("profile:\n%s", profile));
        });
    }

    private fun readRssi(MAC: String) {
        mClient.readRssi(
            MAC
        ) { code, rssi ->
            if (code == REQUEST_SUCCESS) {
            }
        }
    }

    private fun readCharacteristic(MAC: String, serviceUUID: UUID, characterUUID: UUID) {
        mClient.read(
            MAC, serviceUUID, characterUUID
        ) { code, data ->
            if (code == REQUEST_SUCCESS) {
                Log.i(TAG,"$data")
            }
        }
    }

    /**
     * 要注意这里写的byte[]不能超过20字节，如果超过了需要自己分成几次写。建议的办法是第一个byte放剩余要写的字节的长度。
     */
    private fun writeCharacteristic(
        MAC: String,
        serviceUUID: UUID,
        characterUUID: UUID,
        bytes: ByteArray
    ) {
        mClient.write(
            MAC, serviceUUID, characterUUID, bytes
        ) { code ->
            if (code == REQUEST_SUCCESS) {
            }
        }
    }

    /**
     * 这个写是带了WRITE_TYPE_NO_RESPONSE标志的，实践中发现比普通的write快2~3倍，建议用于固件升级。
     */
    private fun writeCharacteristicFast(
        MAC: String,
        serviceUUID: UUID,
        characterUUID: UUID,
        bytes: ByteArray
    ) {
        mClient.writeNoRsp(
            MAC, serviceUUID, characterUUID, bytes
        ) { code ->
            if (code == REQUEST_SUCCESS) {
            }
        }
    }

    private fun readDescriptor(
        MAC: String,
        serviceUUID: UUID,
        characterUUID: UUID,
        descriptorUUID: UUID
    ) {
        mClient.readDescriptor(
            MAC, serviceUUID, characterUUID, descriptorUUID
        ) { code, data -> }
    }

    private fun writeDescriptor(
        MAC: String,
        serviceUUID: UUID,
        characterUUID: UUID,
        descriptorUUID: UUID,
        bytes: ByteArray
    ) {
        mClient.writeDescriptor(
            MAC, serviceUUID, characterUUID, descriptorUUID, bytes
        ) { }
    }

    /**
     * onNotify是接收通知的。
     */
    private fun openNotify(MAC: String, serviceUUID: UUID, characterUUID: UUID) {
        mClient.notify(MAC, serviceUUID, characterUUID, object : BleNotifyResponse {
            override fun onNotify(service: UUID?, character: UUID?, value: ByteArray?) {
                Log.i(TAG,"Notify value "+byteArr2HexString(value, ','))
                if(value?.size!! >= 4){
                    mDeviceState.battery = value[0]
                    mDeviceState.temperature = value[1]
                    mDeviceState.paper_warn = value[2]
                    mDeviceState.work_status = value[3]
                    deviceState.postValue(mDeviceState)
                }
            }
            override fun onResponse(code: Int) {
                if (code == REQUEST_SUCCESS) {
                    Log.i(TAG,"REQUEST_SUCCESS")
                }
            }
        })
    }

    private fun closeNotify(MAC: String, serviceUUID: UUID, characterUUID: UUID) {
        mClient.unnotify(
            MAC, serviceUUID, characterUUID
        ) { code ->
            if (code == REQUEST_SUCCESS) {
            }
        }
    }


}