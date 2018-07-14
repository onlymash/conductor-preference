package android.support.v7.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v7.animation.SimpleMenuAnimation
import android.support.v7.drawable.FixedBoundsDrawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow

import java.util.Arrays

import im.mash.preference.conductor.R

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

/**
 * Extension of [PopupWindow] that implements
 * [Simple Menus](https://material.io/guidelines/components/menus.html#menus-simple-menus)
 * in Material Design.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuPopupWindow @SuppressLint("InflateParams")
constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : PopupWindow(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        const val POPUP_MENU = 0
        const val DIALOG = 1

        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }

    protected val elevation = IntArray(2)
    protected val margin = Array(2) { IntArray(2) }
    val listPadding = Array(2) { IntArray(2) }
    protected val itemHeight: Int
    protected val dialogMaxWidth: Int
    protected val unit: Int
    protected val maxUnits: Int

    var mode = POPUP_MENU
        private set

    private var mRequestMeasure = true

    private val mList: RecyclerView
    private val mAdapter: SimpleMenuListAdapter

    var onItemClickListener: OnItemClickListener? = null
    var entries: Array<CharSequence>? = null
    var selectedIndex: Int = 0

    private var mMeasuredWidth: Int = 0

    interface OnItemClickListener {
        fun onClick(i: Int)
    }

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.styleable.SimpleMenuPreference_popupStyle) : this(context, attrs, defStyleAttr, R.style.Preference_SimpleMenuPreference_Popup) {}

    init {

        isFocusable = true
        isOutsideTouchable = false

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.SimpleMenuPopup, defStyleAttr, defStyleRes)

        elevation[POPUP_MENU] = a.getDimension(R.styleable.SimpleMenuPopup_listElevation, 4f).toInt()
        elevation[DIALOG] = a.getDimension(R.styleable.SimpleMenuPopup_dialogElevation, 48f).toInt()
        margin[POPUP_MENU][HORIZONTAL] = a.getDimension(R.styleable.SimpleMenuPopup_listMarginHorizontal, 0f).toInt()
        margin[POPUP_MENU][VERTICAL] = a.getDimension(R.styleable.SimpleMenuPopup_listMarginVertical, 0f).toInt()
        margin[DIALOG][HORIZONTAL] = a.getDimension(R.styleable.SimpleMenuPopup_dialogMarginHorizontal, 0f).toInt()
        margin[DIALOG][VERTICAL] = a.getDimension(R.styleable.SimpleMenuPopup_dialogMarginVertical, 0f).toInt()
        listPadding[POPUP_MENU][HORIZONTAL] = a.getDimension(R.styleable.SimpleMenuPopup_listItemPadding, 0f).toInt()
        listPadding[DIALOG][HORIZONTAL] = a.getDimension(R.styleable.SimpleMenuPopup_dialogItemPadding, 0f).toInt()
        dialogMaxWidth = a.getDimension(R.styleable.SimpleMenuPopup_dialogMaxWidth, 0f).toInt()
        unit = a.getDimension(R.styleable.SimpleMenuPopup_unit, 0f).toInt()
        maxUnits = a.getInteger(R.styleable.SimpleMenuPopup_maxUnits, 0)

        mList = LayoutInflater.from(context).inflate(R.layout.simple_menu_list, null) as RecyclerView
        mList.isFocusable = true
        mList.layoutManager = LinearLayoutManager(context)
        mList.itemAnimator = null
        contentView = mList

        mAdapter = SimpleMenuListAdapter(this)
        mList.adapter = mAdapter

        a.recycle()

        // TODO do not hardcode
        itemHeight = Math.round(context.resources.displayMetrics.density * 48)
        listPadding[DIALOG][VERTICAL] = Math.round(context.resources.displayMetrics.density * 8)
        listPadding[POPUP_MENU][VERTICAL] = listPadding[DIALOG][VERTICAL]
    }

    override fun getContentView(): RecyclerView {
        return super.getContentView() as RecyclerView
    }

    override fun getBackground(): FixedBoundsDrawable? {
        val background = super.getBackground()
        if (background != null && background !is FixedBoundsDrawable) {
            setBackgroundDrawable(background)
        }
        return super.getBackground() as FixedBoundsDrawable
    }

    override fun setBackgroundDrawable(background: Drawable?) {
        var bg: Drawable? = background
                ?: throw IllegalStateException("SimpleMenuPopupWindow must have a background")

        if (bg !is FixedBoundsDrawable) {
            bg = FixedBoundsDrawable(bg!!)
        }
        super.setBackgroundDrawable(bg)
    }

    /**
     * Show the PopupWindow
     *
     * @param anchor View that will be used to calc the position of windows
     * @param container View that will be used to calc the position of windows
     * @param extraMargin extra margin start
     */
    fun show(anchor: View, container: View, extraMargin: Int) {
        val maxMaxWidth = container.width - margin[POPUP_MENU][HORIZONTAL] * 2
        val measuredWidth = measureWidth(maxMaxWidth, entries)
        if (measuredWidth == -1) {
            mode = DIALOG
        } else if (measuredWidth != 0) {
            mode = POPUP_MENU

            mMeasuredWidth = measuredWidth
        }

        mAdapter.notifyDataSetChanged()

        if (mode == POPUP_MENU) {
            showPopupMenu(anchor, container, mMeasuredWidth, extraMargin)
        } else {
            showDialog(anchor, container)
        }
    }

    /**
     * Show popup window in dialog mode
     *
     * @param parent a parent view to get the [View.getWindowToken] token from
     * @param container Container view that holds preference list, also used to calc width
     */
    private fun showDialog(parent: View, container: View) {
        val index = Math.max(0, selectedIndex)
        val count = entries!!.size

        contentView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        contentView.scrollToPosition(index)

        val width = Math.min(dialogMaxWidth, container.width - margin[DIALOG][HORIZONTAL] * 2)
        setWidth(width)
        height = WRAP_CONTENT
        animationStyle = R.style.Animation_SimpleMenuCenter
        setElevation(elevation[DIALOG].toFloat())

        super.showAtLocation(parent, Gravity.CENTER_VERTICAL, 0, 0)

        contentView.post {
            val w = contentView.width
            val h = contentView.height
            val start = Rect(width / 2, h / 2, w / 2, h / 2)

            SimpleMenuAnimation.startEnterAnimation(contentView, background!!,
                    width, h, w / 2, h / 2, start, itemHeight, elevation[DIALOG] / 4, index)
        }

        contentView.post {
            // disable over scroll when no scroll
            val lm = contentView.layoutManager as LinearLayoutManager?
            if (lm!!.findFirstCompletelyVisibleItemPosition() == 0 && lm.findLastCompletelyVisibleItemPosition() == count - 1) {
                contentView.overScrollMode = View.OVER_SCROLL_NEVER
            }
        }
    }

    /**
     * Show popup window in popup mode
     *
     * @param anchor View that will be used to calc the position of the window
     * @param container Container view that holds preference list, also used to calc width
     * @param width Measured width of this window
     */
    private fun showPopupMenu(anchor: View, container: View, width: Int, extraMargin: Int) {
        val rtl = container.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        val index = Math.max(0, selectedIndex)
        val count = entries!!.size

        val anchorTop = anchor.top
        val anchorHeight = anchor.height
        val measuredHeight = itemHeight * count + listPadding[POPUP_MENU][VERTICAL] * 2

        val location = IntArray(2)
        container.getLocationInWindow(location)

        val containerTopInWindow = location[1]
        val containerHeight = container.height

        var y: Int

        var height = measuredHeight
        val elevation = this.elevation[POPUP_MENU]
        val centerX = if (rtl)
            location[0] + extraMargin - width + listPadding[POPUP_MENU][HORIZONTAL]
        else
            location[0] + extraMargin + listPadding[POPUP_MENU][HORIZONTAL]
        val centerY: Int
        val animItemHeight = itemHeight + listPadding[POPUP_MENU][VERTICAL] * 2
        var animIndex = index
        val animStartRect: Rect

        if (height > containerHeight) {
            // too high, use scroll
            y = containerTopInWindow + margin[POPUP_MENU][VERTICAL]

            // scroll to select item
            val scroll = itemHeight * index - anchorTop + listPadding[POPUP_MENU][VERTICAL] + margin[POPUP_MENU][VERTICAL] - anchorHeight / 2 + itemHeight / 2

            contentView.post {
                contentView.scrollBy(0, -measuredHeight) // to top
                contentView.scrollBy(0, scroll)
            }
            contentView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

            height = containerHeight - margin[POPUP_MENU][VERTICAL] * 2

            animIndex = index

            centerY = itemHeight * index
        } else {
            // calc align to selected
            y = (containerTopInWindow + anchorTop + anchorHeight / 2 - itemHeight / 2
                    - listPadding[POPUP_MENU][VERTICAL] - index * itemHeight)

            // make sure window is in parent view
            val maxY = (containerTopInWindow + containerHeight
                    - measuredHeight - margin[POPUP_MENU][VERTICAL])
            y = Math.min(y, maxY)

            val minY = containerTopInWindow + margin[POPUP_MENU][VERTICAL]
            y = Math.max(y, minY)

            contentView.overScrollMode = View.OVER_SCROLL_NEVER

            // center of selected item
            centerY = (listPadding[POPUP_MENU][VERTICAL].toDouble() + (index * itemHeight).toDouble() + itemHeight * 0.5).toInt()
        }

        setWidth(width)
        setHeight(height)
        setElevation(elevation.toFloat())
        animationStyle = R.style.Animation_SimpleMenuCenter

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            enterTransition = null
            exitTransition = null
        }

        super.showAtLocation(anchor, Gravity.NO_GRAVITY, centerX, y)

        val startTop = centerY - (itemHeight * 0.2).toInt()
        val startBottom = centerY + (itemHeight * 0.2).toInt()
        val startLeft: Int
        val startRight: Int

        if (!rtl) {
            startLeft = centerX
            startRight = centerX + unit
        } else {
            startLeft = centerX + width - unit
            startRight = centerX + width
        }

        animStartRect = Rect(startLeft, startTop, startRight, startBottom)

        val animElevation = Math.round(elevation * 0.25).toInt()

        SimpleMenuAnimation.postStartEnterAnimation(this, background!!,
                width, height, centerX, centerY, animStartRect, animItemHeight, animElevation, animIndex)
    }

    /**
     * Request a measurement before next show, call this when entries changed.
     */
    fun requestMeasure() {
        mRequestMeasure = true
    }

    /**
     * Measure window width
     *
     * @param maxWidth max width for popup
     * @param entries Entries of preference hold this window
     * @return  0: skip
     * -1: use dialog
     * other: measuredWidth
     */
    private fun measureWidth(maxWidth: Int, entries: Array<CharSequence>?): Int {
        var mw = maxWidth
        var es = entries
        // skip if should not measure
        if (!mRequestMeasure) {
            return 0
        }

        mRequestMeasure = false

        es = Arrays.copyOf(es!!, es.size)

        Arrays.sort(es) { o1, o2 -> o2.length - o1.length }

        val context = contentView.context
        var width = 0

        mw = Math.min(unit * maxUnits, mw)

        val bounds = Rect()
        val textPaint = TextPaint()
        // TODO do not hardcode
        textPaint.textSize = 16 * context.resources.displayMetrics.scaledDensity

        for (chs in es) {
            textPaint.getTextBounds(chs.toString(), 0, chs.length, bounds)

            width = Math.max(width, bounds.width() + listPadding[POPUP_MENU][HORIZONTAL] * 2)

            // more than one line should use dialog
            if (width > mw || chs.toString().contains("\n")) {
                return -1
            }
        }

        // width is a multiple of a unit
        var w = 0
        while (width > w) {
            w += unit
        }

        return w
    }

    override fun showAtLocation(parent: View, gravity: Int, x: Int, y: Int) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    override fun showAsDropDown(anchor: View) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    override fun showAsDropDown(anchor: View, xoff: Int, yoff: Int) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    override fun showAsDropDown(anchor: View, xoff: Int, yoff: Int, gravity: Int) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }
}