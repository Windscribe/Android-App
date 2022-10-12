/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome;

import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class SoftInputAssist {

    private final Rect contentAreaOfWindowBounds = new Rect();

    private ViewGroup contentContainer;

    private final int[] notImportantViews;

    private View rootView;

    private final FrameLayout.LayoutParams rootViewLayout;

    private int usableHeightPrevious = 0;

    private final ViewTreeObserver.OnGlobalLayoutListener listener = this::possiblyResizeChildOfContent;

    private ViewTreeObserver viewTreeObserver;

    public SoftInputAssist(Activity activity, int[] notImportantViews) {
        contentContainer = activity.findViewById(android.R.id.content);
        rootView = contentContainer.getChildAt(0);
        rootViewLayout = (FrameLayout.LayoutParams) rootView.getLayoutParams();
        this.notImportantViews = notImportantViews;
    }

    public boolean isLargeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public void onDestroy() {
        rootView = null;
        contentContainer = null;
        viewTreeObserver = null;
    }

    public void onPause() {
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnGlobalLayoutListener(listener);
        }
    }

    public void onResume() {
        if (viewTreeObserver == null || !viewTreeObserver.isAlive()) {
            viewTreeObserver = rootView.getViewTreeObserver();
        }

        viewTreeObserver.addOnGlobalLayoutListener(listener);
    }

    private void possiblyResizeChildOfContent() {
        if (contentContainer == null) return;
        contentContainer.getWindowVisibleDisplayFrame(contentAreaOfWindowBounds);
        int usableHeightNow = contentAreaOfWindowBounds.bottom;
        if (usableHeightNow != usableHeightPrevious) {
            boolean hideViews = usableHeightNow < usableHeightPrevious && usableHeightPrevious != 0 && !isLargeScreen(
                    contentContainer.getContext());
            for (int viewId : notImportantViews) {
                View view = contentContainer.findViewById(viewId);
                if (view != null) {
                    view.setVisibility(hideViews ? View.GONE : VISIBLE);
                }
            }
            rootViewLayout.height = usableHeightNow;
            rootView.requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }
}
