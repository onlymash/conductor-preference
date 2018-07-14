package im.mash.preference

import android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.RestrictTo
import android.support.annotation.XmlRes
import android.support.v4.app.Fragment
import android.support.v7.preference.AndroidResources
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceGroup
import android.support.v7.preference.PreferenceGroupAdapter
import android.support.v7.preference.PreferenceManager
import android.support.v7.preference.PreferenceRecyclerViewAccessibilityDelegate
import android.support.v7.preference.PreferenceScreen
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.preference.internal.AbstractMultiSelectListPreference
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.bluelinelabs.conductor.RestoreViewOnCreateController

import im.mash.preference.conductor.R

abstract class PreferenceController : RestoreViewOnCreateController(), PreferenceManager.OnPreferenceTreeClickListener, PreferenceManager.OnDisplayPreferenceDialogListener, PreferenceManager.OnNavigateToScreenListener, DialogPreference.TargetFragment {

    companion object {
        const val ARG_PREFERENCE_ROOT = "PreferenceController.PREFERENCE_ROOT"
        private const val PREFERENCES_TAG = "android:preferences"
        private const val DIALOG_CONTROLLER_TAG = "PreferenceController.DIALOG"
        private const val MSG_BIND_PREFERENCES = 1
    }

    /**
     * Returns the [PreferenceManager] used by this fragment.
     * @return The [PreferenceManager].
     */
    var preferenceManager: PreferenceManager? = null
        private set
    var listView: RecyclerView? = null
        private set
    private var mHavePrefs: Boolean = false
    private var mInitDone: Boolean = false
    private var mStyledContext: Context? = null
    private var mLayoutResId = R.layout.preference_list_fragment
    private val mDividerDecoration = DividerDecoration()
    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_BIND_PREFERENCES -> bindPreferences()
            }
        }
    }
    private val mRequestFocus = Runnable { listView!!.focusableViewAvailable(listView) }
    private var mSelectPreferenceRunnable: Runnable? = null
    /**
     * Gets the root of the preference hierarchy that this fragment is showing.
     *
     * @return The [PreferenceScreen] that is the root of the preference
     * hierarchy.
     */
    /**
     * Sets the root of the preference hierarchy that this fragment is showing.
     *
     * @param preferenceScreen The root [PreferenceScreen] of the preference hierarchy.
     */
    var preferenceScreen: PreferenceScreen?
        get() = preferenceManager!!.preferenceScreen
        set(preferenceScreen) {
            if (preferenceManager!!.setPreferences(preferenceScreen) && preferenceScreen != null) {
                onUnbindPreferences()
                mHavePrefs = true
                if (mInitDone) {
                    postBindPreferences()
                }
            }
        }

    val callbackFragment: Fragment?
        @RestrictTo(LIBRARY_GROUP)
        get() = null

    interface OnPreferenceStartFragmentCallback {
        fun onPreferenceStartFragment(caller: PreferenceController, pref: Preference): Boolean
    }

    interface OnPreferenceStartScreenCallback {
        fun onPreferenceStartScreen(caller: PreferenceController, pref: PreferenceScreen): Boolean
    }

    interface OnPreferenceDisplayDialogCallback {
        /**
         *
         * @param caller The fragment containing the preference requesting the dialog.
         * @param pref The preference requesting the dialog.
         * @return true if the dialog creation has been handled.
         */
        fun onPreferenceDisplayDialog(caller: PreferenceController,
                                      pref: Preference): Boolean
    }

    abstract fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)

    @SuppressLint("RestrictedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup,
                                     savedInstanceState: Bundle?): View {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        val theme = tv.resourceId
        if (theme == 0) {
            throw IllegalStateException("Must specify preferenceTheme in theme")
        }
        mStyledContext = ContextThemeWrapper(activity, theme)
        preferenceManager = PreferenceManager(mStyledContext!!)
        preferenceManager!!.onNavigateToScreenListener = this
        val rootKey = args.getString(ARG_PREFERENCE_ROOT)
        onCreatePreferences(savedInstanceState, rootKey)

        val a = mStyledContext!!.obtainStyledAttributes(null,
                R.styleable.PreferenceFragmentCompat,
                R.attr.preferenceFragmentCompatStyle,
                0)
        mLayoutResId = a.getResourceId(R.styleable.PreferenceFragmentCompat_android_layout,
                mLayoutResId)
        val divider = a.getDrawable(
                R.styleable.PreferenceFragmentCompat_android_divider)
        val dividerHeight = a.getDimensionPixelSize(
                R.styleable.PreferenceFragmentCompat_android_dividerHeight, -1)
        val allowDividerAfterLastItem = a.getBoolean(
                R.styleable.PreferenceFragmentCompat_allowDividerAfterLastItem, true)
        a.recycle()
        val themedContext = ContextThemeWrapper(inflater.context, theme)
        val themedInflater = inflater.cloneInContext(themedContext)
        val view = themedInflater.inflate(mLayoutResId, container, false)
        val rawListContainer = view.findViewById<View>(AndroidResources.ANDROID_R_LIST_CONTAINER) as? ViewGroup
                ?: throw RuntimeException("Content has view with id attribute " + "'android.R.id.list_container' that is not a ViewGroup class")
        val listView = onCreateRecyclerView(themedInflater, rawListContainer,
                savedInstanceState) ?: throw RuntimeException("Could not create RecyclerView")
        this.listView = listView
        listView.addItemDecoration(mDividerDecoration)
        setDivider(divider)
        if (dividerHeight != -1) {
            setDividerHeight(dividerHeight)
        }
        mDividerDecoration.setAllowDividerAfterLastItem(allowDividerAfterLastItem)
        rawListContainer.addView(this.listView)
        mHandler.post(mRequestFocus)
        onViewCreated(view, savedInstanceState)
        return view
    }

    fun setDivider(divider: Drawable?) {
        mDividerDecoration.setDivider(divider)
    }

    fun setDividerHeight(height: Int) {
        mDividerDecoration.setDividerHeight(height)
    }

    private fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (mHavePrefs) {
            bindPreferences()
            if (mSelectPreferenceRunnable != null) {
                mSelectPreferenceRunnable!!.run()
                mSelectPreferenceRunnable = null
            }
        }
        mInitDone = true

        if (savedInstanceState != null) {
            val container = savedInstanceState.getBundle(PREFERENCES_TAG)
            if (container != null) {
                val preferenceScreen = preferenceScreen
                preferenceScreen?.restoreHierarchyState(container)
            }
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        preferenceManager!!.onPreferenceTreeClickListener = this
        preferenceManager!!.onDisplayPreferenceDialogListener = this
    }

    override fun onDetach(view: View) {
        preferenceManager!!.onPreferenceTreeClickListener = null
        preferenceManager!!.onDisplayPreferenceDialogListener = null
    }

    override fun onDestroyView(view: View) {
        mHandler.removeCallbacks(mRequestFocus)
        mHandler.removeMessages(MSG_BIND_PREFERENCES)
        if (mHavePrefs) {
            unbindPreferences()
        }
        listView = null
        super.onDestroyView(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val preferenceScreen = preferenceScreen
        if (preferenceScreen != null) {
            val container = Bundle()
            preferenceScreen.saveHierarchyState(container)
            outState.putBundle(PREFERENCES_TAG, container)
        }
    }

    /**
     * Inflates the given XML resource and adds the preference hierarchy to the current
     * preference hierarchy.
     *
     * @param preferencesResId The XML resource ID to inflate.
     */
    @SuppressLint("RestrictedApi")
    fun addPreferencesFromResource(@XmlRes preferencesResId: Int) {
        requirePreferenceManager()
        preferenceScreen = preferenceManager!!.inflateFromResource(mStyledContext, preferencesResId, preferenceScreen)
    }

    @SuppressLint("RestrictedApi")
    fun setPreferencesFromResource(@XmlRes preferencesResId: Int, key: String?) {
        requirePreferenceManager()
        val xmlRoot = preferenceManager!!.inflateFromResource(mStyledContext,
                preferencesResId, null)
        val root: Preference
        if (key != null) {
            root = xmlRoot.findPreference(key)
            if (root !is PreferenceScreen) {
                throw IllegalArgumentException("Preference object with key " + key
                        + " is not a PreferenceScreen")
            }
        } else {
            root = xmlRoot
        }
        preferenceScreen = root as PreferenceScreen
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.fragment != null) {
            var handled = false
            if (callbackFragment is OnPreferenceStartFragmentCallback) {
                handled = (callbackFragment as OnPreferenceStartFragmentCallback)
                        .onPreferenceStartFragment(this, preference)
            }
            if (!handled && activity is OnPreferenceStartFragmentCallback) {
                handled = (activity as OnPreferenceStartFragmentCallback)
                        .onPreferenceStartFragment(this, preference)
            }
            return handled
        }
        return false
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        var handled = false
        if (callbackFragment is OnPreferenceStartScreenCallback) {
            handled = (callbackFragment as OnPreferenceStartScreenCallback)
                    .onPreferenceStartScreen(this, preferenceScreen)
        }
        if (!handled && activity is OnPreferenceStartScreenCallback) {
            (activity as OnPreferenceStartScreenCallback)
                    .onPreferenceStartScreen(this, preferenceScreen)
        }
    }

    override fun findPreference(key: CharSequence): Preference? {
        return if (preferenceManager == null) {
            null
        } else preferenceManager!!.findPreference(key)
    }

    private fun requirePreferenceManager() {
        if (preferenceManager == null) {
            throw RuntimeException("This should be called after super.onCreate.")
        }
    }

    private fun postBindPreferences() {
        if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) return
        mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget()
    }

    private fun bindPreferences() {
        val preferenceScreen = preferenceScreen
        if (preferenceScreen != null) {
            listView!!.adapter = onCreateAdapter(preferenceScreen)
            preferenceScreen.onAttached()
        }
        onBindPreferences()
    }

    private fun unbindPreferences() {
        val preferenceScreen = preferenceScreen
        preferenceScreen?.onDetached()
        onUnbindPreferences()
    }

    /** @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    open fun onBindPreferences() {
    }

    /** @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    open fun onUnbindPreferences() {
    }

    @SuppressLint("RestrictedApi")
    open fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup,
                             savedInstanceState: Bundle?): RecyclerView {
        val recyclerView = inflater
                .inflate(R.layout.preference_recyclerview, parent, false) as RecyclerView
        recyclerView.layoutManager = onCreateLayoutManager()
        recyclerView.setAccessibilityDelegateCompat(
                PreferenceRecyclerViewAccessibilityDelegate(recyclerView))
        return recyclerView
    }

    open fun onCreateLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(activity)
    }

    @SuppressLint("RestrictedApi")
    open fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return PreferenceGroupAdapter(preferenceScreen)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        var handled = false
        if (callbackFragment is OnPreferenceDisplayDialogCallback) {
            handled = (callbackFragment as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
        }
        if (!handled && activity is OnPreferenceDisplayDialogCallback) {
            handled = (activity as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
        }
        if (handled) {
            return
        }
        // check if dialog is already showing
        if (router.getControllerWithTag(DIALOG_CONTROLLER_TAG) != null) {
            return
        }
        val f: PreferenceDialogController
        if (preference is EditTextPreference) {
            f = EditTextPreferenceDialogController.newInstance(preference.getKey())
        } else if (preference is ListPreference) {
            f = ListPreferenceDialogController.newInstance(preference.getKey())
        } else if (preference is AbstractMultiSelectListPreference) {
            f = MultiSelectListPreferenceDialogController.newInstance(preference.getKey())
        } else {
            throw IllegalArgumentException("Tried to display dialog for unknown " + "preference type. Did you forget to override onDisplayPreferenceDialog()?")
        }
        f.targetController = this
        f.showDialog(router, DIALOG_CONTROLLER_TAG)
    }

    fun scrollToPreference(key: String) {
        scrollToPreferenceInternal(null, key)
    }

    fun scrollToPreference(preference: Preference) {
        scrollToPreferenceInternal(preference, null)
    }

    private fun scrollToPreferenceInternal(preference: Preference?, key: String?) {
        val r = Runnable {
            val adapter = listView!!.adapter
            if (adapter !is PreferenceGroup.PreferencePositionCallback) {
                if (adapter != null) {
                    throw IllegalStateException("Adapter must implement " + "PreferencePositionCallback")
                } else {
                    // Adapter was set to null, so don't scroll I guess?
                    return@Runnable
                }
            }
            val position: Int
            position = if (preference != null) {
                (adapter as PreferenceGroup.PreferencePositionCallback)
                        .getPreferenceAdapterPosition(preference)
            } else {
                (adapter as PreferenceGroup.PreferencePositionCallback)
                        .getPreferenceAdapterPosition(key)
            }
            if (position != RecyclerView.NO_POSITION) {
                listView!!.scrollToPosition(position)
            } else {
                // Item not found, wait for an update and try again
                adapter.registerAdapterDataObserver(ScrollToPreferenceObserver(adapter, listView!!, preference, key!!))
            }
        }
        if (listView == null) {
            mSelectPreferenceRunnable = r
        } else {
            r.run()
        }
    }

    private class ScrollToPreferenceObserver(private val mAdapter: RecyclerView.Adapter<*>, private val mList: RecyclerView,
                                             private val mPreference: Preference?, private val mKey: String) : RecyclerView.AdapterDataObserver() {
        private fun scrollToPreference() {
            mAdapter.unregisterAdapterDataObserver(this)
            val position = if (mPreference != null) {
                (mAdapter as PreferenceGroup.PreferencePositionCallback).getPreferenceAdapterPosition(mPreference)
            } else {
                (mAdapter as PreferenceGroup.PreferencePositionCallback).getPreferenceAdapterPosition(mKey)
            }
            if (position != RecyclerView.NO_POSITION) {
                mList.scrollToPosition(position)
            }
        }

        override fun onChanged() {
            scrollToPreference()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            scrollToPreference()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            scrollToPreference()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            scrollToPreference()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            scrollToPreference()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            scrollToPreference()
        }
    }

    private inner class DividerDecoration : RecyclerView.ItemDecoration() {
        private var mDivider: Drawable? = null
        private var mDividerHeight: Int = 0
        private var mAllowDividerAfterLastItem = true
        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (mDivider == null) {
                return
            }
            val childCount = parent.childCount
            val width = parent.width
            for (childViewIndex in 0 until childCount) {
                val view = parent.getChildAt(childViewIndex)
                if (shouldDrawDividerBelow(view, parent)) {
                    val top = view.y.toInt() + view.height
                    mDivider!!.setBounds(0, top, width, top + mDividerHeight)
                    mDivider!!.draw(c)
                }
            }
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                    state: RecyclerView.State) {
            if (shouldDrawDividerBelow(view, parent)) {
                outRect.bottom = mDividerHeight
            }
        }

        private fun shouldDrawDividerBelow(view: View, parent: RecyclerView): Boolean {
            val holder = parent.getChildViewHolder(view)
            val dividerAllowedBelow = holder is PreferenceViewHolder && holder.isDividerAllowedBelow
            if (!dividerAllowedBelow) {
                return false
            }
            var nextAllowed = mAllowDividerAfterLastItem
            val index = parent.indexOfChild(view)
            if (index < parent.childCount - 1) {
                val nextView = parent.getChildAt(index + 1)
                val nextHolder = parent.getChildViewHolder(nextView)
                nextAllowed = nextHolder is PreferenceViewHolder && nextHolder.isDividerAllowedAbove
            }
            return nextAllowed
        }

        fun setDivider(divider: Drawable?) {
            mDividerHeight = divider?.intrinsicHeight ?: 0
            mDivider = divider
            listView!!.invalidateItemDecorations()
        }

        fun setDividerHeight(dividerHeight: Int) {
            mDividerHeight = dividerHeight
            listView!!.invalidateItemDecorations()
        }

        fun setAllowDividerAfterLastItem(allowDividerAfterLastItem: Boolean) {
            mAllowDividerAfterLastItem = allowDividerAfterLastItem
        }
    }
}