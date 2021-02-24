package com.flyco.tablayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.flyco.tablayout.listener.OnTabSelectListener
import com.flyco.tablayout.utils.UnreadMsgUtils
import com.flyco.tablayout.widget.MsgView
import java.util.*

/**
 * Created by yxm at 2021/2/24 16:55
 * @email: yxmbest@163.com
 * @description: viewpager2支持
 */
class SlidingViewPager2TabLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : HorizontalScrollView(context, attrs, defStyleAttr) {
    private var mContext: Context? = null
    private var mViewPager: ViewPager2? = null
    private var mTitles: MutableList<String>? = null
    private var mTabsContainer: LinearLayout? = null
    private var mCurrentTab = 0
    private var mCurrentPositionOffset = 0f
    private var mTabCount = 0

    /**
     * 用于绘制显示器
     */
    private val mIndicatorRect = Rect()

    /**
     * 用于实现滚动居中
     */
    private val mTabRect = Rect()
    private val mIndicatorDrawable = GradientDrawable()

    private val mRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTrianglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTrianglePath = Path()
    private val STYLE_NORMAL = 0
    private val STYLE_TRIANGLE = 1
    private val STYLE_BLOCK = 2
    private var mIndicatorStyle = STYLE_NORMAL

    private var mTabPadding = 0f
    private var mTabSpaceEqual = false
    private var mTabWidth = 0f

    /**
     * indicator
     */
    private var mIndicatorColor = 0
    private var mIndicatorHeight = 0f
    private var mIndicatorWidth = 0f
    private var mIndicatorCornerRadius = 0f
    private var mIndicatorMarginLeft = 0f
    private var mIndicatorMarginTop = 0f
    private var mIndicatorMarginRight = 0f
    private var mIndicatorMarginBottom = 0f
    private var mIndicatorGravity = 0
    private var mIndicatorWidthEqualTitle = false

    /**
     * underline
     */
    private var mUnderlineColor = 0
    private var mUnderlineHeight = 0f
    private var mUnderlineGravity = 0

    /**
     * divider
     */
    private var mDividerColor = 0
    private var mDividerWidth = 0f
    private var mDividerPadding = 0f

    /**
     * title
     */
    private val TEXT_BOLD_NONE = 0
    private val TEXT_BOLD_WHEN_SELECT = 1
    private val TEXT_BOLD_BOTH = 2
    private var mTextsize = 0f
    private var mTextSelectColor = 0
    private var mTextUnselectColor = 0
    private var mTextBold = 0
    private var mTextAllCaps = false

    private var mLastScrollX = 0
    private var mHeight = 0
    private var mSnapOnTabClick = false

    private val mPageChangeCallback: OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            /**
             * position:当前View的位置
             * mCurrentPositionOffset:当前View的偏移量比例.[0,1)
             */
            mCurrentTab = position
            mCurrentPositionOffset = positionOffset
            scrollToCurrentTab()
            invalidate()
        }

        override fun onPageSelected(position: Int) {
            updateTabSelection(position)
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    init {
        isFillViewport = true //设置滚动视图是否可以伸缩其内容以填充视口
        setWillNotDraw(false) //重写onDraw方法,需要调用这个方法来清除flag
        clipChildren = false
        clipToPadding = false
        mContext = context
        mTabsContainer = LinearLayout(context)
        addView(mTabsContainer)
        obtainAttributes(context, attrs)

        //get layout_height
        when (attrs?.getAttributeValue("http://schemas.android.com/apk/res/android", "layout_height")) {
            ViewGroup.LayoutParams.MATCH_PARENT.toString() + "" -> {
            }
            ViewGroup.LayoutParams.WRAP_CONTENT.toString() + "" -> {
            }
            else -> {
                val systemAttrs = intArrayOf(android.R.attr.layout_height)
                val a = context.obtainStyledAttributes(attrs, systemAttrs)
                mHeight = a.getDimensionPixelSize(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                a.recycle()
            }
        }
    }

    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingTabLayout)
        mIndicatorStyle = ta.getInt(R.styleable.SlidingTabLayout_tl_indicator_style, STYLE_NORMAL)
        mIndicatorColor = ta.getColor(R.styleable.SlidingTabLayout_tl_indicator_color, Color.parseColor(if (mIndicatorStyle == STYLE_BLOCK) "#4B6A87" else "#ffffff"))
        mIndicatorHeight = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_height,
                dp2px(if (mIndicatorStyle == STYLE_TRIANGLE) 4.toFloat() else (if (mIndicatorStyle == STYLE_BLOCK) -1 else 2).toFloat()).toFloat())
        mIndicatorWidth = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_width, dp2px(if (mIndicatorStyle == STYLE_TRIANGLE) 10.toFloat() else (-1).toFloat()).toFloat())
        mIndicatorCornerRadius = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_corner_radius, dp2px(if (mIndicatorStyle == STYLE_BLOCK) (-1).toFloat() else 0.toFloat()).toFloat())
        mIndicatorMarginLeft = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_margin_left, dp2px(0f).toFloat())
        mIndicatorMarginTop = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_margin_top, dp2px(if (mIndicatorStyle == STYLE_BLOCK) 7.toFloat() else 0.toFloat()).toFloat())
        mIndicatorMarginRight = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_margin_right, dp2px(0f).toFloat())
        mIndicatorMarginBottom = ta.getDimension(R.styleable.SlidingTabLayout_tl_indicator_margin_bottom, dp2px(if (mIndicatorStyle == STYLE_BLOCK) 7.toFloat() else 0.toFloat()).toFloat())
        mIndicatorGravity = ta.getInt(R.styleable.SlidingTabLayout_tl_indicator_gravity, Gravity.BOTTOM)
        mIndicatorWidthEqualTitle = ta.getBoolean(R.styleable.SlidingTabLayout_tl_indicator_width_equal_title, false)
        mUnderlineColor = ta.getColor(R.styleable.SlidingTabLayout_tl_underline_color, Color.parseColor("#ffffff"))
        mUnderlineHeight = ta.getDimension(R.styleable.SlidingTabLayout_tl_underline_height, dp2px(0f).toFloat())
        mUnderlineGravity = ta.getInt(R.styleable.SlidingTabLayout_tl_underline_gravity, Gravity.BOTTOM)
        mDividerColor = ta.getColor(R.styleable.SlidingTabLayout_tl_divider_color, Color.parseColor("#ffffff"))
        mDividerWidth = ta.getDimension(R.styleable.SlidingTabLayout_tl_divider_width, dp2px(0f).toFloat())
        mDividerPadding = ta.getDimension(R.styleable.SlidingTabLayout_tl_divider_padding, dp2px(12f).toFloat())
        mTextsize = ta.getDimension(R.styleable.SlidingTabLayout_tl_textsize, sp2px(14f).toFloat())
        mTextSelectColor = ta.getColor(R.styleable.SlidingTabLayout_tl_textSelectColor, Color.parseColor("#ffffff"))
        mTextUnselectColor = ta.getColor(R.styleable.SlidingTabLayout_tl_textUnselectColor, Color.parseColor("#AAffffff"))
        mTextBold = ta.getInt(R.styleable.SlidingTabLayout_tl_textBold, TEXT_BOLD_NONE)
        mTextAllCaps = ta.getBoolean(R.styleable.SlidingTabLayout_tl_textAllCaps, false)
        mTabSpaceEqual = ta.getBoolean(R.styleable.SlidingTabLayout_tl_tab_space_equal, false)
        mTabWidth = ta.getDimension(R.styleable.SlidingTabLayout_tl_tab_width, dp2px(-1f).toFloat())
        mTabPadding = ta.getDimension(R.styleable.SlidingTabLayout_tl_tab_padding, if (mTabSpaceEqual || mTabWidth > 0) dp2px(0f).toFloat() else dp2px(20f).toFloat())
        ta.recycle()
    }

    /**
     * 关联ViewPager
     * 需要自己设置adapter
     */
    fun setViewPager(vp: ViewPager2?) {
        check(!(vp == null || vp.adapter == null)) { "ViewPager or ViewPager adapter can not be NULL !" }
        mViewPager = vp
        mViewPager!!.unregisterOnPageChangeCallback(mPageChangeCallback)
        mViewPager!!.registerOnPageChangeCallback(mPageChangeCallback)
        notifyDataSetChanged()
    }

    /**
     * 关联ViewPager,用于连适配器都不想自己实例化的情况
     */
    fun setViewPager(vp: ViewPager2, titles: List<String>, fa: FragmentActivity, fragments: ArrayList<Fragment>) {
        check(titles.isNotEmpty()) { "Titles can not be EMPTY !" }
        mViewPager = vp
        mViewPager!!.adapter = InnerPagerAdapter(fa, fragments, fa.lifecycle)
        mViewPager!!.unregisterOnPageChangeCallback(mPageChangeCallback)
        mViewPager!!.registerOnPageChangeCallback(mPageChangeCallback)
        mTitles = ArrayList()
        mTitles!!.addAll(titles)
        notifyDataSetChanged()
    }

    /**
     * 更新数据
     */
    private fun notifyDataSetChanged() {
        mTabsContainer!!.removeAllViews()
        mTabCount = if (mTitles == null) mViewPager!!.adapter!!.itemCount else mTitles!!.size
        var tabView: View
        for (i in 0 until mTabCount) {
            tabView = View.inflate(mContext, R.layout.layout_tab, null)
            val pageTitle: CharSequence = mTitles!![i]
            addTab(i, pageTitle.toString(), tabView)
        }
        updateTabStyles()
    }

    fun addNewTab(title: String) {
        val tabView = View.inflate(mContext, R.layout.layout_tab, null)
        if (mTitles != null) {
            mTitles!!.add(title)
        }
        val pageTitle: CharSequence = mTitles!![mTabCount]
        addTab(mTabCount, pageTitle.toString(), tabView)
        mTabCount = mTitles!!.size
        updateTabStyles()
    }

    /**
     * 创建并添加tab
     */
    private fun addTab(position: Int, title: String?, tabView: View) {
        val tv_tab_title = tabView.findViewById<TextView>(R.id.tv_tab_title)
        if (tv_tab_title != null) {
            if (title != null) tv_tab_title.text = title
        }
        tabView.setOnClickListener { v ->
            val position = mTabsContainer!!.indexOfChild(v)
            if (position != -1) {
                if (mViewPager!!.currentItem != position) {
                    if (mSnapOnTabClick) {
                        mViewPager!!.setCurrentItem(position, false)
                    } else {
                        mViewPager!!.currentItem = position
                    }
                    mListener?.onTabSelect(position)
                } else {
                    mListener?.onTabReselect(position)
                }
            }
        }
        /** 每一个Tab的布局参数  */
        var lp_tab = if (mTabSpaceEqual) LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, 1.0f) else LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)
        if (mTabWidth > 0) {
            lp_tab = LinearLayout.LayoutParams(mTabWidth.toInt(), FrameLayout.LayoutParams.MATCH_PARENT)
        }
        mTabsContainer!!.addView(tabView, position, lp_tab)
    }

    private fun updateTabStyles() {
        for (i in 0 until mTabCount) {
            val v = mTabsContainer!!.getChildAt(i)
            //            v.setPadding((int) mTabPadding, v.getPaddingTop(), (int) mTabPadding, v.getPaddingBottom());
            val tv_tab_title = v.findViewById<TextView>(R.id.tv_tab_title)
            if (tv_tab_title != null) {
                tv_tab_title.setTextColor(if (i == mCurrentTab) mTextSelectColor else mTextUnselectColor)
                tv_tab_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextsize)
                tv_tab_title.setPadding(mTabPadding.toInt(), 0, mTabPadding.toInt(), 0)
                if (mTextAllCaps) {
                    tv_tab_title.text = tv_tab_title.text.toString().toUpperCase()
                }
                if (mTextBold == TEXT_BOLD_BOTH) {
                    tv_tab_title.paint.isFakeBoldText = true
                } else if (mTextBold == TEXT_BOLD_NONE) {
                    tv_tab_title.paint.isFakeBoldText = false
                } else if (mTextBold == TEXT_BOLD_WHEN_SELECT) {
                    tv_tab_title.paint.isFakeBoldText = mCurrentTab == i
                }
            }
        }
    }

    /**
     * HorizontalScrollView滚到当前tab,并且居中显示
     */
    private fun scrollToCurrentTab() {
        if (mTabCount <= 0) {
            return
        }
        val offset = (mCurrentPositionOffset * mTabsContainer!!.getChildAt(mCurrentTab).width).toInt()

        /**当前Tab的left+当前Tab的Width乘以positionOffset */
        var newScrollX = mTabsContainer!!.getChildAt(mCurrentTab).left + offset
        if (mCurrentTab > 0 || offset > 0) {
            /**HorizontalScrollView移动到当前tab,并居中 */
            newScrollX -= getWidth() / 2 - getPaddingLeft()
            calcIndicatorRect()
            newScrollX += (mTabRect.right - mTabRect.left) / 2
        }
        if (newScrollX != mLastScrollX) {
            mLastScrollX = newScrollX
            /** scrollTo（int x,int y）:x,y代表的不是坐标点,而是偏移量
             * x:表示离起始位置的x水平方向的偏移量
             * y:表示离起始位置的y垂直方向的偏移量
             */
            scrollTo(newScrollX, 0)
        }
    }

    private fun updateTabSelection(position: Int) {
        for (i in 0 until mTabCount) {
            val tabView = mTabsContainer!!.getChildAt(i)
            val isSelect = i == position
            val tab_title = tabView.findViewById<TextView>(R.id.tv_tab_title)
            if (tab_title != null) {
                tab_title.setTextColor(if (isSelect) mTextSelectColor else mTextUnselectColor)
                if (mTextBold == TEXT_BOLD_WHEN_SELECT) {
                    tab_title.paint.isFakeBoldText = isSelect
                }
            }
        }
    }

    private var margin = 0f

    private fun calcIndicatorRect() {
        val currentTabView = mTabsContainer!!.getChildAt(mCurrentTab)
        var left = currentTabView.left.toFloat()
        var right = currentTabView.right.toFloat()

        //for mIndicatorWidthEqualTitle
        if (mIndicatorStyle == STYLE_NORMAL && mIndicatorWidthEqualTitle) {
            val tab_title = currentTabView.findViewById<TextView>(R.id.tv_tab_title)
            mTextPaint.textSize = mTextsize
            val textWidth = mTextPaint.measureText(tab_title.text.toString())
            margin = (right - left - textWidth) / 2
        }
        if (mCurrentTab < mTabCount - 1) {
            val nextTabView = mTabsContainer!!.getChildAt(mCurrentTab + 1)
            val nextTabLeft = nextTabView.left.toFloat()
            val nextTabRight = nextTabView.right.toFloat()
            left = left + mCurrentPositionOffset * (nextTabLeft - left)
            right = right + mCurrentPositionOffset * (nextTabRight - right)

            //for mIndicatorWidthEqualTitle
            if (mIndicatorStyle == STYLE_NORMAL && mIndicatorWidthEqualTitle) {
                val next_tab_title = nextTabView.findViewById<TextView>(R.id.tv_tab_title)
                mTextPaint.textSize = mTextsize
                val nextTextWidth = mTextPaint.measureText(next_tab_title.text.toString())
                val nextMargin = (nextTabRight - nextTabLeft - nextTextWidth) / 2
                margin = margin + mCurrentPositionOffset * (nextMargin - margin)
            }
        }
        mIndicatorRect.left = left.toInt()
        mIndicatorRect.right = right.toInt()
        //for mIndicatorWidthEqualTitle
        if (mIndicatorStyle == STYLE_NORMAL && mIndicatorWidthEqualTitle) {
            mIndicatorRect.left = (left + margin - 1).toInt()
            mIndicatorRect.right = (right - margin - 1).toInt()
        }
        mTabRect.left = left.toInt()
        mTabRect.right = right.toInt()
        if (mIndicatorWidth < 0) {   //indicatorWidth小于0时,原jpardogo's PagerSlidingTabStrip
        } else { //indicatorWidth大于0时,圆角矩形以及三角形
            var indicatorLeft = currentTabView.left + (currentTabView.width - mIndicatorWidth) / 2
            if (mCurrentTab < mTabCount - 1) {
                val nextTab = mTabsContainer!!.getChildAt(mCurrentTab + 1)
                indicatorLeft = indicatorLeft + mCurrentPositionOffset * (currentTabView.width / 2 + nextTab.width / 2)
            }
            mIndicatorRect.left = indicatorLeft.toInt()
            mIndicatorRect.right = (mIndicatorRect.left + mIndicatorWidth).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isInEditMode() || mTabCount <= 0) {
            return
        }
        val height: Int = getHeight()
        val paddingLeft: Int = getPaddingLeft()
        // draw divider
        if (mDividerWidth > 0) {
            mDividerPaint.strokeWidth = mDividerWidth
            mDividerPaint.color = mDividerColor
            for (i in 0 until mTabCount - 1) {
                val tab = mTabsContainer!!.getChildAt(i)
                canvas.drawLine(paddingLeft + tab.right.toFloat(), mDividerPadding, paddingLeft + tab.right.toFloat(), height - mDividerPadding, mDividerPaint)
            }
        }

        // draw underline
        if (mUnderlineHeight > 0) {
            mRectPaint.color = mUnderlineColor
            if (mUnderlineGravity == Gravity.BOTTOM) {
                canvas.drawRect(paddingLeft.toFloat(), height - mUnderlineHeight, mTabsContainer!!.width + paddingLeft.toFloat(), height.toFloat(), mRectPaint)
            } else {
                canvas.drawRect(paddingLeft.toFloat(), 0f, mTabsContainer!!.width + paddingLeft.toFloat(), mUnderlineHeight, mRectPaint)
            }
        }

        //draw indicator line
        calcIndicatorRect()
        if (mIndicatorStyle == STYLE_TRIANGLE) {
            if (mIndicatorHeight > 0) {
                mTrianglePaint.color = mIndicatorColor
                mTrianglePath.reset()
                mTrianglePath.moveTo(paddingLeft + mIndicatorRect.left.toFloat(), height.toFloat())
                mTrianglePath.lineTo(paddingLeft + mIndicatorRect.left / 2 + (mIndicatorRect.right / 2).toFloat(), height - mIndicatorHeight)
                mTrianglePath.lineTo(paddingLeft + mIndicatorRect.right.toFloat(), height.toFloat())
                mTrianglePath.close()
                canvas.drawPath(mTrianglePath, mTrianglePaint)
            }
        } else if (mIndicatorStyle == STYLE_BLOCK) {
            if (mIndicatorHeight < 0) {
                mIndicatorHeight = height - mIndicatorMarginTop - mIndicatorMarginBottom
            } else {
            }
            if (mIndicatorHeight > 0) {
                if (mIndicatorCornerRadius < 0 || mIndicatorCornerRadius > mIndicatorHeight / 2) {
                    mIndicatorCornerRadius = mIndicatorHeight / 2
                }
                mIndicatorDrawable.setColor(mIndicatorColor)
                mIndicatorDrawable.setBounds(paddingLeft + mIndicatorMarginLeft.toInt() + mIndicatorRect.left,
                        mIndicatorMarginTop.toInt(), (paddingLeft + mIndicatorRect.right - mIndicatorMarginRight).toInt(),
                        (mIndicatorMarginTop + mIndicatorHeight).toInt())
                mIndicatorDrawable.cornerRadius = mIndicatorCornerRadius
                mIndicatorDrawable.draw(canvas)
            }
        } else {
            /* mRectPaint.setColor(mIndicatorColor);
                calcIndicatorRect();
                canvas.drawRect(getPaddingLeft() + mIndicatorRect.left, getHeight() - mIndicatorHeight,
                        mIndicatorRect.right + getPaddingLeft(), getHeight(), mRectPaint);*/
            if (mIndicatorHeight > 0) {
                mIndicatorDrawable.setColor(mIndicatorColor)
                if (mIndicatorGravity == Gravity.BOTTOM) {
                    mIndicatorDrawable.setBounds(paddingLeft + mIndicatorMarginLeft.toInt() + mIndicatorRect.left,
                            height - mIndicatorHeight.toInt() - mIndicatorMarginBottom.toInt(),
                            paddingLeft + mIndicatorRect.right - mIndicatorMarginRight.toInt(),
                            height - mIndicatorMarginBottom.toInt())
                } else {
                    mIndicatorDrawable.setBounds(paddingLeft + mIndicatorMarginLeft.toInt() + mIndicatorRect.left,
                            mIndicatorMarginTop.toInt(),
                            paddingLeft + mIndicatorRect.right - mIndicatorMarginRight.toInt(),
                            mIndicatorHeight.toInt() + mIndicatorMarginTop.toInt())
                }
                mIndicatorDrawable.cornerRadius = mIndicatorCornerRadius
                mIndicatorDrawable.draw(canvas)
            }
        }
    }

    //setter and getter
    fun setCurrentTab(currentTab: Int) {
        mCurrentTab = currentTab
        mViewPager!!.currentItem = currentTab
        updateTabSelection(currentTab)
    }

    fun setCurrentTab(currentTab: Int, smoothScroll: Boolean) {
        mCurrentTab = currentTab
        mViewPager!!.setCurrentItem(currentTab, smoothScroll)
        updateTabSelection(currentTab)
    }

    fun setIndicatorStyle(indicatorStyle: Int) {
        mIndicatorStyle = indicatorStyle
        invalidate()
    }

    fun setTabPadding(tabPadding: Float) {
        mTabPadding = dp2px(tabPadding).toFloat()
        updateTabStyles()
    }

    fun setTabSpaceEqual(tabSpaceEqual: Boolean) {
        mTabSpaceEqual = tabSpaceEqual
        updateTabStyles()
    }

    fun setTabWidth(tabWidth: Float) {
        mTabWidth = dp2px(tabWidth).toFloat()
        updateTabStyles()
    }

    fun setIndicatorColor(indicatorColor: Int) {
        mIndicatorColor = indicatorColor
        invalidate()
    }

    fun setIndicatorHeight(indicatorHeight: Float) {
        mIndicatorHeight = dp2px(indicatorHeight).toFloat()
        invalidate()
    }

    fun setIndicatorWidth(indicatorWidth: Float) {
        mIndicatorWidth = dp2px(indicatorWidth).toFloat()
        invalidate()
    }

    fun setIndicatorCornerRadius(indicatorCornerRadius: Float) {
        mIndicatorCornerRadius = dp2px(indicatorCornerRadius).toFloat()
        invalidate()
    }

    fun setIndicatorGravity(indicatorGravity: Int) {
        mIndicatorGravity = indicatorGravity
        invalidate()
    }

    fun setIndicatorMargin(indicatorMarginLeft: Float, indicatorMarginTop: Float,
                           indicatorMarginRight: Float, indicatorMarginBottom: Float) {
        mIndicatorMarginLeft = dp2px(indicatorMarginLeft).toFloat()
        mIndicatorMarginTop = dp2px(indicatorMarginTop).toFloat()
        mIndicatorMarginRight = dp2px(indicatorMarginRight).toFloat()
        mIndicatorMarginBottom = dp2px(indicatorMarginBottom).toFloat()
        invalidate()
    }

    fun setIndicatorWidthEqualTitle(indicatorWidthEqualTitle: Boolean) {
        mIndicatorWidthEqualTitle = indicatorWidthEqualTitle
        invalidate()
    }

    fun setUnderlineColor(underlineColor: Int) {
        mUnderlineColor = underlineColor
        invalidate()
    }

    fun setUnderlineHeight(underlineHeight: Float) {
        mUnderlineHeight = dp2px(underlineHeight).toFloat()
        invalidate()
    }

    fun setUnderlineGravity(underlineGravity: Int) {
        mUnderlineGravity = underlineGravity
        invalidate()
    }

    fun setDividerColor(dividerColor: Int) {
        mDividerColor = dividerColor
        invalidate()
    }

    fun setDividerWidth(dividerWidth: Float) {
        mDividerWidth = dp2px(dividerWidth).toFloat()
        invalidate()
    }

    fun setDividerPadding(dividerPadding: Float) {
        mDividerPadding = dp2px(dividerPadding).toFloat()
        invalidate()
    }

    fun setTextsize(textsize: Float) {
        mTextsize = sp2px(textsize).toFloat()
        updateTabStyles()
    }

    fun setTextSelectColor(textSelectColor: Int) {
        mTextSelectColor = textSelectColor
        updateTabStyles()
    }

    fun setTextUnselectColor(textUnselectColor: Int) {
        mTextUnselectColor = textUnselectColor
        updateTabStyles()
    }

    fun setTextBold(textBold: Int) {
        mTextBold = textBold
        updateTabStyles()
    }

    fun setTextAllCaps(textAllCaps: Boolean) {
        mTextAllCaps = textAllCaps
        updateTabStyles()
    }

    fun setSnapOnTabClick(snapOnTabClick: Boolean) {
        mSnapOnTabClick = snapOnTabClick
    }


    fun getTabCount(): Int {
        return mTabCount
    }

    fun getCurrentTab(): Int {
        return mCurrentTab
    }

    fun getIndicatorStyle(): Int {
        return mIndicatorStyle
    }

    fun getTabPadding(): Float {
        return mTabPadding
    }

    fun isTabSpaceEqual(): Boolean {
        return mTabSpaceEqual
    }

    fun getTabWidth(): Float {
        return mTabWidth
    }

    fun getIndicatorColor(): Int {
        return mIndicatorColor
    }

    fun getIndicatorHeight(): Float {
        return mIndicatorHeight
    }

    fun getIndicatorWidth(): Float {
        return mIndicatorWidth
    }

    fun getIndicatorCornerRadius(): Float {
        return mIndicatorCornerRadius
    }

    fun getIndicatorMarginLeft(): Float {
        return mIndicatorMarginLeft
    }

    fun getIndicatorMarginTop(): Float {
        return mIndicatorMarginTop
    }

    fun getIndicatorMarginRight(): Float {
        return mIndicatorMarginRight
    }

    fun getIndicatorMarginBottom(): Float {
        return mIndicatorMarginBottom
    }

    fun getUnderlineColor(): Int {
        return mUnderlineColor
    }

    fun getUnderlineHeight(): Float {
        return mUnderlineHeight
    }

    fun getDividerColor(): Int {
        return mDividerColor
    }

    fun getDividerWidth(): Float {
        return mDividerWidth
    }

    fun getDividerPadding(): Float {
        return mDividerPadding
    }

    fun getTextsize(): Float {
        return mTextsize
    }

    fun getTextSelectColor(): Int {
        return mTextSelectColor
    }

    fun getTextUnselectColor(): Int {
        return mTextUnselectColor
    }

    fun getTextBold(): Int {
        return mTextBold
    }

    fun isTextAllCaps(): Boolean {
        return mTextAllCaps
    }

    fun getTitleView(tab: Int): TextView? {
        val tabView = mTabsContainer!!.getChildAt(tab)
        return tabView.findViewById(R.id.tv_tab_title)
    }

    //setter and getter

    //setter and getter
    // show MsgTipView
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    @SuppressLint("UseSparseArrays")
    private val mInitSetMap = SparseArray<Boolean?>()

    /**
     * 显示未读消息
     *
     * @param position 显示tab位置
     * @param num      num小于等于0显示红点,num大于0显示数字
     */
    fun showMsg(position: Int, num: Int) {
        var position = position
        if (position >= mTabCount) {
            position = mTabCount - 1
        }
        val tabView = mTabsContainer!!.getChildAt(position)
        val tipView: MsgView = tabView.findViewById(R.id.rtv_msg_tip)
        if (tipView != null) {
            UnreadMsgUtils.show(tipView, num)
            if (mInitSetMap[position] != null && mInitSetMap[position]!!) {
                return
            }
            setMsgMargin(position, -6f, 2f)
            mInitSetMap.put(position, true)
        }
    }

    /**
     * 显示未读红点
     *
     * @param position 显示tab位置
     */
    fun showDot(position: Int) {
        var position = position
        if (position >= mTabCount) {
            position = mTabCount - 1
        }
        showMsg(position, 0)
    }

    /**
     * 隐藏未读消息
     */
    fun hideMsg(position: Int) {
        var position = position
        if (position >= mTabCount) {
            position = mTabCount - 1
        }
        val tabView = mTabsContainer!!.getChildAt(position)
        val tipView: MsgView = tabView.findViewById(R.id.rtv_msg_tip)
        if (tipView != null) {
            tipView.setVisibility(View.GONE)
        }
    }

    /**
     * 设置未读消息偏移,原点为文字的右上角.当控件高度固定,消息提示位置易控制,显示效果佳
     */
    fun setMsgMargin(position: Int, leftPadding: Float, bottomPadding: Float) {
        var position = position
        if (position >= mTabCount) {
            position = mTabCount - 1
        }
        val tabView = mTabsContainer!!.getChildAt(position)
        val tipView: MsgView = tabView.findViewById(R.id.rtv_msg_tip)
        if (tipView != null) {
            val tv_tab_title = tabView.findViewById<TextView>(R.id.tv_tab_title)
            mTextPaint.textSize = mTextsize
            val textWidth = mTextPaint.measureText(tv_tab_title.text.toString())
            val textHeight = mTextPaint.descent() - mTextPaint.ascent()
            val lp = tipView.getLayoutParams() as MarginLayoutParams
            lp.leftMargin = if (mTabWidth >= 0) (mTabWidth / 2 + textWidth / 2 + dp2px(leftPadding)).toInt() else (mTabPadding + textWidth + dp2px(leftPadding)).toInt()
            lp.topMargin = if (mHeight > 0) (mHeight - textHeight).toInt() / 2 - dp2px(bottomPadding) else 0
            tipView.setLayoutParams(lp)
        }
    }

    /**
     * 当前类只提供了少许设置未读消息属性的方法,可以通过该方法获取MsgView对象从而各种设置
     */
    fun getMsgView(position: Int): MsgView? {
        var position = position
        if (position >= mTabCount) {
            position = mTabCount - 1
        }
        val tabView = mTabsContainer!!.getChildAt(position)
        return tabView.findViewById(R.id.rtv_msg_tip)
    }

    private var mListener: OnTabSelectListener? = null

    fun setOnTabSelectListener(listener: OnTabSelectListener?) {
        mListener = listener
    }

    internal class InnerPagerAdapter : FragmentStateAdapter {
        var fragmentArrayList: List<Fragment>

        constructor(fragmentManager: FragmentActivity?, fragmentArrayList: List<Fragment>) : super(fragmentManager!!) {
            this.fragmentArrayList = fragmentArrayList
        }

        constructor(fragmentManager: FragmentActivity, fragmentArrayList: List<Fragment>, lifecycle: Lifecycle?) : super(fragmentManager.supportFragmentManager, lifecycle!!) {
            this.fragmentArrayList = fragmentArrayList
        }

        constructor(fa: FragmentActivity?, fragmentManager: FragmentManager?, lifecycle: Lifecycle?) : super(fragmentManager!!, lifecycle!!) {
            fragmentArrayList = ArrayList()
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentArrayList[position]
        }

        override fun getItemCount(): Int {
            return fragmentArrayList.size
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        bundle.putInt("mCurrentTab", mCurrentTab)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var state = state
        if (state is Bundle) {
            val bundle = state
            mCurrentTab = bundle.getInt("mCurrentTab")
            state = bundle.getParcelable("instanceState")
            if (mCurrentTab != 0 && mTabsContainer!!.childCount > 0) {
                updateTabSelection(mCurrentTab)
                scrollToCurrentTab()
            }
        }
        super.onRestoreInstanceState(state)
    }

    private fun dp2px(dp: Float): Int {
        val scale = mContext!!.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun sp2px(sp: Float): Int {
        val scale = mContext!!.resources.displayMetrics.scaledDensity
        return (sp * scale + 0.5f).toInt()
    }
}