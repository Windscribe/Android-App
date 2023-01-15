/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.holder;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;
import com.windscribe.mobile.R;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.constants.AnimConstants;

public class RegionViewHolder extends GroupViewHolder {

    public interface ItemExpandListener {

        void onItemExpand();
    }

    public final ImageView imgAnimationLine;

    public final ImageView imgCountryFlag;

    public ImageView imgDropDown;

    public final ImageView imgProBadge;

    public final LinearProgressIndicator serverLoadBar;

    public final TextView tvCountryName;

    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    private ItemExpandListener itemExpandListener;

    public RegionViewHolder(View itemView) {
        super(itemView);
        imgDropDown = itemView.findViewById(R.id.img_drop_down);
        tvCountryName = itemView.findViewById(R.id.country_name);
        imgCountryFlag = itemView.findViewById(R.id.country_flag);
        imgDropDown = itemView.findViewById(R.id.img_drop_down);
        imgAnimationLine = itemView.findViewById(R.id.field_line_location);
        imgProBadge = itemView.findViewById(R.id.img_pro_badge);
        serverLoadBar = itemView.findViewById(R.id.server_health);
    }

    @Override
    public void collapse() {
        super.collapse();
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.addUpdateListener(valueAnimator1 -> tvCountryName.setTextColor((Integer) argbEvaluator
                .evaluate(valueAnimator1.getAnimatedFraction(), ThemeUtils
                                .getColor(itemView.getContext(), R.attr.nodeListGroupTextColorSelected,
                                        R.color.colorWhite),
                        ThemeUtils.getColor(itemView.getContext(), R.attr.nodeListGroupTextColor,
                                R.color.colorWhite))));
        valueAnimator.setDuration(AnimConstants.RECYCLER_VIEW_UNDERLINE_ANIMATION_DURATION);
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
                valueAnimator.removeAllListeners();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                valueAnimator.removeAllListeners();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });
        valueAnimator.start();
        scaleLineAnimation(itemView.findViewById(R.id.field_line_location), 1, 0, View.GONE);
        rotateImageAnimation(itemView.findViewById(R.id.img_drop_down), -45, true);
    }

    @Override
    public void expand() {
        super.expand();
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.addUpdateListener(valueAnimator1 -> tvCountryName.setTextColor((Integer) argbEvaluator
                .evaluate(valueAnimator1.getAnimatedFraction(), ThemeUtils
                                .getColor(itemView.getContext(), R.attr.nodeListGroupTextColor, R.color.colorWhite),
                        ThemeUtils.getColor(itemView.getContext(), R.attr.nodeListGroupTextColorSelected,
                                R.color.colorWhite))));
        valueAnimator.setDuration(AnimConstants.RECYCLER_VIEW_UNDERLINE_ANIMATION_DURATION);
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
                valueAnimator.removeAllListeners();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                valueAnimator.removeAllListeners();
                if (itemExpandListener != null) {
                    itemExpandListener.onItemExpand();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationStart(Animator animation) {

            }
        });
        valueAnimator.start();

        scaleLineAnimation(itemView.findViewById(R.id.field_line_location), 0, 1, View.VISIBLE);
        rotateImageAnimation(itemView.findViewById(R.id.img_drop_down), 45, false);

    }

    public void setGroupName(String name) {
        tvCountryName.setText(name);

    }

    public void setItemExpandListener(ItemExpandListener itemExpandListener) {
        this.itemExpandListener = itemExpandListener;
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
                    imgDropDown.setImageResource(ThemeUtils
                            .getResourceId(itemView.getContext(), R.attr.close_list_icon,
                                    R.drawable.ic_location_dropdown_collapse));
                } else {
                    imgDropDown.setImageResource(ThemeUtils
                            .getResourceId(itemView.getContext(), R.attr.expand_list_icon,
                                    R.drawable.ic_location_drop_down_expansion));
                    viewToRotate.clearAnimation();
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

    private void scaleLineAnimation(final View viewToScale, float fromScale,
            float toScale, final int visible) {
        final ScaleAnimation scaleAnimation = new ScaleAnimation(fromScale, toScale, 1, 1,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setDuration(AnimConstants.RECYCLER_VIEW_UNDERLINE_ANIMATION_DURATION);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if (visible == View.GONE) {
                    viewToScale.setVisibility(visible);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {
                if (visible == View.VISIBLE) {
                    viewToScale.setVisibility(visible);
                }
            }
        });
        viewToScale.setAnimation(scaleAnimation);
    }
}