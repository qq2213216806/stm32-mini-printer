package com.embeded.miniprinter.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.embeded.miniprinter.R
import java.io.IOException

class EmojiBSFragment : BottomSheetDialogFragment() {
    private var mEmojiListener: EmojiListener? = null

    interface EmojiListener {
        fun onEmojiClick(bitmap: Bitmap?)
    }

    private val mBottomSheetBehaviorCallback: BottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.fragment_bottom_sticker_emoji_dialog, null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        (contentView.parent as View).setBackgroundColor(resources.getColor(android.R.color.transparent))
        val rvEmoji: RecyclerView = contentView.findViewById(R.id.rvEmoji)
        val gridLayoutManager = GridLayoutManager(activity, 5)
        rvEmoji.layoutManager = gridLayoutManager
        val emojiAdapter = EmojiAdapter()
        rvEmoji.adapter = emojiAdapter
        rvEmoji.setHasFixedSize(true)
        rvEmoji.setItemViewCacheSize(emojisList.size)
    }

    fun setEmojiListener(emojiListener: EmojiListener?) {
        mEmojiListener = emojiListener
    }

    inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.row_sticker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val fromAsset = getBitmapFromAsset(holder.itemView.context, emojisList[position])
            holder.imgEmoji.setImageBitmap(fromAsset)
        }

        override fun getItemCount(): Int {
            return emojisList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgEmoji: ImageView = itemView.findViewById(R.id.imgSticker)
            init {
                itemView.setOnClickListener {
                    if (mEmojiListener != null) {
                        val fromAsset = getBitmapFromAsset(itemView.context, emojisList[layoutPosition])
                        mEmojiListener!!.onEmojiClick(fromAsset)
                    }
                    dismiss()
                }
            }
        }

        private fun getBitmapFromAsset(context: Context, strName: String): Bitmap? {
            val assetManager = context.assets
            return try {
                val istr = assetManager.open(strName)
                BitmapFactory.decodeStream(istr)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    companion object {
        private var emojisList = arrayOf(
            "emoji/10.png",
            "emoji/11.png",
            "emoji/12.png",
            "emoji/13.png",
            "emoji/14.png",
            "emoji/15.png",
            "emoji/16.png",
            "emoji/17.png",
            "emoji/18.png",
            "emoji/19.png",
            "emoji/110.png",
            "emoji/111.png",
            "emoji/112.png",
            "emoji/113.png",
            "emoji/114.png",
            "emoji/115.png",
            "emoji/116.png",
            "emoji/117.png",
            "emoji/118.png",
            "emoji/119.png",
            "emoji/120.png",
            "emoji/121.png",
            "emoji/122.png",
            )
    }
}