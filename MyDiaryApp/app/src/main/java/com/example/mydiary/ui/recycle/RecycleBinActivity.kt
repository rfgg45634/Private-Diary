package com.example.mydiary.ui.recycle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydiary.MyApplication
import com.example.mydiary.R
import com.example.mydiary.databinding.ActivityRecycleBinBinding

class RecycleBinActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, RecycleBinActivity::class.java)
        }
    }

    private lateinit var binding: ActivityRecycleBinBinding
    private val viewModel: RecycleBinViewModel by viewModels {
        RecycleBinViewModelFactory((application as MyApplication).repository)
    }

    private lateinit var adapter: RecycleBinAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecycleBinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 导航栏返回
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 初始化列表
        adapter = RecycleBinAdapter { note ->
            showOptionsDialog(note)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 观察逻辑删除卡片列表
        viewModel.deletedNotes.observe(this) { items ->
            adapter.submitList(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.btnClear.isEnabled = items.isNotEmpty()
        }

        // 清空回收站
        binding.btnClear.setOnClickListener {
            confirmClearAll()
        }

        // 启动时自动清理过期的垃圾数据（超过7天）
        viewModel.pruneExpired()
    }

    private fun showOptionsDialog(note: com.example.mydiary.data.NoteEntity) {
        val options = arrayOf(getString(R.string.restore), getString(R.string.delete_permanently))
        AlertDialog.Builder(this)
            .setTitle(R.string.recycle_bin_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.restore(note) // 恢复
                    1 -> confirmDeletePermanently(note) // 永久删除
                }
            }
            .show()
    }

    private fun confirmDeletePermanently(note: com.example.mydiary.data.NoteEntity) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_permanently_confirm_title)
            .setMessage(R.string.delete_permanently_confirm_message)
            .setPositiveButton(R.string.delete_permanently) { _, _ ->
                viewModel.deletePermanently(note)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.recycle_bin_clear_confirm_title)
            .setMessage(R.string.recycle_bin_clear_confirm_message)
            .setPositiveButton(R.string.recycle_bin_clear) { _, _ ->
                viewModel.emptyAll()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
