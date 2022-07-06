/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view;

import android.os.Bundle;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.windscribe.mobile.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgressFragment extends Fragment {

    @BindView(R.id.progressLabel)
    TextView mProgressLabel;

    private String progressText = "";

    public static ProgressFragment getInstance() {
        return new ProgressFragment();
    }

    public ProgressFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateProgressStatus(progressText);
    }

    @Override
    public void onDestroyView() {
        progressText = "";
        super.onDestroyView();
    }

    public void add(AppCompatActivity activity, int container, boolean addToBackStack) {
        setEnterTransition(new Fade().addTarget(R.id.progress_fragment_container));
        setExitTransition(new Fade().addTarget(R.id.progress_fragment_container));
        FragmentTransaction transaction = activity.getSupportFragmentManager()
                .beginTransaction()
                .add(container, this);
        if (addToBackStack) {
            transaction.addToBackStack(this.getClass().getName());
        }
        transaction.commit();
    }

    public void add(String progressText, AppCompatActivity activity, int container, boolean addToBackStack) {
        this.progressText = progressText;
        add(activity, container, addToBackStack);
    }

    public void finishProgress() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    public void updateProgressStatus(String call) {
        if (mProgressLabel != null) {
            progressText = call;
            mProgressLabel.setText(progressText);
        }
    }
}