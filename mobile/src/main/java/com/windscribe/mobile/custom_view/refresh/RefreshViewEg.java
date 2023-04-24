/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view.refresh;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

import com.windscribe.mobile.R;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.commonutils.WindUtilities;


public class RefreshViewEg extends AppCompatImageView implements IRefreshStatus {

    int currentArrayIndex = 0;

    boolean isRefreshing = false;

    final Paint paint = new Paint();

    final Paint textPaint = new Paint();

    private int radius;

    public RefreshViewEg(Context context) {
        this(context, null);
    }

    public RefreshViewEg(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        initAnimation();
    }

    @Override
    public void pullProgress(float pullDistance, float pullProgress) {
        currentArrayIndex = getProgress(pullDistance);
        postInvalidate();
    }

    @Override
    public void pullToRefresh() {
        clearAnimation();
    }

    @Override
    public void refreshComplete() {
        animate().cancel();
        currentArrayIndex = 0;
        isRefreshing = false;
        this.setImageDrawable(null);
    }

    @Override
    public void refreshing() {
        animate().cancel();
        isRefreshing = true;
        setImageDrawable(null);
        boolean vpnDisconnected = !Windscribe.getAppContext().vpnConnectionStateManager.isVPNActive();
        boolean networkAvailable = WindUtilities.isOnline();
        if (vpnDisconnected && networkAvailable) {
            startAnimation();
        }
    }

    @Override
    public void reset() {
        currentArrayIndex = 0;
        isRefreshing = false;
        animate().cancel();
        this.setImageDrawable(null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean vpnDisconnected = !Windscribe.getAppContext().getVpnConnectionStateManager().isVPNActive();
        boolean networkAvailable = WindUtilities.isOnline();
        if (networkAvailable && vpnDisconnected) {
            drawCircles(canvas);
        }
        super.onDraw(canvas);
    }

    private void drawCircles(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        final RectF rect = new RectF();
        rect.set((float) width / 2 - radius, (float) height / 2 - radius, (float) width / 2 + radius,
                (float) height / 2 + radius);
        for (int i = 0; i < currentArrayIndex; i++) {
            int textColor = ThemeUtils.getColor(getContext(), R.attr.nodeListGroupTextColor, R.color.colorWhite40);
            paint.setColor(textColor);
            int secLength = 50;
            double secRot = Math.PI / 6 * (i - 3);
            float secX = (float) Math.sin(secRot) * secLength;
            float secY = (float) -Math.cos(secRot) * secLength;
            float startX = (float) Math.sin(secRot) * 35;
            float startY = (float) -Math.cos(secRot) * 35;
            canvas.drawLine(rect.centerX() + startX, rect.centerY() + startY, rect.centerX() + secX,
                    rect.centerY() + secY, paint);
        }

    }

    private int getProgress(float distance) {
        float totalDistance = 200;
        if (distance <= 0) {
            return 0;
        }
        if (distance >= totalDistance | currentArrayIndex > 12) {
            return 12;
        }
        float percent = distance / totalDistance * 12;
        return Math.round(percent);
    }

    private void initAnimation() {
        Animation mRotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setDuration(10000);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);

    }

    private void initView() {
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        this.setScaleType(ScaleType.CENTER);
        radius = Math.round(getResources().getDimension(R.dimen.reg_24dp));

        int stroke = Math.round(getResources().getDimension(R.dimen.reg_2dp));
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(stroke);
        int textColor = ThemeUtils.getColor(getContext(), R.attr.nodeListGroupTextColor, R.color.colorWhite40);

        Typeface textTypeFace = ResourcesCompat.getFont(getContext(), R.font.ibm_plex_sans_bold);
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTypeface(textTypeFace);
        float textSize = getResources().getDimension(R.dimen.text_size_14);
        textPaint.setTextSize(textSize);
    }

    private void startAnimation() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (isRefreshing) {
                    animate().rotationBy(360).withEndAction(this).setDuration(1000)
                            .setInterpolator(new LinearInterpolator()).start();
                }
            }
        };

        animate().rotationBy(360).withEndAction(runnable).setDuration(1000).setInterpolator(new LinearInterpolator())
                .start();
    }


}
