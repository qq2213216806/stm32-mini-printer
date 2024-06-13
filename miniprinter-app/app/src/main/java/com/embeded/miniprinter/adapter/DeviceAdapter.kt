package com.embeded.miniprinter.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.embeded.miniprinter.device.BleDevice
import com.embeded.miniprinter.R
import kotlinx.android.synthetic.main.device_list_item.view.*

class DeviceAdapter (val context: Context, val deviceList: ArrayList<BleDevice>) : BaseAdapter() {
    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getItem(p0: Int): Any? {
        return null
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val li = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater;
        val itemView = convertView ?: li.inflate(R.layout.device_list_item,parent,false)
        val mac = deviceList[position].mac
        val rssi = deviceList[position].rssi
        val name = deviceList[position].name
        itemView.tv_name.text = name
        itemView.tv_mac.text = mac
        itemView.tv_rssi.text = rssi.toString()
        return itemView
    }

}