/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view.refresh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The RecyclerRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The RecyclerRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * Maybe you need a custom refresh components, can be implemented by call
 * the function {@link #setRefreshView(View, ViewGroup.LayoutParams)}
 * </p>
 */
public class RecyclerRefreshLayout extends ViewGroup
        implements NestedScrollingParent, NestedScrollingChild {

    /**
     * Per-child layout information for layouts that support margins.
     */
    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public interface OnRefreshListener {

        void onRefresh();
    }

    public enum RefreshStyle {
        NORMAL,
        PINNED,
        FLOAT
    }

    private static final int INVALID_INDEX = -1;

    private static final int INVALID_POINTER = -1;

    //the default height of the RefreshView
    private static final int DEFAULT_REFRESH_SIZE_DP = 30;

    //the animation duration of the RefreshView scroll to the refresh point or the start point
    private static final int DEFAULT_ANIMATE_DURATION = 300;

    // the threshold of the trigger to refresh
    private static final int DEFAULT_REFRESH_TARGET_OFFSET_DP = 50;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2.0f;

    private int mActivePointerId = INVALID_POINTER;

    private final Interpolator mAnimateToRefreshInterpolator
            = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

    private final Interpolator mAnimateToStartInterpolator
            = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

    private float mCurrentTouchOffsetY;

    private boolean mDispatchTargetTouchDown;

    private IDragDistanceConverter mDragDistanceConverter;

    private int mFrom;

    private float mInitialDownY;

    private float mInitialMotionY;

    private float mInitialScrollY;

    //whether to remind the callback listener(OnRefreshListener)
    private boolean mIsAnimatingToStart;

    private boolean mIsBeingDragged;

    private boolean mIsFitRefresh;

    private boolean mIsRefreshing;

    private boolean mNestedScrollInProgress;

    private final NestedScrollingChildHelper mNestedScrollingChildHelper;

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;

    private boolean mNotifyListener;

    private OnRefreshListener mOnRefreshListener;

    private final int[] mParentOffsetInWindow = new int[2];

    private final int[] mParentScrollConsumed = new int[2];

    private float mRefreshInitialOffset;

    private IRefreshStatus mRefreshStatus;

    private final RefreshStyle mRefreshStyle = RefreshStyle.NORMAL;

    private float mRefreshTargetOffset;

    private View mRefreshView;

    private int mRefreshViewIndex = INVALID_INDEX;

    // Whether or not the RefreshView has been measured.
    private boolean mRefreshViewMeasured = false;

    private final int mRefreshViewSize;

    private final Animation.AnimationListener mRefreshingListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            if (mNotifyListener) {
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }
            }

            mIsAnimatingToStart = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimatingToStart = true;
            mRefreshStatus.refreshing();
        }
    };

    private View mTarget;

    private float mTargetOrRefreshViewOffsetY;

    private final Animation mAnimateToRefreshingAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            animateToTargetOffset(mRefreshTargetOffset, mTarget.getTop(), interpolatedTime);
        }
    };

    private final Animation mAnimateToStartAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            animateToTargetOffset(0.0f, mTarget.getTop(), interpolatedTime);
        }
    };

    private final Animation.AnimationListener mResetListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            reset();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimatingToStart = true;
            mRefreshStatus.refreshComplete();
        }
    };

    // NestedScroll
    private float mTotalUnconsumed;

    private final int mTouchSlop;

    // Whether the client has set a custom starting position;
    private boolean mUsingCustomRefreshInitialOffset = false;

    public RecyclerRefreshLayout(Context context) {
        this(context, null);
    }

    public RecyclerRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mRefreshViewSize = (int) (DEFAULT_REFRESH_SIZE_DP * metrics.density);

        mRefreshTargetOffset = DEFAULT_REFRESH_TARGET_OFFSET_DP * metrics.density;

        mTargetOrRefreshViewOffsetY = 0.0f;
        mRefreshInitialOffset = 0.0f;

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

        initRefreshView();
        initDragDistanceConverter();
        setNestedScrollingEnabled(true);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // support compile sdk version < 23
                onStopNestedScroll(this);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();
        if (mTarget == null) {
            return false;
        }

        if (mRefreshStyle == RefreshStyle.FLOAT) {
            if (!isEnabled() || canChildScrollUp(mTarget)
                    || mIsRefreshing || mNestedScrollInProgress) {
                // Fail fast if we're not in a state where a swipe is possible
                return false;
            }
        } else {
            if ((!isEnabled() || (canChildScrollUp(mTarget) && !mDispatchTargetTouchDown))) {
                return false;
            }
        }

        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;

                float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }

                // Animation.AnimationListener.onAnimationEnd() can't be ensured to be called
                if (mAnimateToRefreshingAnimation.hasEnded() && mAnimateToStartAnimation.hasEnded()) {
                    mIsAnimatingToStart = false;
                }

                mInitialDownY = initialDownY;
                mInitialScrollY = mTargetOrRefreshViewOffsetY;
                mDispatchTargetTouchDown = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                float activeMoveY = getMotionEventY(ev, mActivePointerId);
                if (activeMoveY == -1) {
                    return false;
                }

                initDragStatus(activeMoveY);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
            default:
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ensureTarget();
        if (mTarget == null) {
            return;
        }

        measureTarget();
        measureRefreshView(widthMeasureSpec, heightMeasureSpec);

        if (!mRefreshViewMeasured && !mUsingCustomRefreshInitialOffset) {
            switch (mRefreshStyle) {
                case PINNED:
                    mTargetOrRefreshViewOffsetY = mRefreshInitialOffset = 0.0f;
                    break;
                case FLOAT:
                    mTargetOrRefreshViewOffsetY = mRefreshInitialOffset = -mRefreshView.getMeasuredHeight();
                    break;
                default:
                    mTargetOrRefreshViewOffsetY = 0.0f;
                    mRefreshInitialOffset = -mRefreshView.getMeasuredHeight();
                    break;
            }
        }
        if (!mRefreshViewMeasured) {
            if (mRefreshTargetOffset < mRefreshView.getMeasuredHeight()) {
                mRefreshTargetOffset = mRefreshView.getMeasuredHeight();
            }
        }

        mRefreshViewMeasured = true;

        mRefreshViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mRefreshView) {
                mRefreshViewIndex = index;
                break;
            }
        }

    }

    @Override
    public boolean onNestedFling(@NotNull View target, float velocityX, float velocityY,
            boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(@NotNull View target, float velocityX,
            float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public void onNestedPreScroll(@NotNull View target, int dx, int dy, @NotNull int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;

            }
            RefreshLogger.i("pre scroll");
            moveSpinner(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(@NotNull final View target, final int dxConsumed, final int dyConsumed,
            final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0) {
            mTotalUnconsumed += Math.abs(dy);
            RefreshLogger.i("nested scroll");
            moveSpinner(mTotalUnconsumed);
        }
    }

    @Override
    public void onNestedScrollAccepted(@NotNull View child, @NotNull View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    // NestedScrollingParent
    @Override
    public boolean onStartNestedScroll(@NotNull View child, @NotNull View target, int nestedScrollAxes) {
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            return isEnabled() && canChildScrollUp(mTarget) && !mIsRefreshing
                    && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        }
        return isEnabled() && canChildScrollUp(mTarget)
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onStopNestedScroll(@NotNull View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishSpinner();
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    // NestedScrollingChild

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        ensureTarget();
        if (mTarget == null) {
            return false;
        }

        if (mRefreshStyle == RefreshStyle.FLOAT) {
            if (!isEnabled() || canChildScrollUp(mTarget) || mNestedScrollInProgress) {
                // Fail fast if we're not in a state where a swipe is possible
                return false;
            }
        } else {
            if ((!isEnabled() || (canChildScrollUp(mTarget) && !mDispatchTargetTouchDown))) {
                return false;
            }
        }

        if (mRefreshStyle == RefreshStyle.FLOAT && (canChildScrollUp(mTarget) || mNestedScrollInProgress)) {
            return false;
        }

        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final float activeMoveY = getMotionEventY(ev, mActivePointerId);
                if (activeMoveY == -1) {
                    return false;
                }

                float overScrollY;
                if (mIsAnimatingToStart) {
                    overScrollY = getTargetOrRefreshViewTop();

                    mInitialMotionY = activeMoveY;
                    mInitialScrollY = overScrollY;

                    RefreshLogger.i("animatetostart overscrolly " + overScrollY + " -- " + mInitialMotionY);
                } else {
                    overScrollY = activeMoveY - mInitialMotionY + mInitialScrollY;
                    RefreshLogger
                            .i("overscrolly " + overScrollY + " --" + mInitialMotionY + " -- " + mInitialScrollY);
                }

                if (mIsRefreshing) {
                    //note: float style will not come here
                    if (overScrollY <= 0) {
                        if (mDispatchTargetTouchDown) {
                            mTarget.dispatchTouchEvent(ev);
                        } else {
                            MotionEvent obtain = MotionEvent.obtain(ev);
                            obtain.setAction(MotionEvent.ACTION_DOWN);
                            mDispatchTargetTouchDown = true;
                            mTarget.dispatchTouchEvent(obtain);
                        }
                    } else if (overScrollY > 0 && overScrollY < mRefreshTargetOffset) {
                        if (mDispatchTargetTouchDown) {
                            MotionEvent obtain = MotionEvent.obtain(ev);
                            obtain.setAction(MotionEvent.ACTION_CANCEL);
                            mDispatchTargetTouchDown = false;
                            mTarget.dispatchTouchEvent(obtain);
                        }
                    }
                    RefreshLogger.i("moveSpinner refreshing -- " + mInitialScrollY + " -- " + (activeMoveY
                            - mInitialMotionY));
                    moveSpinner(overScrollY);
                } else {
                    if (mIsBeingDragged) {
                        if (overScrollY > 0) {
                            moveSpinner(overScrollY);
                        } else {
                            return false;
                        }
                    } else {
                        initDragStatus(activeMoveY);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                onNewerPointerDown(ev);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER
                        || getMotionEventY(ev, mActivePointerId) == -1) {
                    resetTouchEvent();
                    return false;
                }

                if (mIsRefreshing || mIsAnimatingToStart) {
                    if (mDispatchTargetTouchDown) {
                        mTarget.dispatchTouchEvent(ev);
                    }
                    resetTouchEvent();
                    return false;
                }

                resetTouchEvent();
                finishSpinner();
                return false;
            }
            default:
                break;
        }

        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if (mTarget == null || ViewCompat.isNestedScrollingEnabled(mTarget)) {
            super.requestDisallowInterceptTouchEvent(b);
        }

    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    /**
     * @param refreshInitialOffset the top position of the {@link #mRefreshView} relative to its parent.
     */
    public void setRefreshInitialOffset(float refreshInitialOffset) {
        mRefreshInitialOffset = refreshInitialOffset;
        mUsingCustomRefreshInitialOffset = true;
        requestLayout();
    }

    /**
     * @param refreshView  must implements the interface IRefreshStatus
     * @param layoutParams the with is always the match_parent， no matter how you set
     *                     the height you need to set a specific value
     */
    public void setRefreshView(@NonNull View refreshView, ViewGroup.LayoutParams layoutParams) {
        if (mRefreshView == refreshView) {
            return;
        }

        if (mRefreshView != null && mRefreshView.getParent() != null) {
            ((ViewGroup) mRefreshView.getParent()).removeView(mRefreshView);
        }

        if (refreshView instanceof IRefreshStatus) {
            mRefreshStatus = (IRefreshStatus) refreshView;
        } else {
            throw new ClassCastException("the refreshView must implement the interface IRefreshStatus");
        }
        refreshView.setVisibility(View.GONE);
        removeView(refreshView);
        addView(refreshView, layoutParams);

        mRefreshView = refreshView;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && !mIsRefreshing) {
            mIsRefreshing = true;
            mNotifyListener = false;

            animateToRefreshingPosition((int) mTargetOrRefreshViewOffsetY, mRefreshingListener);
        } else {
            setRefreshing(refreshing, false);
        }
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            if (mRefreshViewIndex < 0) {
                return i;
            } else if (i == childCount - 1) {
                // Draw the selected child last
                return mRefreshViewIndex;
            } else if (i >= mRefreshViewIndex) {
                // Move the children after the selected child earlier one
                return i + 1;
            } else {
                // Keep the children before the selected child the same
                return i;
            }
        }
        if (mRefreshViewIndex < 0) {
            return i;
        } else if (i == 0) {
            // Draw the selected child first
            return mRefreshViewIndex;
        } else if (i <= mRefreshViewIndex) {
            // Move the children before the selected child earlier one
            return i - 1;
        } else {
            return i;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        reset();
        clearAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            return;
        }

        ensureTarget();
        if (mTarget == null) {
            return;
        }

        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        final int targetTop = reviseTargetLayoutTop(getPaddingTop());
        final int targetLeft = getPaddingLeft();
        final int targetRight = targetLeft + width - getPaddingLeft() - getPaddingRight();
        final int targetBottom = targetTop + height - getPaddingTop() - getPaddingBottom();

        try {
            mTarget.layout(targetLeft, targetTop, targetRight, targetBottom);
        } catch (Exception e) {
            RefreshLogger.e("error: ignored=" + e.toString() + " " + Arrays.toString(e.getStackTrace()));
        }

        int refreshViewLeft = (width - mRefreshView.getMeasuredWidth()) / 2;
        int refreshViewTop = reviseRefreshViewLayoutTop((int) mRefreshInitialOffset);
        int refreshViewRight = (width + mRefreshView.getMeasuredWidth()) / 2;
        int refreshViewBottom = refreshViewTop + mRefreshView.getMeasuredHeight();

        mRefreshView.layout(refreshViewLeft, refreshViewTop, refreshViewRight, refreshViewBottom);

        RefreshLogger.i("onLayout: " + left + " : " + top + " : " + right + " : " + bottom);
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        clearAnimation();

        if (computeAnimateToStartDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }

        mFrom = from;
        mAnimateToStartAnimation.reset();
        mAnimateToStartAnimation.setDuration(computeAnimateToStartDuration(from));
        mAnimateToStartAnimation.setInterpolator(mAnimateToStartInterpolator);
        if (listener != null) {
            mAnimateToStartAnimation.setAnimationListener(listener);
        }

        startAnimation(mAnimateToStartAnimation);
    }

    private void animateToRefreshingPosition(int from, Animation.AnimationListener listener) {
        clearAnimation();

        if (computeAnimateToRefreshingDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }

        mFrom = from;
        mAnimateToRefreshingAnimation.reset();
        mAnimateToRefreshingAnimation.setDuration(computeAnimateToRefreshingDuration(from));
        mAnimateToRefreshingAnimation.setInterpolator(mAnimateToRefreshInterpolator);

        if (listener != null) {
            mAnimateToRefreshingAnimation.setAnimationListener(listener);
        }

        startAnimation(mAnimateToRefreshingAnimation);
    }

    private void animateToTargetOffset(float targetEnd, float currentOffset, float interpolatedTime) {
        int targetOffset = (int) (mFrom + (targetEnd - mFrom) * interpolatedTime);

        setTargetOrRefreshViewOffsetY((int) (targetOffset - currentOffset));
    }

    private boolean canChildScrollUp(View mTarget) {
        if (mTarget == null) {
            return false;
        }

        if (mTarget instanceof ViewGroup) {
            int childCount = ((ViewGroup) mTarget).getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = ((ViewGroup) mTarget).getChildAt(i);
                if (canChildScrollUp(child)) {
                    return true;
                }
            }
        }

        return canScrollVertically(-1);
    }

    private int computeAnimateToRefreshingDuration(float from) {
        RefreshLogger.i("from -- refreshing " + from);

        if (from < mRefreshInitialOffset) {
            return 0;
        }

        final int animateToRefreshDuration = DEFAULT_ANIMATE_DURATION;
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            return (int) (Math.max(0.0f, Math.min(1.0f,
                    Math.abs(from - mRefreshInitialOffset - mRefreshTargetOffset) / mRefreshTargetOffset))
                    * animateToRefreshDuration);
        }
        return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mRefreshTargetOffset) / mRefreshTargetOffset))
                * animateToRefreshDuration);
    }

    private int computeAnimateToStartDuration(float from) {
        RefreshLogger.i("from -- start " + from);

        if (from < mRefreshInitialOffset) {
            return 0;
        }

        final int animateToStartDuration = DEFAULT_ANIMATE_DURATION;
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            return (int) (
                    Math.max(0.0f, Math.min(1.0f, Math.abs(from - mRefreshInitialOffset) / mRefreshTargetOffset))
                            * animateToStartDuration);
        }
        return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from) / mRefreshTargetOffset))
                * animateToStartDuration);
    }

    private void ensureTarget() {
        if (!isTargetValid()) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mRefreshView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    private void finishSpinner() {
        if (mIsRefreshing || mIsAnimatingToStart) {
            return;
        }

        float scrollY = getTargetOrRefreshViewOffset();
        if (scrollY > mRefreshTargetOffset) {
            setRefreshing(true, true);
        } else {
            mIsRefreshing = false;
            animateOffsetToStartPosition((int) mTargetOrRefreshViewOffsetY, mResetListener);
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
            return -1;
        }
        return ev.getY(index);
    }

    private int getTargetOrRefreshViewOffset() {
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            return (int) (mRefreshView.getTop() - mRefreshInitialOffset);
        }
        return mTarget.getTop();
    }

    private int getTargetOrRefreshViewTop() {
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            return mRefreshView.getTop();
        }
        return mTarget.getTop();
    }

    private void initDragDistanceConverter() {
        mDragDistanceConverter = new MaterialDragDistanceConverter();
    }

    private void initDragStatus(float activeMoveY) {
        float diff = activeMoveY - mInitialDownY;
        if (mIsRefreshing && (diff > mTouchSlop || mTargetOrRefreshViewOffsetY > 0)) {
            mIsBeingDragged = true;
            mInitialMotionY = mInitialDownY + mTouchSlop;
            //scroll direction: from up to down
        } else if (!mIsBeingDragged && diff > mTouchSlop) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
        }
    }

    private void initRefreshView() {
        mRefreshView = new RefreshView(getContext());
        mRefreshView.setVisibility(View.GONE);
        if (mRefreshView instanceof IRefreshStatus) {
            mRefreshStatus = (IRefreshStatus) mRefreshView;
        } else {
            throw new ClassCastException("the refreshView must implement the interface IRefreshStatus");
        }

        LayoutParams layoutParams = new LayoutParams(mRefreshViewSize, mRefreshViewSize);
        addView(mRefreshView, layoutParams);
    }

    private boolean isTargetValid() {
        for (int i = 0; i < getChildCount(); i++) {
            if (mTarget == getChildAt(i)) {
                return true;
            }
        }

        return false;
    }

    private void measureRefreshView(int widthMeasureSpec, int heightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) mRefreshView.getLayoutParams();

        final int childWidthMeasureSpec;
        if (lp.width == LayoutParams.MATCH_PARENT) {
            final int width = Math.max(0, getMeasuredWidth() - getPaddingLeft() - getPaddingRight()
                    - lp.leftMargin - lp.rightMargin);
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
        }

        final int childHeightMeasureSpec;
        if (lp.height == LayoutParams.MATCH_PARENT) {
            final int height = Math.max(0, getMeasuredHeight()
                    - getPaddingTop() - getPaddingBottom()
                    - lp.topMargin - lp.bottomMargin);
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    height, MeasureSpec.EXACTLY);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() +
                            lp.topMargin + lp.bottomMargin,
                    lp.height);
        }

        mRefreshView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private void measureTarget() {
        mTarget.measure(MeasureSpec
                        .makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        MeasureSpec.EXACTLY));
    }

    /**
     * @param targetOrRefreshViewOffsetY the top position of the target
     *                                   or the RefreshView relative to its parent.
     */
    private void moveSpinner(float targetOrRefreshViewOffsetY) {
        mCurrentTouchOffsetY = targetOrRefreshViewOffsetY;

        float convertScrollOffset;
        float refreshTargetOffset;
        if (!mIsRefreshing) {
            if (mRefreshStyle == RefreshStyle.FLOAT) {
                convertScrollOffset = mRefreshInitialOffset
                        + mDragDistanceConverter.convert(targetOrRefreshViewOffsetY, mRefreshTargetOffset);
            } else {
                convertScrollOffset = mDragDistanceConverter
                        .convert(targetOrRefreshViewOffsetY, mRefreshTargetOffset);
            }
        } else {
            //The Float style will never come here
            convertScrollOffset = Math.min(targetOrRefreshViewOffsetY, mRefreshTargetOffset);

            if (convertScrollOffset < 0.0f) {
                convertScrollOffset = 0.0f;
            }

        }
        refreshTargetOffset = mRefreshTargetOffset;

        if (!mIsRefreshing) {
            if (convertScrollOffset > refreshTargetOffset && !mIsFitRefresh) {
                mIsFitRefresh = true;
                mRefreshStatus.pullToRefresh();
            } else if (convertScrollOffset <= refreshTargetOffset && mIsFitRefresh) {
                mIsFitRefresh = false;
            }
        }

        RefreshLogger.i(targetOrRefreshViewOffsetY + " -- " + refreshTargetOffset + " -- "
                + convertScrollOffset + " -- " + mTargetOrRefreshViewOffsetY + " -- " + mRefreshTargetOffset);

        setTargetOrRefreshViewOffsetY((int) (convertScrollOffset - mTargetOrRefreshViewOffsetY));
    }

    private void onNewerPointerDown(MotionEvent ev) {
        final int index = ev.getActionIndex();
        mActivePointerId = ev.getPointerId(index);

        mInitialMotionY = getMotionEventY(ev, mActivePointerId) - mCurrentTouchOffsetY;

        RefreshLogger.i(" onDown " + mInitialMotionY);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = ev.getActionIndex();
        int pointerId = ev.getPointerId(pointerIndex);

        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }

        mInitialMotionY = getMotionEventY(ev, mActivePointerId) - mCurrentTouchOffsetY;

        RefreshLogger.i(" onUp " + mInitialMotionY);
    }

    private void reset() {
        setTargetOrRefreshViewToInitial();

        mCurrentTouchOffsetY = 0.0f;

        mRefreshStatus.reset();
        mRefreshView.setVisibility(View.GONE);

        mIsRefreshing = false;
        mIsAnimatingToStart = false;
    }

    private void resetTouchEvent() {
        mInitialScrollY = 0.0f;

        mIsBeingDragged = false;
        mDispatchTargetTouchDown = false;
        mActivePointerId = INVALID_POINTER;
    }

    private int reviseRefreshViewLayoutTop(int layoutTop) {
        if (mRefreshStyle == RefreshStyle.PINNED) {
            return layoutTop;
        }//not consider mRefreshResistanceRate < 1.0f
        return layoutTop + (int) mTargetOrRefreshViewOffsetY;
    }

    private int reviseTargetLayoutTop(int layoutTop) {
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            return layoutTop;
        }//not consider mRefreshResistanceRate < 1.0f
        return layoutTop + (int) mTargetOrRefreshViewOffsetY;
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mIsRefreshing != refreshing) {
            mNotifyListener = notify;
            mIsRefreshing = refreshing;
            if (refreshing) {
                animateToRefreshingPosition((int) mTargetOrRefreshViewOffsetY, mRefreshingListener);
            } else {
                animateOffsetToStartPosition((int) mTargetOrRefreshViewOffsetY, mResetListener);
            }
        }
    }

    private void setTargetOrRefreshViewOffsetY(int offsetY) {
        if (mTarget == null) {
            return;
        }

        switch (mRefreshStyle) {
            case FLOAT:
                mRefreshView.offsetTopAndBottom(offsetY);
                mTargetOrRefreshViewOffsetY = mRefreshView.getTop();
                break;
            case PINNED:
                mTarget.offsetTopAndBottom(offsetY);
                mTargetOrRefreshViewOffsetY = mTarget.getTop();
                break;
            default:
                mTarget.offsetTopAndBottom(offsetY);
                mRefreshView.offsetTopAndBottom(offsetY);
                mTargetOrRefreshViewOffsetY = mTarget.getTop();
                break;
        }

        RefreshLogger.i("current offset" + mTargetOrRefreshViewOffsetY);

        if (mRefreshStyle == RefreshStyle.FLOAT) {
            mRefreshStatus.pullProgress(mTargetOrRefreshViewOffsetY,
                    (mTargetOrRefreshViewOffsetY - mRefreshInitialOffset) / mRefreshTargetOffset);
        } else {
            mRefreshStatus
                    .pullProgress(mTargetOrRefreshViewOffsetY, mTargetOrRefreshViewOffsetY / mRefreshTargetOffset);
        }

        if (mRefreshView.getVisibility() != View.VISIBLE) {
            mRefreshView.setVisibility(View.VISIBLE);
        }

        invalidate();
    }

    private void setTargetOrRefreshViewToInitial() {
        if (mRefreshStyle == RefreshStyle.FLOAT) {
            setTargetOrRefreshViewOffsetY((int) (mRefreshInitialOffset - mTargetOrRefreshViewOffsetY));
        } else {
            setTargetOrRefreshViewOffsetY((int) (0 - mTargetOrRefreshViewOffsetY));
        }
    }
}