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
    var mContinueToFreeButton: Button? = null

    @JvmField
    @BindView(R.id.continueToPremium)
    var mContinueToPremiumButton: Button? = null

    @JvmField
    @BindView(R.id.planOptionContainer)
    var mPlanRadioGroup: RadioGroup? = null

    @JvmField
    @BindView(R.id.promoPlan)
    var mPromoPlan: TextView? = null

    @JvmField
    @BindView(R.id.promoSticker)
    var mPromoSticker: TextView? = null

    @JvmField
    @BindView(R.id.terms_policy)
    var mTermAndPolicyView: TextView? = null

    @JvmField
    @BindView(R.id.nav_title)
    var mTitleView: TextView? = null

    private var isEmailAdded = false
    private var isEmailConfirmed = false
    private var mBillingListener: BillingFragmentCallback? = null
    private var mWindscribeInAppProduct: WindscribeInAppProduct? = null
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
    ): View? {
        val view = inflater.inflate(layout.fragment_plans, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mTitleView?.text = getString(string.plans)
        setEmailStatus(isEmailAdded, isEmailConfirmed)
        showPlans()
        setTermAndPolicyText()
        mPlanRadioGroup?.setOnCheckedChangeListener(this)
        if (mPlanRadioGroup?.size!! > 0) {
            val firstItem = mPlanRadioGroup?.get(0) as RadioButton
            mPlanRadioGroup?.check(firstItem.id)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPlans() {
        mWindscribeInAppProduct?.let { plan ->
            if (plan.isPromo()) {
                mPlanRadioGroup?.visibility = View.GONE
                mPromoPlan?.visibility = View.VISIBLE
                mPromoSticker?.visibility = View.VISIBLE
                val firstPlan = plan.getSkus().first()
                val planText: Spannable =
                    SpannableString("${plan.getPrice(firstPlan)}/${plan.getPlanDuration(firstPlan)}")
                mPromoPlan?.append(planText)
                val divider: Spannable = SpannableString("  |  ")
                context?.let {
                    divider.setSpan(
                        ForegroundColorSpan(it.resources.getColor(R.color.colorWhite15)),
                        0,
                        5,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                mPromoPlan?.append(divider)
                val discountText: Spannable = SpannableString(plan.getDiscountLabel(firstPlan))
                mPromoPlan?.append(discountText)
                mPromoSticker?.text = plan.getPromoStickerLabel(firstPlan)
                mContinueToPremiumButton?.setTag(R.id.sku_tag, firstPlan)
                mContinueToPremiumButton?.isEnabled = true
                mContinueToPremiumButton?.text =
                    "Continue ${plan.getPrice(firstPlan)}/${plan.getPlanDuration(firstPlan)}"
            } else {
                mPlanRadioGroup?.visibility = View.VISIBLE
                mPromoPlan?.visibility = View.GONE
                mPromoSticker?.visibility = View.GONE
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
                    mPlanRadioGroup?.addView(radioButton, params)
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
        this.mWindscribeInAppProduct = products
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
        mBillingListener?.onTenGbFreeClick()
    }

    @OnClick(R.id.nav_button)
    fun onBackPressed() {
        requireActivity().onBackPressed()
    }

    @SuppressLint("SetTextI18n")
    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        val viewTag = view?.findViewById<RadioButton>(checkedId)?.getTag(R.id.sku_tag) as String?
        viewTag?.let {
            val planDuration = mWindscribeInAppProduct?.getPlanDuration(viewTag)
            val planPrice = mWindscribeInAppProduct?.getPrice(viewTag)
            planDuration.let {
                planPrice.let {
                    mContinueToPremiumButton?.isEnabled = true
                    mContinueToPremiumButton?.text = "Continue $planPrice/$planDuration"
                    mContinueToPremiumButton?.setTag(R.id.sku_tag, viewTag)
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
        val viewTag = mContinueToPremiumButton?.getTag(R.id.sku_tag) as String?
        viewTag?.let {
            when (mWindscribeInAppProduct) {
                is GoogleProducts -> {
                    val skuDetails = (mWindscribeInAppProduct as GoogleProducts).getSkuDetails(viewTag)
                    skuDetails.subscriptionPeriod
                    mBillingListener?.onContinuePlanClick(skuDetails)
                }
                is AmazonProducts -> {
                    val product = (mWindscribeInAppProduct as AmazonProducts).getProduct(viewTag)
                    mBillingListener?.onContinuePlanClick(product)
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

    @OnClick(R.id.terms_policy)
    fun onTermPolicyClick() {
        mBillingListener?.onTermPolicyClick()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.thirdInfoIcon)
    fun onThirdInfoIconClick() {
        showDialog(getString(string.ad_malware_blocker))
    }

    fun setEmailStatus(isEmailAdded: Boolean, isEmailConfirmed: Boolean) {
        mContinueToFreeButton?.visibility = if (isEmailAdded && isEmailConfirmed) View.GONE else View.VISIBLE
    }

    private fun setTermAndPolicyText() {
        val appName = getString(string.app_name)
        val termAndPolicyText = getString(string.terms_policy)
        val fullText = "$appName $termAndPolicyText"
        val spannable: Spannable = SpannableString(fullText)
        val spanStart = fullText.length - termAndPolicyText.length
        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE), spanStart, fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mTermAndPolicyView?.setText(spannable, SPANNABLE)
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
