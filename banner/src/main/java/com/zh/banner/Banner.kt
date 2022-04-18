package com.zh.banner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.abs

/***
 * 2022/04/15 16:05
 */
class Banner @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), LifecycleObserver {

    companion object {
        const val BIG_SMALL = 0
        const val ALPHA = 1
        const val TAG = "Banner"
    }

    private var transferMode: ViewPager2.PageTransformer
    private var pLeft: Float
    private var pRight: Float
    private var pTop: Float
    private var pBottom: Float
    private var indicatorFillColor = Color.RED
    private var indicatorStrokeColor = Color.RED
    private var indicatorMargin = 5f
    private var indicatorSize = 30f
    private var scrollInterval = 5

    private var viewPager2: ViewPager2

    init {
        clipToPadding = false
        clipChildren = false
        context.obtainStyledAttributes(attrs, R.styleable.Banner).also {
            transferMode = createPageTransfer(it.getInt(R.styleable.Banner_bannerType, BIG_SMALL))

            val vertical = it.getDimension(R.styleable.Banner_pVertical, 0f)
            pTop = vertical
            pBottom = vertical

            val horizontal = it.getDimension(R.styleable.Banner_pHorizontal, 0f)
            pLeft = horizontal
            pRight = horizontal

            pLeft = it.getDimension(R.styleable.Banner_pLeft, pLeft)
            pRight = it.getDimension(R.styleable.Banner_pRight, pRight)
            pTop = it.getDimension(R.styleable.Banner_pTop, pTop)
            pBottom = it.getDimension(R.styleable.Banner_pBottom, pBottom)

            indicatorFillColor =
                it.getColor(R.styleable.Banner_indicatorFillColor, indicatorFillColor)
            indicatorStrokeColor =
                it.getColor(R.styleable.Banner_indicatorStrokeColor, indicatorStrokeColor)
            indicatorMargin = it.getDimension(R.styleable.Banner_indicator_margin, indicatorMargin)
            indicatorSize = it.getDimension(R.styleable.Banner_indicator_size, indicatorSize)
            scrollInterval = it.getInt(R.styleable.Banner_scroll_interval, scrollInterval)
            it.recycle()
        }
        viewPager2 = ViewPager2(context).also {
            it.setPadding(pLeft.toInt(), pTop.toInt(), pRight.toInt(), pBottom.toInt())
            it.clipToPadding = false
            it.clipChildren = false
            it.setPageTransformer(transferMode)
            it.offscreenPageLimit = 2
            val rv = it.getChildAt(0).also { v ->
                v.overScrollMode = View.OVER_SCROLL_NEVER
            }
            it.setOnTouchListener { _, event ->
                it.performClick()
                return@setOnTouchListener rv.onTouchEvent(event)
            }
            addView(
                it, LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                )
            )
        }
    }

    private val indicatorContainer = LinearLayoutCompat(context).also {
        it.gravity = Gravity.CENTER
    }

    private fun createPageTransfer(mode: Int): ViewPager2.PageTransformer = when (mode) {
        ALPHA -> AlphaMode()
        else -> BigSmall()
    }

    private var currentIndex = 1
    fun <T, VH : BannerVH<out ViewBinding>> setBannerAdapter(bannerAdapter: BannerAdapter<T, VH>) {
        val itemCount = bannerAdapter.bannerCount
        viewPager2.adapter = bannerAdapter
        viewPager2.setCurrentItem(currentIndex, false)
        initIndicators(itemCount)

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCheckedIndicator(position)
                currentIndex = position
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                judgePage(currentIndex, state)
            }
        })
    }

    private fun setCheckedIndicator(position: Int) {
        indicatorContainer.children.forEachIndexed { index: Int, view: View ->
            view as Circle
            if (position == 0 || vp2Adapter?.itemCount?.minus(1) == position) {
                view.isFill = position == index
                return
            }
            view.isFill = (position - 1) == index
        }
    }

    private fun judgePage(position: Int, state: Int) {
        if (state == ViewPager2.SCROLL_STATE_IDLE) {
            vp2Adapter?.let {
                when (
                    position
                ) {
                    0 -> {
                        viewPager2.setCurrentItem(it.itemCount - 2, false)
                    }

                    vp2Adapter?.itemCount?.minus(1) -> {
                        viewPager2.setCurrentItem(1, false)
                    }
                }
            }
        }
    }

    private fun initIndicators(itemCount: Int) {
        repeat(itemCount) {
            val circle =
                Circle(
                    context = context,
                    fill = indicatorFillColor,
                    stroke = indicatorStrokeColor,
                    size = indicatorSize.toInt()
                )
            if (it == currentIndex - 1) {
                circle.isFill = true
            }
            indicatorContainer.addView(
                circle,
                LinearLayoutCompat.LayoutParams(
                    indicatorSize.toInt(), indicatorSize.toInt(),
                ).also { layout ->
                    layout.marginStart = indicatorMargin.toInt()
                    layout.marginEnd = indicatorMargin.toInt()
                }
            )
        }
        indicatorContainer.setPadding(0, 10, 0, 10)
        addView(
            indicatorContainer, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).also {
                it.bottomToBottom = R.id.parent
                it.startToStart = R.id.parent
                it.endToEnd = R.id.parent
            }
        )
    }

    val vp2Adapter
        get() = viewPager2.adapter

    private class BigSmall : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            //这个判断是对所有不是当前页的进行处理，当前页：viewpager2.getCurrentItem获取的下标
            if (position < -1 || position > 1) {
                page.alpha = 0.5f
                page.scaleX = 0.8f
                page.scaleY = 0.8f
                return
            }

            //处理左边的
            if (position <= 0) {
                page.alpha =
                    0.5f + 0.5f * (1 + position)
            } else { // 处理右边
                page.alpha =
                    0.5f + 0.5f * (1 - position)
            }
            val scale = 0.8f.coerceAtLeast(1 - abs(position))
            page.scaleX = scale
            page.scaleY = scale
        }
    }

    private class AlphaMode : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            val pageWidth: Int = page.width
            val transX = if (position > 0) -pageWidth * position else pageWidth * position
            page.translationX = transX
            page.translationZ = -position
            if (position > 1 || position < -1) {
                page.alpha = 0f
                page.scaleY = 0f
                page.scaleX = 0f
                return
            }
            val scale = 1 - position * 0.7f
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = 1 - abs(position - position.toInt())
        }
    }

    private val timer = Timer()
    private val scope = MainScope()
    private val runnable: TimerTask.() -> Unit = {
        synchronized(onTouch) {
            Log.d(TAG, "onTouch: onTouch = $onTouch")
            if (onTouch) return@synchronized
            synchronized(currentIndex) {
                vp2Adapter?.itemCount?.let {
                    currentIndex = (currentIndex + 1) % it
                }
            }
            scope.launch {
                viewPager2.currentItem = currentIndex
            }
        }
    }

    @Volatile
    private var onTouch = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "onTouchEvent: down")
                onTouch = true
            }
            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "onTouchEvent: up")
                onTouch = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun startScroll() {
        val interval = scrollInterval * 1000L
        timer.schedule(timerTask(runnable), interval, interval)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun stopScroll() {
        timer.cancel()
    }
}

class Circle @JvmOverloads constructor(
    private val fill: Int, private val stroke: Int, size: Int,
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokeWidth = size / 10f

    private val fillPaint = Paint().also {
        it.style = Paint.Style.FILL
        it.color = fill
        it.isAntiAlias = true
    }

    private val strokePaint = Paint().also {
        it.style = Paint.Style.STROKE
        it.color = stroke
        it.strokeWidth = strokeWidth
        it.isAntiAlias = true
    }

    var isFill = false
        set(value) {
            field = value
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (height != width) error("width 必须等于 height")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            val center = measuredHeight / 2f
            val radius = center - strokeWidth
            if (isFill) {
                it.drawCircle(center, center, radius, fillPaint)
            } else {
                it.drawCircle(center, center, radius - strokeWidth / 2, strokePaint)
            }
        }
    }
}

abstract class BannerVH<T : ViewBinding>(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    abstract val binding: T
}

abstract class BannerAdapter<T, VH : BannerVH<out ViewBinding>>(diff: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diff) {

    val bannerCount
        get() = if (itemCount <= 1) itemCount else itemCount - 2

    private fun addFirstAndLast(list: MutableList<T>?) {
        list?.apply {
            if (size <= 1) return
            add(first())
            add(0, list[lastIndex - 1])
        }
    }

    override fun submitList(list: MutableList<T>?) {
        addFirstAndLast(list)
        super.submitList(list)
    }

    override fun submitList(list: MutableList<T>?, commitCallback: Runnable?) {
        addFirstAndLast(list)
        super.submitList(list, commitCallback)
    }
}