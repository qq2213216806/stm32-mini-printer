package com.embeded.miniprinter.tools

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.embeded.miniprinter.R

class DensitySetDialog{

    enum class DENSITY{
        DENSITY_LOW,DENSITY_MID,DENSITY_HIGH,
    }

    private var context:Context?=null

    private var dialog: Dialog? = null

    private var radioGroup:RadioGroup?=null

    private var radioButton1:RadioButton?=null
    private var radioButton2:RadioButton?=null
    private var radioButton3:RadioButton?=null

    fun initDensitySetDialog(activity: Activity){
        context = activity
        dialog = Dialog(context!!)
        dialog!!.setContentView(R.layout.dialog_density_set)
        //设置dialog以外的不能点击
        dialog!!.setCancelable(false)
        val window = dialog!!.window
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

        radioGroup = dialog!!.findViewById(R.id.radioGroup)
        radioButton1 = dialog!!.findViewById(R.id.radioButton)
        radioButton2 = dialog!!.findViewById(R.id.radioButton2)
        radioButton3 = dialog!!.findViewById(R.id.radioButton3)

        radioButton1?.setOnClickListener {
            dialog!!.dismiss()
            Toast.makeText(activity,"低密度",Toast.LENGTH_SHORT).show()
            typeSelect?.typeSelect(DENSITY.DENSITY_LOW)
        }
        radioButton2?.setOnClickListener {
            dialog!!.dismiss()
            Toast.makeText(activity,"中密度",Toast.LENGTH_SHORT).show()
            typeSelect?.typeSelect(DENSITY.DENSITY_MID)
        }
        radioButton3?.setOnClickListener {
            dialog!!.dismiss()
            Toast.makeText(activity,"高密度",Toast.LENGTH_SHORT).show()
            typeSelect?.typeSelect(DENSITY.DENSITY_HIGH)
        }
    }

    interface DensitySetTypeCallBack{
        fun typeSelect(density:DENSITY)
    }

    var typeSelect : DensitySetTypeCallBack? = null

    fun setDensitySetTypeCallBack(densitySetTypeCallBack: DensitySetTypeCallBack){
        this.typeSelect = densitySetTypeCallBack
    }

    fun show(){
        dialog?.show()
    }

    fun hide(){
        dialog?.hide()
    }

    fun dismiss(){
        dialog?.dismiss()
    }

}