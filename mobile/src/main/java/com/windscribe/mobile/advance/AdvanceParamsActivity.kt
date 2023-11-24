package com.windscribe.mobile.advance

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.test.espresso.action.EditorAction
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.di.DaggerActivityComponent
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.commonutils.WindUtilities
import javax.inject.Inject

class AdvanceParamsActivity : BaseActivity(), AdvanceParamView {

    @Inject
    lateinit var presenter: AdvanceParamPresenter

    @BindView(R.id.nav_title)
    lateinit var titleView: TextView

    @BindView(R.id.advance_params_text)
    lateinit var advanceParamsText: AppCompatEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_advance_params, true)
        presenter.setup()
    }

    override fun setupActivityTitle() {
        titleView.text = getString(R.string.advance)
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClick(){
        super.onBackPressed()
    }

    @OnClick(R.id.saveAdvanceParams)
    fun onSavedAdvanceParamsClick(){
        advanceParamsText.onEditorAction(EditorInfo.IME_ACTION_DONE)
        presenter.saveAdvanceParams(advanceParamsText.text.toString())
    }

    @OnClick(R.id.clearAdvanceParams)
    fun onClearAdvanceParamsClick(){
        advanceParamsText.onEditorAction(EditorInfo.IME_ACTION_DONE)
        presenter.clearAdvanceParams()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun setAdvanceParamsText(text: String) {
        advanceParamsText.setText(text)
    }
}