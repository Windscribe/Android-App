/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity

import android.annotation.SuppressLint
import android.app.AlertDialog.Builder
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RadioGroup.OnCheckedChangeListener
import android.widget.TextView
import android.widget.TextView.BufferType.SPANNABLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.R.*
import com.windscribe.vpn.billing.AmazonProducts
import com.windscribe.vpn.billing.BillingFragmentCallback
import com.windscribe.vpn.billing.GoogleProducts
import com.windscribe.vpn.billing.WindscribeInAppProduct

class PlansFragment : Fragment(), OnCheckedChangeListener {

    @JvmField
    @BindView(R.id.continueToFree)
    var continueToFreeButton: Button? = null

    @JvmField
    @BindView(R.id.continueToPremium)
    var continueToPremiumButton: Button? = null

    @JvmField
    @BindView(R.id.planOptionContainer)
    var planRadioGroup: RadioGroup? = null

    @JvmField
    @BindView(R.id.promoPlan)
    var promoPlan: TextView? = null

    @JvmField
    @BindView(R.id.promoSticker)
    var promoSticker: TextView? = null

    @JvmField
    @BindView(R.id.terms_policy)
    var termAndPolicyView: TextView? = null

    @JvmField
    @BindView(R.id.nav_title)
    var titleView: TextView? = null

    private var isEmailAdded = false
    private var isEmailConfirmed = false
    private var billingListener: BillingFragmentCallback? = null
    private var windscribeInAppProduct: WindscribeInAppProduct? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            billingListener = context as BillingFragmentCallback
        } catch (ignored: ClassCastException) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layout.fragment_plans, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView?.text = getString(string.plans)
        setEmailStatus(isEmailAdded, isEmailConfirmed)
        showPlans()
        setTermAndPolicyText()
        planRadioGroup?.setOnCheckedChangeListener(this)
        if (planRadioGroup?.size!! > 0) {
            val firstItem = planRadioGroup?.get(0) as RadioButton
            planRadioGroup?.check(firstItem.id)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPlans() {
        windscribeInAppProduct?.let { plan ->
            if(plan.getSkus().isEmpty()) {
                return
            }
            if (plan.isPromo()) {
                planRadioGroup?.visibility = View.GONE
                promoPlan?.visibility = View.VISIBLE
                promoSticker?.visibility = View.VISIBLE
                val firstPlan = plan.getSkus().first()
                val planText: Spannable =
                    SpannableString("${plan.getPrice(firstPlan)}/${plan.getPlanDuration(firstPlan)}")
                promoPlan?.append(planText)
                val divider: Spannable = SpannableString("  |  ")
                context?.let {
                    divider.setSpan(
                        ForegroundColorSpan(it.resources.getColor(R.color.colorWhite15)),
                        0,
                        5,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                promoPlan?.append(divider)
                val discountText: Spannable = SpannableString(plan.getDiscountLabel(firstPlan))
                promoPlan?.append(discountText)
                promoSticker?.text = plan.getPromoStickerLabel(firstPlan)
                continueToPremiumButton?.setTag(R.id.sku_tag, firstPlan)
                continueToPremiumButton?.isEnabled = true
                continueToPremiumButton?.text =
                    "Continue ${plan.getPrice(firstPlan)}/${plan.getPlanDuration(firstPlan)}"
            } else {
                planRadioGroup?.visibility = View.VISIBLE
                promoPlan?.visibility = View.GONE
                promoSticker?.visibility = View.GONE
                plan.getSkus().forEach {
                    val params = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.WRAP_CONTENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.weight = 1F
                    val radioButton =
                        this.layoutInflater.inflate(layout.plan_radio_option, null) as RadioButton
                    val planDuration = plan.getPlanDuration(it)
                    val planPrice = plan.getPrice(it)
                    radioButton.text = "$planPrice/$planDuration"
                    radioButton.setTag(R.id.sku_tag, it)
                    planRadioGroup?.addView(radioButton, params)
                }
            }
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
        this.windscribeInAppProduct = products
        enterTransition = Slide(Gravity.BOTTOM).addTarget(R.id.plan_fragment_container)
        val transaction = activity.supportFragmentManager
            .beginTransaction()
            .replace(container, this)
        if (addToBackStack) {
            transaction.addToBackStack(this.javaClass.name)
        }
        transaction.commit()
    }

    @OnClick(R.id.continueToFree)
    fun tenGbFree() {
        billingListener?.onTenGbFreeClick()
    }

    @OnClick(R.id.nav_button)
    fun onBackPressed() {
        requireActivity().onBackPressed()
    }

    @SuppressLint("SetTextI18n")
    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        val viewTag = view?.findViewById<RadioButton>(checkedId)?.getTag(R.id.sku_tag) as String?
        viewTag?.let {
            val planDuration = windscribeInAppProduct?.getPlanDuration(viewTag)
            val planPrice = windscribeInAppProduct?.getPrice(viewTag)
            planDuration.let {
                planPrice.let {
                    continueToPremiumButton?.isEnabled = true
                    continueToPremiumButton?.text = "Continue $planPrice/$planDuration"
                    continueToPremiumButton?.setTag(R.id.sku_tag, viewTag)
                }
            }
        }
    }

    @OnClick(R.id.firstInfoIcon)
    fun onFirstInfoIconClick() {
        showDialog(getString(string.as_much_as_bandwidth_you_like))
    }

    @OnClick(R.id.continueToPremium)
    fun onPlanClicked() {
        val viewTag = continueToPremiumButton?.getTag(R.id.sku_tag) as String?
        viewTag?.let {
            when (windscribeInAppProduct) {
                is GoogleProducts -> {
                    val skuDetails =
                        (windscribeInAppProduct as GoogleProducts).getSkuDetails(viewTag)
                    billingListener?.onContinuePlanClick(skuDetails, 0)
                }
                is AmazonProducts -> {
                    val product = (windscribeInAppProduct as AmazonProducts).getProduct(viewTag)
                    billingListener?.onContinuePlanClick(product)
                }
                else -> {}
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.secondInfoIcon)
    fun onSecondInfoIconClick() {
        showDialog(getString(string.access_to_multiple_servers))
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.thirdInfoIcon)
    fun onThirdInfoIconClick() {
        showDialog(getString(string.ad_malware_blocker))
    }

    fun setEmailStatus(isEmailAdded: Boolean, isEmailConfirmed: Boolean) {
        continueToFreeButton?.visibility =
            if (isEmailAdded && isEmailConfirmed) View.GONE else View.VISIBLE
    }

    private fun setTermAndPolicyText() {
        val appName = getString(string.app_name)
        val termAndPolicyText = getString(string.terms_policy_en)
        val fullText = "$appName $termAndPolicyText"
        val spannable: Spannable = SpannableString(fullText)
        val spanStart = fullText.length - termAndPolicyText.length
        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE),
            spanStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val termsSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                billingListener?.onTermsClick()
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.color = textPaint.linkColor
                textPaint.isUnderlineText = false
            }
        }
        val policySpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                billingListener?.onPolicyClick()
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.color = textPaint.linkColor
                textPaint.isUnderlineText = false
            }
        }
        if (fullText.indexOf("&") != -1) {
            spannable.setSpan(
                termsSpan, spanStart, fullText.indexOf("&") - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                policySpan,
                fullText.indexOf("&") + 1,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            termAndPolicyView?.movementMethod = LinkMovementMethod.getInstance()
            termAndPolicyView?.setText(spannable, SPANNABLE)
        }
    }

    private fun showDialog(message: String) {
        Builder(context, style.tool_tip_dialog)
            .setMessage(message)
            .setPositiveButton("OK") { dialog1: DialogInterface, _: Int -> dialog1.cancel() }.show()
    }

    companion object {
        @JvmStatic
        fun newInstance(): PlansFragment {
            return PlansFragment()
        }
    }
}
