/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view.refresh;

public interface IRefreshStatus {

    /**
     * @param pullDistance The drop-down distance of the refresh View
     * @param pullProgress The drop-down progress of the refresh View and the pullProgress may be more than 1.0f
     *                     pullProgress = pullDistance / refreshTargetOffset
     */
    void pullProgress(float pullDistance, float pullProgress);

    /**
     * Refresh View is dropped down to the refresh point
     */
    void pullToRefresh();

    /**
     * refresh has been completed
     */
    void refreshComplete();

    /**
     * Refresh View is refreshing
     */
    void refreshing();

    /**
     * When the content view has reached to the start point and refresh has been completed, view will be reset.
     */
    void reset();
}
