/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.createBitmap
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.DialogCaptchaBinding
import org.slf4j.LoggerFactory

class CaptchaFragment : DialogFragment() {
    private lateinit var binding: DialogCaptchaBinding
    private var fragmentCallBack: FragmentCallback? = null
    private var password: String? = null
    private var username: String? = null
    private var email: String? = null
    private var isSignup: Boolean = false
    private var captchaArt: String? = null
    private var secureToken: String? = null
    private val logger = LoggerFactory.getLogger("basic")

    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogCaptchaBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun decodeBase64ToArt(base64: String): String? {
        try {
            val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            return String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.debug(e.toString())
        }
        return null
    }

    private fun createAsciiArtBitmap(text: String): Bitmap {
        val paint = Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = 16f
            color = Color.BLACK
            isAntiAlias = false
            letterSpacing = 0.05f
        }
        val lines = text.split("\n").filter { it.isNotEmpty() }
        val testRect = android.graphics.Rect()
        paint.getTextBounds("█", 0, 1, testRect)
        val charWidth = testRect.width().toFloat()
        val charHeight = testRect.height().toFloat()

        val maxLineLength = lines.maxOfOrNull { it.length } ?: 0
        val calculatedWidth = (charWidth * maxLineLength * 1.1f).toInt()

        val lineSpacing = charHeight * 1.2f
        val totalHeight = (lineSpacing * lines.size).toInt()
        val padding = 80
        val bitmap = createBitmap(calculatedWidth + padding, totalHeight + padding)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = charHeight + (padding / 2)
        for (line in lines) {
            canvas.drawText(line, (padding / 2).toFloat(), y, paint)
            y += lineSpacing
        }
        return bitmap
    }

    private fun loadCaptcha() {
        captchaArt?.let { art ->
            val captchaText = decodeBase64ToArt(art)
            captchaText?.let { text ->
                val bitmap = createAsciiArtBitmap(text)
                binding.asciiView.setImageBitmap(bitmap)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        username = arguments?.getString("username")
        password = arguments?.getString("password")
        email = arguments?.getString("email")
        isSignup = arguments?.getBoolean("isSignup", false) ?: false
        captchaArt = arguments?.getString("captchaArt")
        secureToken = arguments?.getString("secureToken")

        loadCaptcha()

        // Update button text based on flow
        binding.verifyButton.text = if (isSignup) {
            getString(com.windscribe.vpn.R.string.text_sign_up)
        } else {
            getString(com.windscribe.vpn.R.string.text_login)
        }

        binding.refreshButton.setOnClickListener {
            // Refresh captcha - request new one from presenter
            if (isSignup) {
                username?.let { user ->
                    password?.let { pass ->
                        fragmentCallBack?.onAuthSignUpClick(user, pass, email)
                    }
                }
            } else {
                username?.let { user ->
                    password?.let { pass ->
                        fragmentCallBack?.onAuthLoginClick(user, pass)
                    }
                }
            }
        }

        binding.refreshButton.setOnFocusChangeListener { _, hasFocus ->
            binding.refreshButton.alpha = if (hasFocus) 1.0f else 0.6f
        }

        binding.back.setOnClickListener {
            dismiss()
        }

        binding.back.setOnFocusChangeListener { _, hasFocus ->
            resetButtonTextColor(hasFocus)
        }

        binding.captchaSolution.setOnFocusChangeListener { _, _ ->
            resetButtonTextColor(false)
        }

        binding.verifyButton.setOnClickListener {
            username?.let { user ->
                password?.let { pass ->
                    val captchaSolution = binding.captchaSolution.text.toString()
                    if (isSignup) {
                        fragmentCallBack?.onSignUpButtonClick(
                            user, pass, email, true, secureToken, captchaSolution
                        )
                    } else {
                        fragmentCallBack?.onLoginButtonClick(
                            user, pass, "", secureToken, captchaSolution
                        )
                    }
                    dismiss()
                }
            }
        }

        binding.captchaSolution.doAfterTextChanged {
            // Clear any errors if needed
        }

        // Request focus on input field
        binding.captchaSolution.requestFocus()
    }

    private fun resetButtonTextColor(hasFocus: Boolean) {
        binding.back.setTextColor(
            if (hasFocus) {
                requireActivity().resources.getColor(R.color.colorWhite)
            } else {
                requireActivity().resources.getColor(R.color.colorWhite50)
            }
        )
    }

    companion object {
        fun newInstance(
            username: String,
            password: String,
            secureToken: String,
            captchaArt: String,
            email: String?,
            isSignup: Boolean
        ): CaptchaFragment {
            val fragment = CaptchaFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("password", password)
            bundle.putString("secureToken", secureToken)
            bundle.putString("captchaArt", captchaArt)
            bundle.putString("email", email)
            bundle.putBoolean("isSignup", isSignup)
            fragment.arguments = bundle
            return fragment
        }
    }
}