package com.embeded.miniprinter.device

import android.util.Log
import com.embeded.miniprinter.base.BaseApplication
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_CHARACTERISTIC_UUID
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_CHARACTERISTIC_WRITE_UUID_STM32
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_SERVICE_UUID
import com.embeded.miniprinter.base.BaseApplication.Companion.CUSTOM_SERVICE_UUID_STM32
import com.embeded.miniprinter.base.BaseApplication.Companion.nowConnectMac
import com.embeded.miniprinter.tools.ByteArraryUtils.byteArr2HexString
import com.inuker.bluetooth.library.Code
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*

object PrinterByte {

    private val TAG = "PrinterByte"

    private const val maxFrameBufferCount = 1000

    //384point 48byte
    private const val maxLineByte = 48

    private val mFrameLock: Object = Object()

    private var lock: Any = Object()

    //用于把byte数据转为对应bit
    private var bitIndex:Int = 0
    private var byte:Byte = 0
    private var arrayIndex:Int = 0
    private val byteArray: ByteArray = ByteArray(maxLineByte)
    val finishByteArray: ByteArray = ByteArray(5)

    var deviceType:Int = 0


    private var mSendFrames: LinkedList<ByteArray> = LinkedList<ByteArray>()

    private fun setBit(byte: Byte, status: Int): Byte {
        return (byte.toInt().shl(1) or status).toByte()
    }

    fun addByteArr(status:Boolean,isPoint: Boolean) {
        if(status){
            addSendFrames(byteArray)
//            Log.i(TAG,"add ${mSendFrames.size}  "+ byteArray.size+" "+byteArr2HexString(byteArray, ','))
            arrayIndex = 0
            return
        }
        byte = if (!isPoint){
            setBit(byte,0)
        }else{
            setBit(byte,1)
        }
        bitIndex ++;
        if(bitIndex >= 8){
            byteArray[arrayIndex] = byte
            arrayIndex ++
            if(arrayIndex >= maxLineByte)
                arrayIndex = maxLineByte
            bitIndex = 0
            byte = 0
        }
    }

    fun sendStartPrinter() {
        finishByteArray[0] = 0xA6.toByte()
        finishByteArray[1] = 0xA6.toByte()
        finishByteArray[2] = 0xA6.toByte()
        finishByteArray[3] = 0xA6.toByte()
        finishByteArray[4] = 0x01.toByte()
        addSendFrames(finishByteArray)
        Log.i(TAG,"----send finish ---------------------")
    }

    private fun notifyGetFrame() {
        synchronized(mFrameLock) {
            //唤醒线程
            mFrameLock.notify()
        }
    }

    private fun waitGetFrame(){
        mSendFrames.let {
            synchronized(it){
                if(it.size > 0)
                    return
            }
        }
        synchronized(mFrameLock){
            try {
                //线程 等待/阻塞
                mFrameLock.wait()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun addSendFrames(byteArray: ByteArray) {
        mSendFrames.let {
            synchronized(it) {
                if (it.size > maxFrameBufferCount)
                    return
                var newArray = byteArray.clone()
                it.add(newArray)
                notifyGetFrame()
            }
        }
    }

    private fun getOneFrame(): ByteArray? {
        var frame: ByteArray? = null
        mSendFrames.let {
            synchronized(it){
                if (it.size > 0) {
                    frame = it.poll()
                }
            }
        }
        return frame
    }

    fun run(): Flow<Boolean> = flow {
        while (true) {
            waitGetFrame()
            synchronized(lock) {
                getOneFrame()?.let {
                    Log.i(TAG,"Send ${mSendFrames.size}  "+ it.size+" "+byteArr2HexString(it, ','))
                    if(deviceType == 0){
                        BaseApplication.mClient.writeNoRsp(
                            nowConnectMac, CUSTOM_SERVICE_UUID, CUSTOM_CHARACTERISTIC_UUID, it
                        ) { code ->
                            if (code == Code.REQUEST_SUCCESS) {
                            }
                        }
                    }else{
                        BaseApplication.mClient.writeNoRsp(
                            nowConnectMac, CUSTOM_SERVICE_UUID_STM32, CUSTOM_CHARACTERISTIC_WRITE_UUID_STM32, it
                        ) { code ->
                            if (code == Code.REQUEST_SUCCESS) {
                            }
                        }
                    }
                }
            }
            emit(true)
            //send interval > 100ms
            delay(20)
        }
    }
}