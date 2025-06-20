/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews
import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.windscribe.tv.R

/*
 Recycle to keep adaptable width for all node fragment.
 */
class AutoFitRecyclerView : RecyclerView {
    private var columnWidth = -1
    private var manager: GridLayoutManager? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        if (columnWidth > 0) {
            val spanCount = Math.max(1, measuredWidth / columnWidth)
            manager?.spanCount = spanCount
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        columnWidth = (200 * context.resources.displayMetrics.density).toInt()
        manager = GridLayoutManager(getContext(), 1)
        manager?.isItemPrefetchEnabled = true
        manager?.spanCount = 4
        layoutManager = manager
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
    }
}