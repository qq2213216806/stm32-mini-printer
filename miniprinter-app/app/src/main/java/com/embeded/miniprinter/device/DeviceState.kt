package com.embeded.miniprinter.device

data class DeviceState (
    var battery:Byte = 100,
    var temperature:Byte = 30,
    var paper_warn:Byte = 0,
    var work_status:Byte = 0,
    var connect_status:Boolean = false,
)