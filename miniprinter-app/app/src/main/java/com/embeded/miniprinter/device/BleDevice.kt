package com.embeded.miniprinter.device

class BleDevice(name:String,mac:String,rssi:Int){
    var name:String = name
    var mac:String = mac
    var rssi:Int = rssi
    var connectStatus:Boolean = false
}