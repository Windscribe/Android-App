/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view;

import android.os.Bundle;
import android.transition.Slide;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.windscribe.mobile.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SuccessFragment extends Fragment {

    @BindView(R.id.message)
    TextView messageView;

    private @ColorInt
    int backgroundColor = -1;

    private String message;

    public static SuccessFragment getInstance() {
        return new SuccessFragment();
    }

    public SuccessFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getString("message");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_success, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (backgroundColor != -1) {
            view.setBackgroundColor(backgroundColor);
        }
        messageView.setText(message);
    }

    public void add(String message, AppCompatActivity activity, int container, boolean addToBackStack,
            @ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        add(message, activity, container, addToBackStack);
    }

    public void add(String message, AppCompatActivity activity, int container, boolean addToBackStack) {
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        setArguments(bundle);
        setEnterTransition(new Slide(Gravity.BOTTOM).addTarget(R.id.success_fragment_container));
        FragmentTransaction transaction = activity.getSupportFragmentManager()
                .beginTransaction()
                .add(container, this);
        if (addToBackStack) {
            transaction.addToBackStack(this.getClass().getName());
        }
        transaction.commit();
    }

    @OnClick(R.id.close_btn)
    public void onCloseButtonClick() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}