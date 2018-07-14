package im.mash.preference

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.RestrictTo
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView

import com.bluelinelabs.conductor.RestoreViewOnCreateController
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler

import android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP

abstract class PreferenceDialogController : RestoreViewOnCreateController(), DialogInterface.OnClickListener {

    companion object {

        const val ARG_KEY = "key"

        private const val SAVE_DIALOG_STATE_TAG = "android:savedDialogState"
        private const val SAVE_STATE_TITLE = "PreferenceDialogController.title"
        private const val SAVE_STATE_POSITIVE_TEXT = "PreferenceDialogController.positiveText"
        private const val SAVE_STATE_NEGATIVE_TEXT = "PreferenceDialogController.negativeText"
        private const val SAVE_STATE_MESSAGE = "PreferenceDialogController.message"
        private const val SAVE_STATE_LAYOUT = "PreferenceDialogController.layout"
        private const val SAVE_STATE_ICON = "PreferenceDialogController.icon"
    }

    private var mPreference: DialogPreference? = null

    private var mDialogTitle: CharSequence? = null
    private var mPositiveButtonText: CharSequence? = null
    private var mNegativeButtonText: CharSequence? = null
    private var mDialogMessage: CharSequence? = null
    @LayoutRes
    private var mDialogLayoutRes: Int = 0
    private var mDialogIcon: BitmapDrawable? = null
    /** Which button was clicked.  */
    private var mWhichButtonClicked: Int = 0
    protected var dialog: Dialog? = null
        private set
    private var dismissed: Boolean = false
    /**
     * Get the preference that requested this dialog. Available after [.onCreate] has
     * been called on the [PreferenceControllerCompat] which launched this dialog.
     *
     * @return The [DialogPreference] associated with this
     * dialog.
     */
    val preference: DialogPreference
        get() {
            if (mPreference == null) {
                val key = args.getString(ARG_KEY)
                val controller = targetController as DialogPreference.TargetFragment?
                mPreference = controller!!.findPreference(key) as DialogPreference
            }
            return mPreference!!
        }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup,
                              savedViewState: Bundle?): View {

        onCreate(savedViewState)

        dialog = onCreateDialog(savedViewState)
        dialog!!.ownerActivity = activity!!
        dialog!!.setOnDismissListener { dismissDialog() }
        if (savedViewState != null) {
            val dialogState = savedViewState.getBundle(SAVE_DIALOG_STATE_TAG)
            if (dialogState != null) {
                dialog!!.onRestoreInstanceState(dialogState)
            }
        }
        return View(activity)//stub view
    }

    open fun onCreate(savedInstanceState: Bundle?) {
        val rawController = targetController as? DialogPreference.TargetFragment
                ?: throw IllegalStateException("Target fragment must implement TargetFragment" + " interface")
        val key = args.getString(ARG_KEY)
        if (savedInstanceState == null) {
            mPreference = rawController.findPreference(key) as DialogPreference
            mDialogTitle = mPreference!!.dialogTitle
            mPositiveButtonText = mPreference!!.positiveButtonText
            mNegativeButtonText = mPreference!!.negativeButtonText
            mDialogMessage = mPreference!!.dialogMessage
            mDialogLayoutRes = mPreference!!.dialogLayoutResource
            val icon = mPreference!!.dialogIcon
            mDialogIcon = when (icon) {
                null -> null
                is BitmapDrawable -> icon
                else -> {
                    val bitmap = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    BitmapDrawable(resources, bitmap)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle)
        outState.putCharSequence(SAVE_STATE_POSITIVE_TEXT, mPositiveButtonText)
        outState.putCharSequence(SAVE_STATE_NEGATIVE_TEXT, mNegativeButtonText)
        outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage)
        outState.putInt(SAVE_STATE_LAYOUT, mDialogLayoutRes)
        if (mDialogIcon != null) {
            outState.putParcelable(SAVE_STATE_ICON, mDialogIcon!!.bitmap)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
        mPositiveButtonText = savedInstanceState.getCharSequence(SAVE_STATE_POSITIVE_TEXT)
        mNegativeButtonText = savedInstanceState.getCharSequence(SAVE_STATE_NEGATIVE_TEXT)
        mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
        mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0)
        val bitmap = savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_ICON)
        if (bitmap != null) {
            mDialogIcon = BitmapDrawable(resources, bitmap)
        }
    }

    fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val context = activity
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE
        val builder = AlertDialog.Builder(context!!)
                .setTitle(mDialogTitle)
                .setIcon(mDialogIcon)
                .setPositiveButton(mPositiveButtonText, this)
                .setNegativeButton(mNegativeButtonText, this)
        val contentView = onCreateDialogView(context)
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(mDialogMessage)
        }
        onPrepareDialogBuilder(builder)
        // Create the dialog
        val dialog = builder.create()
        if (needInputMethod()) {
            requestInputMethod(dialog)
        }
        return dialog
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        val dialogState = dialog!!.onSaveInstanceState()
        outState.putBundle(SAVE_DIALOG_STATE_TAG, dialogState)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        dialog!!.show()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        dialog!!.hide()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        dialog!!.setOnDismissListener(null)
        dialog!!.dismiss()
        dialog = null
        mPreference = null
    }

    /**
     * Display the dialog, create a transaction and pushing the controller.
     * @param router The router on which the transaction will be applied
     * @param tag The tag for this controller
     */
    @JvmOverloads
    fun showDialog(router: Router, tag: String? = null) {
        dismissed = false
        router.pushController(RouterTransaction.with(this)
                .pushChangeHandler(SimpleSwapChangeHandler(false))
                .popChangeHandler(SimpleSwapChangeHandler(false))
                .tag(tag))
    }

    /**
     * Dismiss the dialog and pop this controller
     */
    fun dismissDialog() {
        if (dismissed) {
            return
        }
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        router.popController(this)
        dismissed = true
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked.
     * Use this to set custom properties on the dialog.
     *
     *
     * Do not [AlertDialog.Builder.create] or
     * [AlertDialog.Builder.show].
     */
    open fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {}

    /**
     * Returns whether the preference needs to display a soft input method when the dialog
     * is displayed. Default is false. Subclasses should override this method if they need
     * the soft input method brought up automatically.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    open fun needInputMethod(): Boolean {
        return false
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private fun requestInputMethod(dialog: Dialog) {
        val window = dialog.window
        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    /**
     * Creates the content view for the dialog (if a custom content view is
     * required). By default, it inflates the dialog layout resource if it is
     * set.
     *
     * @return The content View for the dialog.
     * @see DialogPreference.setLayoutResource
     */
    fun onCreateDialogView(context: Context): View? {
        val resId = mDialogLayoutRes
        if (resId == 0) {
            return null
        }
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(resId, null)
    }

    open fun onBindDialogView(view: View) {
        val dialogMessageView = view.findViewById<View>(android.R.id.message)

        if (dialogMessageView != null) {
            val message = mDialogMessage
            var newVisibility = View.GONE

            if (!TextUtils.isEmpty(message)) {
                if (dialogMessageView is TextView) {
                    dialogMessageView.text = message
                }

                newVisibility = View.VISIBLE
            }

            if (dialogMessageView.visibility != newVisibility) {
                dialogMessageView.visibility = newVisibility
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        mWhichButtonClicked = which
    }

    abstract fun onDialogClosed(positiveResult: Boolean)
}