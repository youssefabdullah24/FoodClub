package com.example.foodclub.utils


import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.ahmadhamwi.tabsync.TabViewCompositeClickListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener

/**
 * This class is made to provide the ability to sync between RecyclerView's specific items with
 * TabLayout tabs.
 *
 * @param mRecyclerView     The RecyclerView that is going to be synced with the TabLayout
 * @param mTabLayout        The TabLayout that is going to be synced with the RecyclerView specific
 *                          items.
 * @param mIndices          The indices of the RecyclerView's items that is going to be playing a
 *                          role of "check points" for the syncing operation.
 * @param mIsSmoothScroll   Defines the ability of smooth scroll when clicking the tabs of the
 *                          TabLayout.
 */
class TabRecyclerSync(
    private val mRecyclerView: RecyclerView,
    private val mTabLayout: TabLayout,
    private var mIndices: List<Int>,
    private var mIsSmoothScroll: Boolean = false
) {

    private var mIsAttached = false

    private var mRecyclerState = RecyclerView.SCROLL_STATE_IDLE
    private var mTabClickFlag = false

    private val smoothScroller: SmoothScroller =
        object : LinearSmoothScroller(mRecyclerView.context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }

    private var tabViewCompositeClickListener: TabViewCompositeClickListener =
        TabViewCompositeClickListener(mTabLayout)

    /**
     * Calling this method will ensure that the data that has been provided to the mediator is
     * valid for use, and start syncing between the the RecyclerView and the TabLayout.
     *
     * Call this method when you have:
     *      1- provided a RecyclerView Adapter,
     *      2- provided a TabLayout with the appropriate number of tabs,
     *      3- provided indices of the recyclerview items that you are syncing the tabs with. (You
     *         need to be providing indices of at most the number of Tabs inflated in the TabLayout.)
     */
    fun attach() {
        mRecyclerView.adapter
            ?: throw RuntimeException("Cannot attach with no Adapter provided to RecyclerView")

        if (mTabLayout.tabCount == 0)
            throw RuntimeException("Cannot attach with no tabs provided to TabLayout")

        if (mIndices.size > mTabLayout.tabCount)
            throw RuntimeException("Cannot attach using more indices than the available tabs")

        notifyIndicesChanged()
        mIsAttached = true
    }


    /**
     * This method will attach the listeners required to make the synchronization possible.
     */

    private fun notifyIndicesChanged() {
        tabViewCompositeClickListener.addListener { _, _ -> mTabClickFlag = true }
        tabViewCompositeClickListener.build()
        mTabLayout.addOnTabSelectedListener(onTabSelectedListener)
        mRecyclerView.addOnScrollListener(onScrollListener)
    }

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {

            if (!mTabClickFlag) return

            val position = tab.position

            if (mIsSmoothScroll) {
                smoothScroller.targetPosition = mIndices[position]
                mRecyclerView.layoutManager?.startSmoothScroll(smoothScroller)
            } else {
                (mRecyclerView.layoutManager as LinearLayoutManager?)?.scrollToPositionWithOffset(
                    mIndices[position],
                    0
                )
            }
            mTabClickFlag = false

        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}
        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            mRecyclerState = newState
            if (mIsSmoothScroll && newState == RecyclerView.SCROLL_STATE_IDLE) {
                mTabClickFlag = false
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (mTabClickFlag) {
                return
            }

            val linearLayoutManager: LinearLayoutManager =
                recyclerView.layoutManager as LinearLayoutManager?
                    ?: throw RuntimeException("No LinearLayoutManager attached to the RecyclerView.")

            var itemPosition =
                linearLayoutManager.findFirstCompletelyVisibleItemPosition()

            if (itemPosition == -1) {
                itemPosition =
                    linearLayoutManager.findFirstVisibleItemPosition()
            }

            if (mRecyclerState == RecyclerView.SCROLL_STATE_DRAGGING
                || mRecyclerState == RecyclerView.SCROLL_STATE_SETTLING
            ) {
                for (i in mIndices.indices) {
                    if (itemPosition == mIndices[i]) {
                        if (!mTabLayout.getTabAt(i)!!.isSelected) {
                            mTabLayout.getTabAt(i)!!.select()
                        }
                        if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == mIndices[mIndices.size - 1]) {
                            if (!mTabLayout.getTabAt(mIndices.size - 1)!!.isSelected) {
                                mTabLayout.getTabAt(mIndices.size - 1)!!.select()
                            }
                            return
                        }
                    }
                }
            }
        }
    }
}