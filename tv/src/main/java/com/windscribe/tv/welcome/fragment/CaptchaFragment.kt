/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentCaptchaBinding
import org.slf4j.LoggerFactory
import kotlin.io.encoding.ExperimentalEncodingApi

class CaptchaFragment : Fragment(),  WelcomeActivityCallback {
    private lateinit var binding: FragmentCaptchaBinding
    private var fragmentCallBack: FragmentCallback? = null
    private var password: String? = null
    private var username: String? = null
    private val logger = LoggerFactory.getLogger("basic")
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCaptchaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(ExperimentalEncodingApi::class)
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
            textSize = 24f
            color = Color.GREEN
            isAntiAlias = false
        }
        
        val lines = text.split("\n")
        val maxWidth = lines.maxOfOrNull { paint.measureText(it) }?.toInt() ?: 0
        val lineHeight = paint.fontMetrics.let { it.bottom - it.top }
        val totalHeight = (lineHeight * lines.size).toInt()
        
        val bitmap = Bitmap.createBitmap(
            maxWidth + 20, 
            totalHeight + 20, 
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        
        var y = -paint.fontMetrics.top + 10
        for (line in lines) {
            canvas.drawText(line, 10f, y, paint)
            y += lineHeight
        }
        
        return bitmap
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        username = arguments?.getString("username")
        password = arguments?.getString("password")
        val captchaArt = arguments?.getString("captchaArt")
        val captchaText = decodeBase64ToArt(captchaArt!!)
        val token = arguments?.getString("secureToken")
        captchaText?.let { text ->
            val bitmap = createAsciiArtBitmap(text)
            binding.asciiView.setImageBitmap(bitmap)
        }
        binding.back.setOnClickListener {
            fragmentCallBack?.onBackButtonPressed()
        }
        binding.back.setOnFocusChangeListener { _, _ ->
            resetButtonTextColor()
        }
        binding.captchaSolution.setOnFocusChangeListener { _, _ ->
            resetButtonTextColor()
        }
        binding.loginSignUp.setOnClickListener {
            username?.let {
                password?.let { pass ->
                    fragmentCallBack?.onLoginButtonClick(
                        it, pass, "", token, binding.captchaSolution.text.toString()
                    )
                }
            }
        }
        binding.captchaSolution.doAfterTextChanged {
            clearInputErrors()
        }
    }

    override fun clearInputErrors() {
        binding.error.visibility = View.INVISIBLE
        binding.error.text = ""
    }

    override fun setLoginError(error: String) {
        binding.error.visibility = View.VISIBLE
        binding.error.text = error
    }

    private fun resetButtonTextColor() {
        binding.back.setTextColor(
            if (binding.back.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
    }
}
