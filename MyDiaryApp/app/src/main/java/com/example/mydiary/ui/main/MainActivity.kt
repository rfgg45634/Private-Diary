package com.example.mydiary.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mydiary.R
import com.example.mydiary.databinding.ActivityMainBinding
import com.example.mydiary.ui.diary.DiaryFragment
import com.example.mydiary.ui.knowledge.KnowledgeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 防止"点底部导航切页"和"滑动切页"两个事件源互相触发造成死循环
    private var isSyncingFromPager = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) KnowledgeFragment() else DiaryFragment()
            }
        }
        // 两个页面都保留在内存里，滑动切换时不用每次重新创建 Fragment
        binding.viewPager.offscreenPageLimit = 1

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                isSyncingFromPager = true
                binding.bottomNav.selectedItemId =
                    if (position == 0) R.id.knowledgeFragment else R.id.diaryFragment
                isSyncingFromPager = false
            }
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (!isSyncingFromPager) {
                val index = if (item.itemId == R.id.knowledgeFragment) 0 else 1
                binding.viewPager.setCurrentItem(index, true)
            }
            true
        }
    }
}
