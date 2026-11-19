package com.example.unitime.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import com.example.unitime.R
import com.google.android.material.button.MaterialButton

fun showDeleteConfirmationDialog(
    context: Context,
    title: String,
    message: String,
    onConfirm: () -> Unit
) {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

    val view = LayoutInflater.from(context)
        .inflate(R.layout.dialog_delete_confirmation, null)

    view.findViewById<TextView>(R.id.dialogTitle).text = title
    view.findViewById<TextView>(R.id.dialogMessage).text = message

    dialog.setContentView(view)
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(true)

    view.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
        dialog.dismiss()
    }
    view.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
        dialog.dismiss()
        onConfirm()
    }

    dialog.show()

    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}