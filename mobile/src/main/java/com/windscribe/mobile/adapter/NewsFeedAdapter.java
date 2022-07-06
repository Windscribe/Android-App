/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;


import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.newsfeedactivity.NewsFeedListener;
import com.windscribe.vpn.constants.AnimConstants;
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction;
import com.windscribe.vpn.localdatabase.tables.WindNotification;

import java.util.List;

public class NewsFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private class NewsFeedViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView btnAction;

        final ConstraintLayout clBodyLayout;

        final ImageView imgCloseIcon;

        final ImageView imgReadIcon;

        final TextView tvBody;

        final TextView tvTitle;

        WindNotification windNotification;

        public NewsFeedViewHolder(View itemView) {
            super(itemView);
            clBodyLayout = itemView.findViewById(R.id.cl_notification_body);
            tvTitle = itemView.findViewById(R.id.tv_welcome_title);
            tvBody = itemView.findViewById(R.id.tv_body_message);
            imgCloseIcon = itemView.findViewById(R.id.img_close_btn);
            imgReadIcon = itemView.findViewById(R.id.img_read_icon);
            tvBody.setMovementMethod(LinkMovementMethod.getInstance());
            btnAction = itemView.findViewById(R.id.action_label);

            tvTitle.setOnClickListener(this);
            clBodyLayout.setOnClickListener(this);
            imgCloseIcon.setOnClickListener(this);
            btnAction.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cl_notification_body:
                    break;
                case R.id.tv_welcome_title:
                case R.id.img_close_btn:
                    imgReadIcon.setVisibility(View.INVISIBLE);
                    onClickAnimation(clBodyLayout, imgCloseIcon, windNotification, tvTitle);
                    break;
                case R.id.action_label:
                    newsFeedListener.onNotificationActionClick(windNotification);
                    break;
            }

        }

        void bind(WindNotification windNotification) {
            this.windNotification = windNotification;
        }
    }

    private final int firstItemToOpen;

    private final List<WindNotification> mNotificationList;

    private final NewsFeedListener newsFeedListener;

    public NewsFeedAdapter(List<WindNotification> mNotificationList, int firstItemToOpen,
            NewsFeedListener newsFeedListener) {
        this.mNotificationList = mNotificationList;
        this.newsFeedListener = newsFeedListener;
        this.firstItemToOpen = firstItemToOpen;
    }

    @Override
    public int getItemCount() {
        return mNotificationList != null ? mNotificationList.size() : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final NewsFeedViewHolder newsFeedViewHolder = (NewsFeedViewHolder) holder;
        //The first position will always be visible
        WindNotification windNotification = mNotificationList.get(holder.getAdapterPosition());
        newsFeedViewHolder.bind(windNotification);
        if (mNotificationList.get(holder.getAdapterPosition()).isRead()) {
            newsFeedViewHolder.imgReadIcon.setVisibility(View.INVISIBLE);
        } else {
            newsFeedViewHolder.imgReadIcon.setVisibility(View.VISIBLE);
        }
        if (windNotification.getNotificationId() == firstItemToOpen) {
            newsFeedViewHolder.clBodyLayout.setVisibility(View.VISIBLE);
            newsFeedViewHolder.imgCloseIcon.setImageResource(R.drawable.ic_close_white);
            newsFeedViewHolder.imgCloseIcon.setTag(1);
            newsFeedViewHolder.imgReadIcon.setVisibility(View.INVISIBLE);
            newsFeedListener.onNotificationExpand(windNotification);
        } else {
            newsFeedViewHolder.imgCloseIcon.setImageResource(R.drawable.ic_close_white_25_alpha);
            newsFeedViewHolder.imgCloseIcon.setTag(0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ((NewsFeedViewHolder) holder).tvBody.setText(
                    Html.fromHtml(mNotificationList.get(holder.getAdapterPosition()).getNotificationMessage(),
                            Html.FROM_HTML_MODE_LEGACY));
            ((NewsFeedViewHolder) holder).tvTitle.setText(Html.fromHtml(
                    mNotificationList.get(holder.getAdapterPosition()).getNotificationTitle().toUpperCase(),
                    Html.FROM_HTML_MODE_LEGACY));

        } else {
            ((NewsFeedViewHolder) holder).tvBody.setText(
                    Html.fromHtml(mNotificationList.get(holder.getAdapterPosition()).getNotificationMessage()));
            ((NewsFeedViewHolder) holder).tvTitle.setText(Html.fromHtml(
                    mNotificationList.get(holder.getAdapterPosition()).getNotificationTitle().toUpperCase()));

        }
        NewsfeedAction newsfeedAction = windNotification.getAction();
        if (newsfeedAction != null) {
            newsFeedViewHolder.btnAction.setVisibility(View.VISIBLE);
            newsFeedViewHolder.btnAction.setText(newsfeedAction.getLabel());
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NewsFeedViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.news_feed_view, parent, false));
    }

    private void onClickAnimation(final View clBodyLayout, final View imgCloseIcon,
            WindNotification windNotification, final TextView tvTitle) {
        if (clBodyLayout.getVisibility() == View.VISIBLE) {
            tvTitle.animate().alpha(0.5f).setDuration(250);
            clBodyLayout.animate().alpha(0).setDuration(250).withStartAction(
                    () -> rotateImageAnimation(imgCloseIcon, -45, true)).withEndAction(
                    () -> clBodyLayout.setVisibility(View.GONE));

        } else {
            newsFeedListener.onNotificationExpand(windNotification);
            tvTitle.animate().alpha(0);
            tvTitle.animate().alpha(1).setDuration(250);
            clBodyLayout.setAlpha(0);
            clBodyLayout.setVisibility(View.VISIBLE);
            clBodyLayout.animate().alpha(1).setDuration(250).withStartAction(
                    () -> rotateImageAnimation(imgCloseIcon, 45, false)).start();
        }
    }

    private void rotateImageAnimation(final View viewToRotate, float toDegrees, final boolean collapse) {
        final RotateAnimation rotateAnimation = new RotateAnimation(0, toDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(AnimConstants.RECYCLER_VIEW_UNDERLINE_ANIMATION_DURATION);
        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if (collapse) {
                    viewToRotate.clearAnimation();
                    ((ImageView) viewToRotate).setImageResource(R.drawable.ic_close_white_25_alpha);
                } else {
                    viewToRotate.clearAnimation();
                    ((ImageView) viewToRotate).setImageResource(R.drawable.ic_close_white);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        viewToRotate.setAnimation(rotateAnimation);
    }
}
