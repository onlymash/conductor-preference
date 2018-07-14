package android.support.v7.widget

import android.os.Build
import android.support.annotation.RequiresApi
import android.view.LayoutInflater
import android.view.ViewGroup

import im.mash.preference.conductor.R

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuListAdapter(private val mWindow: SimpleMenuPopupWindow) : RecyclerView.Adapter<SimpleMenuListItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleMenuListItemHolder {
        return SimpleMenuListItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.simple_menu_item, parent, false))
    }

    override fun onBindViewHolder(holder: SimpleMenuListItemHolder, position: Int) {
        holder.bind(mWindow, position)
    }

    override fun getItemCount(): Int {
        return mWindow.entries?.size ?: 0
    }
}
