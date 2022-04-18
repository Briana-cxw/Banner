package com.zh.banner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.zh.banner.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val list = arrayListOf(
        "https://image.baidu.com/search/down?tn=download&word=download&ie=utf8&fr=detail&url=http://browser9.qhimg.com/bdm/1000_618_85/t01bbd94b90e850d1d3.jpg",
        "https://image.baidu.com/search/down?tn=download&word=download&ie=utf8&fr=detail&url=http://browser9.qhimg.com/bdm/1000_618_85/t016ad88ddaf2ae2d92.jpg",
        "https://image.baidu.com/search/down?tn=download&word=download&ie=utf8&fr=detail&url=http://browser9.qhimg.com/bdm/1000_618_85/t0183def7a3a7924215.jpg",
        "https://image.baidu.com/search/down?tn=download&word=download&ie=utf8&fr=detail&url=http://browser9.qhimg.com/bdm/1000_618_85/t01cd97ec806b712059.jpg",
        "https://image.baidu.com/search/down?tn=download&word=download&ie=utf8&fr=detail&url=http://browser9.qhimg.com/bdm/1000_618_85/t01a78941bc25ae2cf9.jpg",
    )

    private val idList = arrayListOf(
        R.drawable.b870d18f8c2edc23efc2e148f19c4b5b,
        R.drawable.d5f9b48efc88194a00dcbd32f6e6c33c,
        R.drawable.a5c20af0e07f4ff455768c4583439ce6,
        R.drawable.a92d85e921fc2cea15d38ac3ce07572a,
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        val vp2Adapter = Vp2Adapter().also { it.submitList(list) }

        viewBinding.vpTransformer.apply {
            offscreenPageLimit = list.size

            setOnTouchListener { v, event ->
                performClick()
                return@setOnTouchListener getChildAt(0).onTouchEvent(event)
            }

            getChildAt(0).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren = false
//                clipToPadding = false
            }

            adapter = vp2Adapter

            setPageTransformer { page, position ->
                val id = page.id
                Log.d(TAG, "onCreate: id = $id, position = $position")

                val pageWidth: Int = page.width
                val transX = if (position > 0) -pageWidth * position else pageWidth * position
                page.translationX = transX

                page.translationZ = -position
                if (position > 1 || position < -1) {
                    page.alpha = 0f
                    page.scaleY = 0f
                    page.scaleX = 0f
                    return@setPageTransformer
                }
                val scale = 1 - position * 0.7f
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 1 - abs(position - position.toInt())
            }
        }

        viewBinding.banner.setBannerAdapter(vp2Adapter)
        lifecycle.addObserver(viewBinding.banner)

        viewBinding.vp2.apply {
            getChildAt(0).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren = false
//                clipToPadding = false
            }

            setOnTouchListener { v, event ->
                performClick()
                return@setOnTouchListener getChildAt(0).onTouchEvent(event)
            }

            offscreenPageLimit = list.size
            adapter = vp2Adapter
            setPageTransformer(CompositePageTransformer().apply {
                addTransformer { page, position ->
                    if (position < -1 || position > 1) {
                        page.alpha = 0.5f
                        page.scaleX = 0.8f
                        page.scaleY = 0.8f
                        return@addTransformer
                    }

                    if (position <= 0) {
                        page.alpha =
                            0.5f + 0.5f * (1 + position)
                    } else {
                        page.alpha =
                            0.5f + 0.5f * (1 - position)
                    }
                    //缩放效果
                    val scale = 0.8f.coerceAtLeast(1 - abs(position))
                    page.scaleX = scale
                    page.scaleY = scale
                }
            })
        }

    }
}