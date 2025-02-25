/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.tv.upgrade

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentPlansBinding
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.BillingFragmentCallback
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.WindscribeInAppProduct

class PlansFragment : Fragment(), OnClickListener {

    private var isEmailAdded = false
    private var isEmailConfirmed = false
    private var mBillingListener: BillingFragmentCallback? = null
    private var mWindscribeInAppProduct: WindscribeInAppProduct? = null
    private lateinit var binding: FragmentPlansBinding


    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mBillingListener = context as BillingFragmentCallback
        } catch (ignored: ClassCastException) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mWindscribeInAppProduct?.let {
            if (it.isPromo()) {
                setPromoPlan(it)
            } else {
                setPlans(it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setPlans(windscribeInAppProduct: WindscribeInAppProduct) {
        if (windscribeInAppProduct.getSkus().isEmpty()) {
            return
        }
        windscribeInAppProduct.getSkus().forEach {
            val viewGroup: LinearLayoutCompat =
                layoutInflater.inflate(R.layout.plan_button_layout, null) as LinearLayoutCompat
            val labelView = viewGroup.findViewById<TextView>(R.id.label)
            val priceView = viewGroup.findViewById<TextView>(R.id.price)
            val planView = viewGroup.findViewById<LinearLayout>(R.id.plan)
            priceView.text = windscribeInAppProduct.getPrice(it)
            labelView.text = windscribeInAppProduct.getPlanName(it)
            planView.setTag(R.id.sku_tag, it)
            planView.setOnClickListener(this)
            planView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    labelView.setTextColor(resources.getColor(R.color.sea_green))
                    priceView.setTextColor(resources.getColor(R.color.sea_green))
                } else {
                    labelView.setTextColor(resources.getColor(R.color.colorWhite48))
                    priceView.setTextColor(resources.getColor(R.color.colorWhite48))
                }
            }
            binding.planContainer.addView(viewGroup)
            if (windscribeInAppProduct is AmazonProducts) {
                binding.restorePurchase.visibility = View.VISIBLE
                binding.restorePurchase.setOnClickListener {
                    mBillingListener?.onRestorePurchaseClick()
                }
            }
        }
        val firstView = binding.planContainer[0]
        firstView.requestFocus()
    }

    @SuppressLint("SetTextI18n")
    private fun setPromoPlan(windscribeInAppProduct: WindscribeInAppProduct) {
        val it = windscribeInAppProduct.getSkus()[0]
        binding.promoSticker.visibility = View.VISIBLE
        binding.promoSticker.text = windscribeInAppProduct.getPromoStickerLabel(it)
        val viewGroup =
            layoutInflater.inflate(R.layout.promo_plan_button_layout, null) as LinearLayoutCompat
        val labelView = viewGroup.findViewById<TextView>(R.id.label)
        val priceView = viewGroup.findViewById<TextView>(R.id.price)
        val planView = viewGroup.findViewById<LinearLayout>(R.id.plan)
        priceView.text = " - ${windscribeInAppProduct.getDiscountLabel(it)}"
        labelView.text =
            "${windscribeInAppProduct.getPrice(it)}/${windscribeInAppProduct.getPlanDuration(it)}"
        planView.setTag(R.id.sku_tag, it)
        planView.setOnClickListener(this)
        planView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                labelView.setTextColor(resources.getColor(R.color.colorWhite))
                priceView.setTextColor(resources.getColor(R.color.colorWhite))
            } else {
                labelView.setTextColor(resources.getColor(R.color.colorWhite48))
                priceView.setTextColor(resources.getColor(R.color.colorWhite48))
            }
        }
        binding.planContainer.addView(viewGroup)
        planView.requestFocus()

        val backButton = viewGroup.findViewById(R.id.back) as TextView
        backButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                backButton.setTextColor(resources.getColor(R.color.colorWhite))
            } else {
                backButton.setTextColor(resources.getColor(R.color.colorWhite50))
            }
        }
        backButton.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    fun add(
        activity: AppCompatActivity,
        products: WindscribeInAppProduct,
        container: Int,
        addToBackStack: Boolean,
        isEmailAdded: Boolean,
        isEmailConfirmed: Boolean
    ) {
        this.isEmailAdded = isEmailAdded
        this.isEmailConfirmed = isEmailConfirmed
        this.mWindscribeInAppProduct = products
        enterTransition = Slide(Gravity.BOTTOM)
        val transaction = activity.supportFragmentManager
            .beginTransaction()
            .replace(container, this)
        if (addToBackStack) {
            transaction.addToBackStack(this.javaClass.name)
        }
        transaction.commit()
    }

    companion object {

        @JvmStatic
        fun newInstance(): PlansFragment {
            return PlansFragment()
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            val viewTag = it.getTag(R.id.sku_tag) as String?
            viewTag?.let {
                when (mWindscribeInAppProduct) {
                    is GoogleProducts -> {
                        val skuDetails =
                            (mWindscribeInAppProduct as GoogleProducts).getSkuDetails(viewTag)
                        mBillingListener?.onContinuePlanClick(skuDetails, 0)
                    }

                    is AmazonProducts -> {
                        val product =
                            (mWindscribeInAppProduct as AmazonProducts).getProduct(viewTag)
                        mBillingListener?.onContinuePlanClick(product)
                    }

                    else -> {}
                }
            }
        }
    }
}
