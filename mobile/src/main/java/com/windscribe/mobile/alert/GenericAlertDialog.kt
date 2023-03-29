package com.windscribe.mobile.alert

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.windscribe.mobile.R

/**
 * Interface to receive events from GenericAlertDialog.
 */
interface GenericAlertCallback {
    fun onAlertDialogButtonClick(type: DialogType)
}

/**
 * Generic alert dialog type.
 */
enum class DialogType {
    EmergencyConnect
}

/**
 * Use it to configure this dialog.
 */
data class GenericAlertData(
    val icon: Int,
    val title: String,
    val description: String,
    val okText: String,
    val type: DialogType = DialogType.EmergencyConnect
) : java.io.Serializable

/**
 * Fullscreen generic dialog.
 */
class GenericAlertDialog : Fragment() {
    private var genericAlertCallback: GenericAlertCallback? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.generic_alert_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val data = arguments?.getSerializable(argumentKey) as? GenericAlertData
        data?.let {
            view.findViewById<ImageView>(R.id.iconImage).setImageResource(data.icon)
            view.findViewById<TextView>(R.id.tvTitle).text = data.title
            view.findViewById<TextView>(R.id.tvDescription).text = data.description
            val okButton = view.findViewById<TextView>(R.id.ok)
            okButton.text = data.okText
            okButton.setOnClickListener {
                activity?.supportFragmentManager?.popBackStackImmediate()
                genericAlertCallback?.onAlertDialogButtonClick(data.type)
            }
            view.findViewById<View>(R.id.cancel).setOnClickListener {
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        genericAlertCallback = context as? GenericAlertCallback
    }

    companion object {
        const val argumentKey: String = "GenericAlertDialogArgumentKey"

        /**
         * shows full screen dialog.
         */
        fun show(
            data: GenericAlertData, fragmentManager: FragmentManager
        ) {
            val fragment = GenericAlertDialog()
            val bundle = Bundle()
            bundle.putSerializable(argumentKey, data)
            fragment.arguments = bundle
            fragmentManager.beginTransaction().addToBackStack("GenericAlertDialog")
                .setTransition(TRANSIT_FRAGMENT_OPEN).add(android.R.id.content, fragment).commit()
        }
    }
}