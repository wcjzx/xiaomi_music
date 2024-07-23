package com.example.music_yaoyuhang;

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.music_yaoyuhang.Request.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HomePageAdapter
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = HomePageAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 添加滚动监听器
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 检查RecyclerView是否滚动到顶部
                val isAtTop = !recyclerView.canScrollVertically(-1)
                swipeRefreshLayout.isEnabled = isAtTop

                // 检查RecyclerView是否滚动到底部
                if (!recyclerView.canScrollVertically(1) && !isLoading && !isLastPage) {
                    loadMoreData()
                }
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            currentPage = 1
            isLastPage = false
            loadData()
        }

        loadData()
    }

    private fun loadData() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getHomePageData(currentPage)
                if (response.code == 200) {
                    withContext(Dispatchers.Main) {
                        if (currentPage == 1) {
                            adapter.setData(response.data.records)
                        } else {
                            adapter.addData(response.data.records)
                        }
                        swipeRefreshLayout.isRefreshing = false
                        isLoading = false
                        if (response.data.records.isEmpty()) {
                            isLastPage = true
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, response.msg, Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            }
        }
    }

    private fun loadMoreData() {
        currentPage++
        loadData()
    }
}
