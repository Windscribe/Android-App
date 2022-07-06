/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.NetworkListAdapter;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.CustomDialog;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.di.DaggerActivityComponent;
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailsActivity;
import com.windscribe.mobile.networksecurity.viewholder.NetworkAdapterActionListener;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.backend.utils.WindVpnController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NetworkSecurityActivity extends BaseActivity implements NetworkSecurityView,
        NetworkAdapterActionListener {

    @BindView(R.id.nav_title)
    TextView mActivityTitleView;

    @Inject
    CustomDialog mCustomProgress;

    @BindView(R.id.recycler_view_network_list)
    RecyclerView mNetworkListRecyclerView;

    @Inject
    NetworkSecurityPresenter mNetworkPresenter;

    @Inject
    WindVpnController mWindVpnController;

    @BindView(R.id.tv_no_network_list)
    TextView tvNoNetworkFound;

    private final String TAG = "net_security_a";

    private final Logger mActivityLog = LoggerFactory.getLogger(TAG);

    public static Intent getStartIntent(Context context) {
        return new Intent(context, NetworkSecurityActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_network_security,true);
        mNetworkListRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        mActivityTitleView.setText(getString(R.string.network_options));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNetworkPresenter.setupNetworkListAdapter();
    }

    @Override
    protected void onDestroy() {
        mNetworkPresenter.onDestroy();
        super.onDestroy();
    }


    @Override
    public void hideProgress() {
        mActivityLog.info("Dismissing progress dialog...");
        mCustomProgress.dismiss();
    }


    @Override
    public void onAdapterLoadFailed(String showUpdate) {
        tvNoNetworkFound.setVisibility(View.VISIBLE);
        tvNoNetworkFound.setText(showUpdate);
    }

    @OnClick(R.id.nav_button)
    public void onBackArrowClicked() {
        mActivityLog.info("User clicked back arrow...");
        onBackPressed();
    }

    @Override
    public void onItemSelected(com.windscribe.vpn.localdatabase.tables.NetworkInfo networkInfo) {
        mActivityLog.info("User selected " + networkInfo.getNetworkName());
        mNetworkPresenter.onNetworkSecuritySelected(networkInfo);
    }

    @Override
    public void openNetworkSecurityDetails(String networkName) {
        Intent intent = NetworkDetailsActivity.getStartIntent(this, networkName);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivity(intent, options.toBundle());
    }

    @Override
    public void setAdapter(List<com.windscribe.vpn.localdatabase.tables.NetworkInfo> mNetworkList) {
        NetworkListAdapter mAdapter = new NetworkListAdapter(mNetworkList);
        mNetworkListRecyclerView.setAdapter(mAdapter);
        mNetworkListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter.setAdapterActionListener(this);
        mNetworkPresenter.onAdapterSet();
    }

    @Override
    public void showProgress(String progressTitle) {
        mActivityLog.info("Showing loading dialog...");
        mCustomProgress.show();
        ((TextView) mCustomProgress.findViewById(R.id.tv_dialog_header)).setText(progressTitle);
    }
}
