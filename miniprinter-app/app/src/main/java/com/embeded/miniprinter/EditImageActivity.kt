package com.embeded.miniprinter

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.burhanrashid52.photoediting.ShapeBSFragment
import com.burhanrashid52.photoediting.StickerBSFragment
import com.burhanrashid52.photoediting.StickerBSFragment.StickerListener
import com.burhanrashid52.photoediting.TextEditorDialogFragment
import com.burhanrashid52.photoediting.filters.FilterListener
import com.burhanrashid52.photoediting.filters.FilterViewAdapter
import com.burhanrashid52.photoediting.tools.ToolType
import com.embeded.miniprinter.adapter.EditingToolsAdapter
import com.embeded.miniprinter.adapter.EditingToolsAdapter.OnItemSelected
import com.embeded.miniprinter.base.BaseActivity
import com.embeded.miniprinter.base.BaseApplication.Companion.deviceState
import com.embeded.miniprinter.device.DeviceState
import com.embeded.miniprinter.device.PrinterByte
import com.embeded.miniprinter.fragment.EmojiBSFragment
import com.embeded.miniprinter.fragment.PropertiesBSFragment
import com.embeded.miniprinter.tools.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vslimit.kotlindemo.ui.LoadingDialog
import ja.burhanrashid52.photoeditor.*
import ja.burhanrashid52.photoeditor.PhotoEditor.OnSaveListener
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import kotlinx.android.synthetic.main.activity_edit_image.*
import java.io.File
import java.io.IOException


class EditImageActivity : BaseActivity(), OnPhotoEditorListener, View.OnClickListener,
    PropertiesBSFragment.Properties, ShapeBSFragment.Properties, EmojiBSFragment.EmojiListener, StickerListener,
    OnItemSelected, FilterListener,DensitySetDialog.DensitySetTypeCallBack  {

    var mPhotoEditor: PhotoEditor? = null
    private var mPhotoEditorView: PhotoEditorView? = null
    private var mPropertiesBSFragment: PropertiesBSFragment? = null
    private var mShapeBSFragment: ShapeBSFragment? = null
    private var mShapeBuilder: ShapeBuilder? = null
    private var mEmojiBSFragment: EmojiBSFragment? = null
    private var mStickerBSFragment: StickerBSFragment? = null
    private var mTxtCurrentTool: TextView? = null
    private var mWonderFont: Typeface? = null
    private var mRvTools: RecyclerView? = null
    private var mRvFilters: RecyclerView? = null
    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private var mRootView: ConstraintLayout? = null
    private val mConstraintSet = ConstraintSet()
    private var mIsFilterVisible = false

    private var mDesitySetDialog:DensitySetDialog? = null

    private var bitmapCache:Bitmap? = null
    private var mSeekBar:SeekBar? = null

    private var mDeviceState= DeviceState()

    var imageUri: Uri? = null
    var loadingDialog: LoadingDialog? = null
    var isGreyImage = false
    private var needPrinterBitmap:Bitmap? = null

    @VisibleForTesting
    var mSaveImageUri: Uri? = null
    private var mSaveFileHelper: FileSaveHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_edit_image)
        initViews()
        handleIntentImage(mPhotoEditorView?.source)
        mWonderFont = Typeface.createFromAsset(assets, "beyond_wonderland.ttf")
        mPropertiesBSFragment = PropertiesBSFragment()
        mEmojiBSFragment = EmojiBSFragment()
        mStickerBSFragment = StickerBSFragment()
        mShapeBSFragment = ShapeBSFragment()
        mStickerBSFragment?.setStickerListener(this)
        mEmojiBSFragment?.setEmojiListener(this)
        mPropertiesBSFragment?.setPropertiesChangeListener(this)
        mShapeBSFragment?.setPropertiesChangeListener(this)
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools?.layoutManager = llmTools
        mRvTools?.adapter = mEditingToolsAdapter
        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvFilters?.layoutManager = llmFilters
        mRvFilters?.adapter = mFilterViewAdapter

        loadingDialog = LoadingDialog(this)

        // NOTE(lucianocheng): Used to set integration testing parameters to PhotoEditor
        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        //Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);
        //Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");
        mPhotoEditor = mPhotoEditorView?.run {
            PhotoEditor.Builder(this@EditImageActivity, this)
                .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build() // build photo editor sdk
        }
        mPhotoEditor?.setOnPhotoEditorListener(this)

        //Set Image Dynamically
        mPhotoEditorView?.source?.setImageResource(R.drawable.blank_image)
        bitmapCache = BitmapFactory.decodeResource(this.resources, R.drawable.blank_image)
        mSaveFileHelper = FileSaveHelper(this)

        deviceState.observe(this){ it ->
            if(it == null) return@observe
            Log.i(TAG,"Notify value $it")
            val paperWarn:String = if(it.paper_warn.toInt() == 0){
                "否"
            }else{
                "是"
            }
            val connectStatus:String = if(it.connect_status){
                "已连接"
            }else{
                "未连接"
            }
            var workStatus:String = "初始化中"
            if (it.work_status.toInt() == 0){
                workStatus = "初始化完成"
            }else if(it.work_status.toInt() == 1){
                workStatus = "开始打印"
                loadingDialog!!.hide()
            }else if(it.work_status.toInt() == 2){
                workStatus = "打印中"
            }else if(it.work_status.toInt() == 3){
                workStatus = "打印完成"
            }

            tv_device_status.text = "电量:${it.battery}% 温度:${it.temperature}°C 缺纸:${paperWarn} \r\n 工作状态:${workStatus} 连接状态:${connectStatus}";
            mDeviceState = it
            if(!it.connect_status){
                onBackPressed()
            }
        }
    }

    private fun handleIntentImage(source: ImageView?) {
        if (intent == null) {
            return;
        }

        when (intent.action) {
            Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT -> {
                try {
                    val uri = intent.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    source?.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            else -> {
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    val imageUri = intent.data
                    if (imageUri != null) {
                        source?.setImageURI(imageUri)
                    }
                }
            }
        }
    }

    private fun initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView)
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRvFilters = findViewById(R.id.rvFilterView)
        mRootView = findViewById(R.id.rootView)
        mTxtCurrentTool?.setOnClickListener(this)

        val imgUndo: ImageView = findViewById(R.id.imgUndo)
        imgUndo.setOnClickListener(this)
        val imgRedo: ImageView = findViewById(R.id.imgRedo)
        imgRedo.setOnClickListener(this)
        val imgCamera: ImageView = findViewById(R.id.imgCamera)
        imgCamera.setOnClickListener(this)
        val imgGallery: ImageView = findViewById(R.id.imgGallery)
        imgGallery.setOnClickListener(this)
        val imgSave: ImageView = findViewById(R.id.imgSave)
        imgSave.setOnClickListener(this)
        val imgClose: ImageView = findViewById(R.id.imgClose)
        imgClose.setOnClickListener(this)
        val imgShare: ImageView = findViewById(R.id.imgShare)
        imgShare.setOnClickListener(this)
        val imgDensity: ImageView = findViewById(R.id.imgDensity)
        imgDensity.setOnClickListener(this)

        mDesitySetDialog = DensitySetDialog()
        mDesitySetDialog!!.initDensitySetDialog(this)
        mDesitySetDialog!!.setDensitySetTypeCallBack(this)

        mSeekBar = findViewById(R.id.seekBar)
        mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                if(bitmapCache != null){
                    mPhotoEditor?.clearAllViews()
                    var newBitmap = p0?.let { ImageFilter.convertToBMW(bitmapCache,false, it.progress) }
                    mPhotoEditorView?.source?.setImageBitmap(newBitmap)
                    isGreyImage = false
                }
            }
        })
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment = TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener (object : TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String?, colorCode: Int) {
                val styleBuilder = TextStyleBuilder()
                styleBuilder.withTextColor(colorCode)
                if (rootView != null) {
                    mPhotoEditor?.editText(rootView, inputText, styleBuilder)
                }
                mTxtCurrentTool?.setText(R.string.label_text)
            }
        })
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(TAG, "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]")
    }

    override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(TAG, "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]")
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onTouchSourceImage(event: MotionEvent?) {
        Log.d(TAG, "onTouchView() called with: event = [$event]")
    }

    @SuppressLint("NonConstantResourceId", "MissingPermission")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> mPhotoEditor?.undo()
            R.id.imgRedo -> mPhotoEditor?.redo()
            R.id.imgSave -> saveImage()
            R.id.imgClose -> onBackPressed()
            R.id.imgShare -> {
                if(!mDeviceState.connect_status){
                    Toast.makeText(this,"已断开连接，请检查设备是否正常工作", Toast.LENGTH_SHORT).show()
                    return
                }else if(mDeviceState.paper_warn.toInt() == 1){
                    Toast.makeText(this,"设备缺纸 已停止打印!", Toast.LENGTH_SHORT).show()
                    return
                }else if(mDeviceState.temperature >= 65){
                    Toast.makeText(this,"打印头温度过高 已停止打印!", Toast.LENGTH_SHORT).show()
                    return
                }
                Toast.makeText(this,"准备打印中...", Toast.LENGTH_LONG).show()
                loadingDialog!!.show()
                mPhotoEditor?.saveAsBitmap(object :OnSaveBitmap{
                    override fun onBitmapReady(saveBitmap: Bitmap?) {

//                        var bitmapShow = BitmapUtils.newBitmap(saveBitmap)
//                        bitmapShow = ImageFilter.convertToBMW(bitmapShow,false,140)
//                        var bitmapPrinter = BitmapUtils.fitBitmap(saveBitmap,384)
//                        var newBitmap = ImageFilter.convertToDithering(bitmapPrinter)
//                        ImageFilter.convertToBMW(newBitmap,false,140)
//                        ImageFilter.convertToBMW(bitmapPrinter,true,140)

                        var bitmapShow = BitmapUtils.newBitmap(saveBitmap)
                        var bitmapPrinter:Bitmap
                        if(isGreyImage){
                            var newBitmap = ImageFilter.convertToDithering(ImageSuperHelper.resizeImage(needPrinterBitmap,384f))
                            bitmapPrinter = ImageSuperHelper.resizeImage(newBitmap,384f)
                            ImageSuperHelper.convertToDataRows(bitmapPrinter)
                        }else{
                            bitmapPrinter = BitmapUtils.fitBitmap(saveBitmap,384)
                            ImageFilter.convertToBMW(bitmapPrinter,true,140)
                        }

                        mPhotoEditor?.clearAllViews()
                        mPhotoEditorView?.source?.setImageBitmap(bitmapShow)
                    }
                    override fun onFailure(e: Exception?) {
                    }
                })
            }
            R.id.imgCamera -> takePhoto()
            R.id.imgGallery -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST)
            }
            R.id.imgDensity -> {
                mDesitySetDialog?.show()
            }
        }
    }

    private fun shareImage() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        val saveImageUri = mSaveImageUri
        if (saveImageUri == null) {
            showSnackbar(getString(R.string.msg_save_image_to_share))
            return
        }
        intent.putExtra(Intent.EXTRA_STREAM, buildFileProviderUri(saveImageUri))
        startActivity(Intent.createChooser(intent, getString(R.string.msg_share_image)))
    }

    private fun buildFileProviderUri(uri: Uri): Uri {
        if (FileSaveHelper.isSdkHigherThan28()) {
            return uri
        }
        val path: String = uri.path ?: throw IllegalArgumentException("URI Path Expected")

        return FileProvider.getUriForFile(
            this,
            FILE_PROVIDER_AUTHORITY,
            File(path)
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading("Saving...")
            mSaveFileHelper?.createFile(fileName, object : FileSaveHelper.OnFileCreateResult {

                @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
                override fun onFileCreateResult(
                    created: Boolean,
                    filePath: String?,
                    error: String?,
                    uri: Uri?
                ) {
                    if (created && filePath != null) {
                        val saveSettings = SaveSettings.Builder()
                            .setClearViewsEnabled(true)
                            .setTransparencyEnabled(true)
                            .build()

                        mPhotoEditor?.saveAsFile(
                            filePath,
                            saveSettings,
                            object : OnSaveListener {
                                override fun onSuccess(imagePath: String) {
                                    mSaveFileHelper?.notifyThatFileIsNowPubliclyAvailable(
                                        contentResolver
                                    )
                                    hideLoading()
                                    showSnackbar("Image Saved Successfully")
                                    mSaveImageUri = uri
                                    mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                                }

                                override fun onFailure(exception: Exception) {
                                    hideLoading()
                                    showSnackbar("Failed to save Image")
                                }
                            })
                    } else {
                        hideLoading()
                        error?.let { showSnackbar(error) }
                    }
                }
            })
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // TODO(lucianocheng): Replace onActivityResult with Result API from Google
    //                     See https://developer.android.com/training/basics/intents/result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    mPhotoEditor?.clearAllViews()
                    var bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri!!))
                    bitmapCache = bitmap
                    isGreyImage = true
                    needPrinterBitmap = ImageFilter.convertToDithering(ImageSuperHelper.resizeImage(bitmap,384f))
                    var newBitmap = ImageFilter.convertToDithering(bitmap)
                    mPhotoEditorView?.source?.setImageBitmap(newBitmap)
                    mSeekBar!!.setProgress(80,true)
                    mSeekBar?.visibility = View.VISIBLE
                }
                PICK_REQUEST -> try {
                    mPhotoEditor?.clearAllViews()
                    val uri = data?.data
                    var bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    bitmapCache = bitmap
                    isGreyImage = true
                    needPrinterBitmap = ImageFilter.convertToDithering(ImageSuperHelper.resizeImage(bitmap,384f))
                    var newBitmap = ImageFilter.convertToDithering(bitmap)
                    mPhotoEditorView?.source?.setImageBitmap(newBitmap)
                    mSeekBar!!.setProgress(80,true)
                    mSeekBar?.visibility = View.VISIBLE
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeColor(colorCode))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeOpacity(opacity))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeSize(shapeSize.toFloat()))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onShapePicked(shapeType: ShapeType?) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeType(shapeType))
    }

    override fun onEmojiClick(bitmap: Bitmap?) {
        mPhotoEditor?.addImage(bitmap)
        mTxtCurrentTool?.setText(R.string.label_emoji)
    }

    override fun onStickerClick(bitmap: Bitmap?) {
        mPhotoEditor?.addImage(bitmap)
        mTxtCurrentTool?.setText(R.string.label_sticker)
    }

    @SuppressLint("MissingPermission")
    override fun isPermissionGranted(isGranted: Boolean, permission: String?) {
        if (isGranted) {
            saveImage()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton("保存") { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton("取消") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton("丢弃") { _: DialogInterface?, _: Int -> finish() }
        builder.create().show()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter?) {
        mPhotoEditor?.setFilterEffect(photoFilter)
    }

    override fun onToolSelected(toolType: ToolType?) {
        mSeekBar?.visibility = View.INVISIBLE
        when (toolType) {
            ToolType.SHAPE -> {
                mPhotoEditor?.setBrushDrawingMode(true)
                mShapeBuilder = ShapeBuilder()
                mPhotoEditor?.setShape(mShapeBuilder)
                mTxtCurrentTool?.setText(R.string.label_shape)
                showBottomSheetDialogFragment(mShapeBSFragment)
            }
            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object : TextEditorDialogFragment.TextEditorListener {
                    override fun onDone(inputText: String?, colorCode: Int) {
                        val styleBuilder = TextStyleBuilder()
                        styleBuilder.withTextColor(colorCode)
                        styleBuilder.withTextSize(50f)
                        mPhotoEditor?.addText(inputText, styleBuilder)
                        mTxtCurrentTool?.setText(R.string.label_text)
                    }
                })
            }
            ToolType.ERASER -> {
                mPhotoEditor?.brushEraser()
                mTxtCurrentTool?.setText(R.string.label_eraser_mode)
            }
            ToolType.FILTER -> {
                mTxtCurrentTool?.setText(R.string.label_filter)
                showFilter(true)
            }
            ToolType.EMOJI -> showBottomSheetDialogFragment(mEmojiBSFragment)
            ToolType.STICKER -> showBottomSheetDialogFragment(mStickerBSFragment)
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(mRootView)
        val rvFilterId: Int = mRvFilters?.id ?: throw IllegalArgumentException("RV Filter ID Expected")
        if (isVisible) {
            mConstraintSet.clear(rvFilterId, ConstraintSet.START)
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            mConstraintSet.clear(rvFilterId, ConstraintSet.END)
        }
        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        mRootView?.let { TransitionManager.beginDelayedTransition(it, changeBounds) }
        mConstraintSet.applyTo(mRootView)
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                1
            )
        } else {
            val mImageName = "" + System.currentTimeMillis() + ".jpg"
            val outputImage = File(Environment.getExternalStorageDirectory().toString() + File.separator +mImageName)
            try  //判断图片是否存在，存在则删除在创建，不存在则直接创建
            {
                if (!outputImage.parentFile.exists()) {
                    outputImage.parentFile.mkdirs()
                }
                if (outputImage.exists()) {
                    outputImage.delete()
                }
                outputImage.createNewFile()
                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(this,
                        "com.embeded.miniprinter.fileprovider", outputImage);
                }else{
                    imageUri = Uri.fromFile(outputImage)
                }
                Log.i(TAG,"imageUri $imageUri")
                //使用隐示的Intent，系统会找到与它对应的活动，即调用摄像头，并把它存储
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, CAMERA_REQUEST)
                //调用会返回结果的开启方式，返回成功的话，则把它显示出来
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onBackPressed() {
        val isCacheEmpty = mPhotoEditor?.isCacheEmpty ?: throw IllegalArgumentException("isCacheEmpty Expected")

        if (mIsFilterVisible) {
            showFilter(false)
            mTxtCurrentTool?.setText(R.string.app_name)
        } else if (!isCacheEmpty) {
            showSaveDialog()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private val TAG = EditImageActivity::class.java.simpleName
        const val FILE_PROVIDER_AUTHORITY = "com.burhanrashid52.photoediting.fileprovider"
        private const val CAMERA_REQUEST = 52
        private const val PICK_REQUEST = 53
        const val ACTION_NEXTGEN_EDIT = "action_nextgen_edit"
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
    }

    override fun typeSelect(density: DensitySetDialog.DENSITY) {
        val byteArray: ByteArray = ByteArray(5)
        byteArray[0] = 0xA5.toByte()
        byteArray[1] = 0xA5.toByte()
        byteArray[2] = 0xA5.toByte()
        byteArray[3] = 0xA5.toByte()
        when (density) {
            DensitySetDialog.DENSITY.DENSITY_LOW -> {
                byteArray[4] = 0x01.toByte()
            }
            DensitySetDialog.DENSITY.DENSITY_MID -> {
                byteArray[4] = 0x02.toByte()
            }
            DensitySetDialog.DENSITY.DENSITY_HIGH -> {
                byteArray[4] = 0x03.toByte()
            }
        }
        PrinterByte.addSendFrames(byteArray)
    }
}