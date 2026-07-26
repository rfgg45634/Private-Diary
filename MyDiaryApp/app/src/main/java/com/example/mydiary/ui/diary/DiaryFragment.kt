package com.example.mydiary.ui.diary

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.mydiary.MyApplication
import com.example.mydiary.R
import com.example.mydiary.databinding.FragmentDiaryBinding
import com.example.mydiary.ui.edit.EditActivity
import com.example.mydiary.ui.recycle.RecycleBinActivity

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryViewModel by viewModels {
        DiaryViewModelFactory((requireActivity().application as MyApplication).repository)
    }

    private lateinit var adapter: DiaryAdapter
    private var isFirstResume = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiaryAdapter { note ->
            startActivity(EditActivity.newIntent(requireContext(), note.id))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val touchHelper = ItemTouchHelper(
            DiarySwipeCallback { position ->
                handleSwipeDelete(position)
            }
        )
        touchHelper.attachToRecyclerView(binding.recyclerView)

        // 正常浏览模式下的分页内容
        viewModel.pageItems.observe(viewLifecycleOwner) { items ->
            if (viewModel.isSearchingValue()) return@observe
            adapter.submitList(items)
            updateEmptyView(items.isEmpty())
        }
        // 搜索模式下的结果
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (!viewModel.isSearchingValue()) return@observe
            adapter.submitList(results)
            updateEmptyView(results.isEmpty())
        }
        viewModel.isSearching.observe(viewLifecycleOwner) { searching ->
            binding.pageBar.visibility = if (searching) View.GONE else View.VISIBLE
            if (searching) {
                adapter.submitList(viewModel.searchResults.value.orEmpty())
                updateEmptyView(viewModel.searchResults.value.orEmpty().isEmpty())
            } else {
                adapter.submitList(viewModel.pageItems.value.orEmpty())
                updateEmptyView(viewModel.pageItems.value.orEmpty().isEmpty())
            }
        }

        viewModel.currentPage.observe(viewLifecycleOwner) { updatePageLabel() }
        viewModel.totalPages.observe(viewLifecycleOwner) { updatePageLabel() }

        binding.btnPrev.setOnClickListener { viewModel.prevPage() }
        binding.btnNext.setOnClickListener { viewModel.nextPage() }

        // 切换日记排序方式：最新在前 / 最早在前
        binding.btnSortOrder.setOnClickListener {
            viewModel.toggleSortOrder()
        }
        viewModel.sortAscending.observe(viewLifecycleOwner) { ascending ->
            // 用同一个图标翻转 180 度表示方向变化，不用额外多切一套图标资源
            binding.btnSortOrder.rotation = if (ascending) 180f else 0f
        }

        // 连续记录天数
        viewModel.streakDays.observe(viewLifecycleOwner) { days ->
            if (days >= 2) {
                binding.streakText.visibility = View.VISIBLE
                binding.streakText.text = getString(R.string.streak_format, days)
            } else {
                binding.streakText.visibility = View.GONE
            }
        }

        binding.fabAdd.setOnClickListener {
            viewModel.addNewDiary { id ->
                startActivity(EditActivity.newIntent(requireContext(), id))
            }
        }

        // 切换日夜间模式
        binding.btnThemeToggle.setOnClickListener {
            toggleNightMode()
        }

        // 打开回收站
        binding.btnRecycleBin.setOnClickListener {
            startActivity(RecycleBinActivity.newIntent(requireContext()))
        }

        setupSearch()
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            val showing = binding.searchBar.visibility == View.VISIBLE
            if (showing) {
                closeSearch()
            } else {
                binding.searchBar.visibility = View.VISIBLE
                binding.searchInput.requestFocus()
            }
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearch()
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun closeSearch() {
        binding.searchInput.setText("")
        binding.searchBar.visibility = View.GONE
        viewModel.clearSearch()
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (!isEmpty) {
            binding.emptyView.visibility = View.GONE
            return
        }
        binding.emptyView.visibility = View.VISIBLE
        val searching = viewModel.isSearchingValue()
        binding.emptyTitle.text = if (searching) {
            getString(R.string.search_no_result_title)
        } else {
            getString(R.string.diary_empty_title)
        }
        binding.emptyDesc.text = if (searching) {
            getString(R.string.search_no_result_desc)
        } else {
            getString(R.string.diary_empty_desc)
        }
    }

    private fun handleSwipeDelete(position: Int) {
        val note = adapter.getItemAt(position) ?: return
        adapter.removeAt(position)
        viewModel.moveToRecycleBin(note)
        Snackbar.make(binding.root, R.string.swipe_deleted_tip, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                viewModel.restore(note)
            }
            .show()
    }

    private fun toggleNightMode() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        prefs.edit().putInt("night_mode", newMode).apply()
        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
        } else {
            viewModel.refresh()
        }
    }

    private fun updatePageLabel() {
        val page = viewModel.currentPage.value ?: 1
        val total = viewModel.totalPages.value ?: 1
        binding.pageLabel.text = getString(R.string.page_label_format, page, total)
        binding.btnPrev.isEnabled = page > 1
        binding.btnNext.isEnabled = page < total
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
