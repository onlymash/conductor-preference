package im.mash.preference

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView

class AlertDialogController : DialogController {

    companion object {
        private const val KEY_TITLE = "DialogController.title"
        private const val KEY_DESCRIPTION = "DialogController.description"
    }

    constructor(title: CharSequence, description: CharSequence) : super(Bundle().apply {
        putCharSequence(KEY_TITLE, title)
        putCharSequence(KEY_DESCRIPTION, description) })

    constructor(args: Bundle) : super(args)

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {

        val alertDialog = AlertDialog.Builder(activity!!)
                .setTitle(args.getCharSequence(KEY_TITLE))
                .setMessage(args.getCharSequence(KEY_DESCRIPTION))
                .setPositiveButton(android.R.string.ok, null)
                .create()

        alertDialog.setOnShowListener {
            val messageTextView = alertDialog.findViewById<View>(android.R.id.message) as TextView?
            if (messageTextView != null) {
                //Make links clickable in textview
                messageTextView.movementMethod = LinkMovementMethod.getInstance()
            }
        }

        return alertDialog
    }
}