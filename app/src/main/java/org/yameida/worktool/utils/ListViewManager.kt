package org.yameida.worktool.utils

import android.annotation.SuppressLint
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import org.yameida.worktool.R

@SuppressLint("StaticFieldLeak")
object ListViewManager {
    private var listView: ListView? = null
    private var adapter: ArrayAdapter<String>? = null
    private val dataList = mutableListOf<String>()

    fun init(listView: ListView) {
        this.listView = listView
        adapter = ArrayAdapter(listView.context, R.layout.item_list)
        listView.adapter = adapter

        // 设置自动滚动模式
        listView.transcriptMode = ListView.TRANSCRIPT_MODE_NORMAL

        // 添加滚动监听器来确保滚动行为
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}
            override fun onScroll(
                view: AbsListView?, firstVisibleItem: Int,
                visibleItemCount: Int, totalItemCount: Int
            ) {
            }
        })
    }

    fun addItem(item: String) {
        // 确保所有UI操作都在主线程执行
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            dataList.add(item)
            adapter?.add(item)
            adapter?.notifyDataSetChanged()

            scrollToBottom()
        }
    }

    fun addItems(items: List<String>) {
        // 确保所有UI操作都在主线程执行
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            dataList.addAll(items)
            adapter?.addAll(items)
            adapter?.notifyDataSetChanged()
            scrollToBottom()
        }
    }

    fun clearItems() {
        // 确保所有UI操作都在主线程执行
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            dataList.clear()
            adapter?.clear()
            adapter?.notifyDataSetChanged()
        }
    }

    fun getItems(): List<String> {
        return dataList.toList()
    }

    // 修改后的滚动到底部方法
    private fun scrollToBottom() {
        // 延迟一帧执行滚动，确保列表已经更新
        listView?.postDelayed({
            try {
                val count = adapter?.count ?: 0
                if (count > 0) {
                    // 直接滚动到最后一个位置
                    listView?.setSelection(count)

                    // 额外确保滚动到底部
                    listView?.smoothScrollToPosition(count)

                    // 如果上面的方法都不能确保滚动到底部，可以尝试这个方法
                    listView?.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 16) // 16ms 约等于一帧的时间
    }

    // 新增滚动到顶部的方法
    private fun scrollToTop() {
        listView?.post {
            try {
                listView?.setSelection(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}