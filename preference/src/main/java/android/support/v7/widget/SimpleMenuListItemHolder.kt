package android.support.v7.widget

import android.os.Build
import android.support.annotation.RequiresApi
import android.view.View
import android.widget.CheckedTextView

import android.support.v7.widget.SimpleMenuPopupWindow.Companion.DIALOG
import android.support.v7.widget.SimpleMenuPopupWindow.Companion.HORIZONTAL

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuListItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    var mCheckedTextView: CheckedTextView = itemView.findViewById(android.R.id.text1)

    private var mWindow: SimpleMenuPopupWindow? = null

    init {

        itemView.setOnClickListener(this)
    }

    fun bind(window: SimpleMenuPopupWindow, position: Int) {
        mWindow = window
        mCheckedTextView.text = mWindow!!.entries!![position]
        mCheckedTextView.isChecked = position == mWindow!!.selectedIndex
        mCheckedTextView.maxLines = if (mWindow!!.mode == DIALOG) Integer.MAX_VALUE else 1

        val padding = mWindow!!.listPadding[mWindow!!.mode][HORIZONTAL]
        val paddingVertical = mCheckedTextView.paddingTop
        mCheckedTextView.setPadding(padding, paddingVertical, padding, paddingVertical)
    }

    override fun onClick(view: View) {
        if (mWindow!!.onItemClickListener != null) {
            mWindow!!.onItemClickListener!!.onClick(adapterPosition)
        }

        if (mWindow!!.isShowing) {
            mWindow!!.dismiss()
        }
    }
}
