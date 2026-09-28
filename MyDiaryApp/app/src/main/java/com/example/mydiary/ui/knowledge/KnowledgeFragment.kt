package com.example.mydiary.ui.knowledge

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
import com.example.mydiary.databinding.FragmentKnowledgeBinding
import com.example.mydiary.ui.edit.EditActivity
import com.example.mydiary.ui.recycle.RecycleBinActivity

class KnowledgeFragment : Fragment() {

    private var _binding: FragmentKnowledgeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KnowledgeViewModel by viewModels {
        KnowledgeViewModelFactory((requireActivity().application as MyApplication).repository)
    }

    private lateinit var adapter: KnowledgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnowledgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = KnowledgeAdapter(
            onItemClick = { note ->
                startActivity(EditActivity.newIntent(requireContext(), note.id))
            },
            onDragFinished = { newList ->
                viewModel.reorder(newList)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val touchHelper = ItemTouchHelper(
            KnowledgeDragCallback(adapter) { position ->
                handleSwipeDelete(position)
            }
        )
        touchHelper.attachToRecyclerView(binding.recyclerView)
        adapter.itemTouchHelper = touchHelper

        viewModel.knowledgeItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            updateEmptyView(items.isEmpty())
        }

        binding.fabAdd.setOnClickListener {
            viewModel.addNewKnowledge { id ->
                startActivity(EditActivity.newIntent(requireContext(), id))
            }
        }

        // 切换日夜间模式
        binding.btnThemeToggle.setOnClickListener {
            toggleNightMode()
        }
        updateThemeIcon()

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
        viewModel.setSearchQuery("")
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (!isEmpty) {
            binding.emptyView.visibility = View.GONE
            return
        }
        binding.emptyView.visibility = View.VISIBLE
        val searching = binding.searchInput.text?.isNotBlank() == true
        binding.emptyTitle.text = if (searching) {
            getString(R.string.search_no_result_title)
        } else {
            getString(R.string.knowledge_empty_title)
        }
        binding.emptyDesc.text = if (searching) {
            getString(R.string.search_no_result_desc)
        } else {
            getString(R.string.knowledge_empty_desc)
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

    /** 夜间模式下显示太阳图标（提示可切回白天），白天显示月牙图标 */
    private fun updateThemeIcon() {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.btnThemeToggle.setImageResource(
            if (isNight) R.drawable.ic_sun else R.drawable.ic_theme_toggle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
