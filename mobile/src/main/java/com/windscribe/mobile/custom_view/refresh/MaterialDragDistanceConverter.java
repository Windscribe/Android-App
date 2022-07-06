/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.custom_view.refresh;


public class MaterialDragDistanceConverter implements IDragDistanceConverter {

    @Override
    public float convert(float scrollDistance, float refreshDistance) {
        float originalDragPercent = scrollDistance / refreshDistance;
        float dragPercent = Math.min(1.0f, Math.abs(originalDragPercent));
        float extraOS = Math.abs(scrollDistance) - refreshDistance;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, refreshDistance * 2.0f) / refreshDistance);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) -
                Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (refreshDistance) * tensionPercent * 2;

        return (int) ((refreshDistance * dragPercent) + extraMove);
    }
}
