package com.example.mydiary.ui.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mydiary.MyApplication
import com.example.mydiary.R
import com.example.mydiary.data.NoteType
import com.example.mydiary.databinding.ActivityEditBinding
import com.example.mydiary.util.DateUtils

class EditActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"
        private const val AUTO_SAVE_DELAY_MS = 600L

        // 滑动超过这个像素值才切换成小标题，避免刚好停在顶部边缘时来回跳动
        private const val SCROLL_TOP_THRESHOLD = 4

        // 用两个几乎不会被正常输入打出来的控制字符，标记"这一段需要居中"，
        // 这样纯文本存储也能保留居中格式，编辑框里完全看不到任何多余符号
        private const val CENTER_MARK_START = '\u0001'
        private const val CENTER_MARK_END = '\u0002'

        // 兼容之前测试版本里用可见的 "[c]" 前缀标记居中段落的旧数据，
        // 打开时自动识别并转换成新的居中格式，不会让老内容显示异常
        private const val LEGACY_CENTER_PREFIX = "[c]"

        fun newIntent(context: Context, noteId: Long): Intent {
            return Intent(context, EditActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
        }
    }

    private data class HeadingPosition(val text: String, val top: Int)

    private lateinit var binding: ActivityEditBinding
    private val viewModel: EditNoteViewModel by viewModels {
        EditNoteViewModelFactory((application as MyApplication).repository)
    }

    private var noteId: Long = -1L
    private val saveHandler = Handler(Looper.getMainLooper())
    private var saveRunnable: Runnable? = null
    private var isLoaded = false
    private var headingPositions: List<HeadingPosition> = emptyList()

    // ---------- 正文内搜索 ----------
    private var searchMatches: List<IntRange> = emptyList()
    private var currentMatchIndex: Int = -1
    private val highlightSpans = mutableListOf<BackgroundColorSpan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId == -1L) {
            finish()
            return
        }

        viewModel.load(noteId)
        viewModel.note.observe(this) { note ->
            if (note == null) return@observe

            // 顶部导航栏中间显示模块图标 + 当前标题，方便一眼确认自己在哪个模块的哪条内容里
            binding.toolbarTypeIcon.setImageResource(
                if (note.type == NoteType.KNOWLEDGE) R.drawable.ic_knowledge else R.drawable.ic_diary
            )
            // 编辑页背景跟随模块：知识用绿色背景，日记用金色背景
            binding.root.setBackgroundResource(
                if (note.type == NoteType.KNOWLEDGE) R.drawable.bg_knowledge_page else R.drawable.bg_diary_page
            )
            updateToolbarTitle()

            if (isLoaded) return@observe
            isLoaded = true
            binding.titleEdit.setText(note.title ?: "")
            binding.contentEdit.setText(deserializeContent(note.content))
            binding.titleEdit.hint = DateUtils.formatFull(note.createdTime)
            refreshHeadingPositions()
            updateWordCount()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scheduleSave()
                updateToolbarTitle()
                refreshHeadingPositions()
                updateWordCount()
                // 正文内搜索开着的时候，正文一变，高亮位置就可能对不上了，重新搜一遍保持同步
                if (binding.contentSearchBar.visibility == View.VISIBLE) {
                    performContentSearch(binding.contentSearchInput.text?.toString().orEmpty())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.titleEdit.addTextChangedListener(watcher)
        binding.contentEdit.addTextChangedListener(watcher)

        binding.scrollContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            updateSectionIndicator(scrollY)
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.edit_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search_content -> {
                    toggleContentSearch()
                    true
                }
                R.id.action_center -> {
                    toggleCenterAlignment()
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }

        setupContentSearch()
    }

    // ---------- 正文内搜索 ----------

    private fun setupContentSearch() {
        binding.btnCloseContentSearch.setOnClickListener { closeContentSearch() }

        binding.contentSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performContentSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSearchNext.setOnClickListener { jumpToMatch(currentMatchIndex + 1) }
        binding.btnSearchPrev.setOnClickListener { jumpToMatch(currentMatchIndex - 1) }
    }

    private fun toggleContentSearch() {
        val showing = binding.contentSearchBar.visibility == View.VISIBLE
        if (showing) {
            closeContentSearch()
        } else {
            binding.contentSearchBar.visibility = View.VISIBLE
            binding.contentSearchInput.requestFocus()
        }
    }

    private fun closeContentSearch() {
        binding.contentSearchInput.setText("")
        binding.contentSearchBar.visibility = View.GONE
        clearHighlights()
    }

    private fun clearHighlights() {
        val editable = binding.contentEdit.text
        if (editable != null) {
            highlightSpans.forEach { editable.removeSpan(it) }
        }
        highlightSpans.clear()
        searchMatches = emptyList()
        currentMatchIndex = -1
        binding.contentSearchCount.text = ""
    }

    private fun performContentSearch(query: String) {
        val editable = binding.contentEdit.text ?: return
        highlightSpans.forEach { editable.removeSpan(it) }
        highlightSpans.clear()

        if (query.isBlank()) {
            searchMatches = emptyList()
            currentMatchIndex = -1
            binding.contentSearchCount.text = ""
            return
        }

        val text = editable.toString()
        val matches = mutableListOf<IntRange>()
        var idx = text.indexOf(query, 0, ignoreCase = true)
        while (idx >= 0) {
            matches.add(idx until (idx + query.length))
            idx = text.indexOf(query, idx + query.length, ignoreCase = true)
        }
        searchMatches = matches

        if (matches.isEmpty()) {
            currentMatchIndex = -1
            binding.contentSearchCount.text = getString(R.string.content_search_no_match)
            return
        }

        val normalColor = ContextCompat.getColor(this, R.color.search_highlight)
        matches.forEach { range ->
            val span = BackgroundColorSpan(normalColor)
            editable.setSpan(span, range.first, range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            highlightSpans.add(span)
        }
        jumpToMatch(0)
    }

    private fun jumpToMatch(index: Int) {
        if (searchMatches.isEmpty()) return
        val safeIndex = ((index % searchMatches.size) + searchMatches.size) % searchMatches.size
        currentMatchIndex = safeIndex
        binding.contentSearchCount.text = getString(
            R.string.content_search_count_format,
            safeIndex + 1,
            searchMatches.size
        )

        // 把当前匹配项的高亮换成更醒目的颜色，其余的保持普通高亮
        val editable = binding.contentEdit.text
        val currentColor = ContextCompat.getColor(this, R.color.search_highlight_current)
        val normalColor = ContextCompat.getColor(this, R.color.search_highlight)
        if (editable != null) {
            highlightSpans.forEachIndexed { i, span ->
                editable.removeSpan(span)
                val range = searchMatches[i]
                val newSpan = BackgroundColorSpan(if (i == safeIndex) currentColor else normalColor)
                editable.setSpan(newSpan, range.first, range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                highlightSpans[i] = newSpan
            }
        }

        // 滚动内容区域，让当前匹配项出现在视野里
        val layout = binding.contentEdit.layout
        if (layout != null) {
            val range = searchMatches[safeIndex]
            val line = layout.getLineForOffset(range.first)
            val top = layout.getLineTop(line) + binding.contentEdit.paddingTop
            binding.scrollContainer.smoothScrollTo(0, (top - 120).coerceAtLeast(0))
        }
    }

    /** 正文字数统计：跟随输入实时更新 */
    private fun updateWordCount() {
        val count = binding.contentEdit.text?.length ?: 0
        binding.wordCountText.text = getString(R.string.word_count_format, count)
    }

    /** 顶部导航栏中间的标题跟随输入实时同步；标题留空时用创建时间代替，跟卡片列表逻辑保持一致 */
    private fun updateToolbarTitle() {
        val titleText = binding.titleEdit.text?.toString()
        val createdTime = viewModel.note.value?.createdTime ?: System.currentTimeMillis()
        binding.toolbarTitle.text = DateUtils.displayTitle(titleText, createdTime)
    }

    /**
     * 重新扫描正文里所有被标记为"居中"的段落（也就是小标题），
     * 记录它们在正文里的竖直位置，用来判断滚动到哪一节了。
     * 必须等布局完成之后才能拿到准确的行位置，所以放进 post{} 里。
     */
    private fun refreshHeadingPositions() {
        binding.contentEdit.post {
            val editable = binding.contentEdit.text ?: return@post
            val layout = binding.contentEdit.layout ?: return@post
            val text = editable.toString()
            headingPositions = editable.getSpans(0, editable.length, AlignmentSpan::class.java)
                .filter { it.alignment == Layout.Alignment.ALIGN_CENTER }
                .mapNotNull { span ->
                    val start = editable.getSpanStart(span)
                    val end = editable.getSpanEnd(span)
                    if (start < 0 || end <= start) return@mapNotNull null
                    val headingText = text.substring(start, end).trim()
                    if (headingText.isEmpty()) return@mapNotNull null
                    val line = layout.getLineForOffset(start)
                    HeadingPosition(headingText, layout.getLineTop(line) + binding.contentEdit.paddingTop)
                }
                .sortedBy { it.top }
            updateSectionIndicator(binding.scrollContainer.scrollY)
        }
    }

    /**
     * 顶部（没滑动）时显示可编辑的标题输入框；
     * 一旦往下滑动进入某个小标题的范围，标题输入框隐藏，改成显示当前所属的小标题——
     * 两者占同一个位置，同一时间只显示一个，不是叠在一起的两行。
     */
    private fun updateSectionIndicator(scrollY: Int) {
        val current = if (scrollY <= SCROLL_TOP_THRESHOLD) null else headingPositions.lastOrNull { it.top <= scrollY }
        if (current == null) {
            binding.titleEdit.visibility = View.VISIBLE
            binding.sectionIndicator.visibility = View.GONE
        } else {
            binding.titleEdit.visibility = View.INVISIBLE
            binding.sectionIndicator.visibility = View.VISIBLE
            binding.sectionIndicator.text = current.text
        }
    }

    /** 把光标所在段落切换为居中/取消居中，光标位置不变，编辑框里不会出现任何标记文字 */
    private fun toggleCenterAlignment() {
        val editable = binding.contentEdit.text ?: return
        val text = editable.toString()
        val cursor = binding.contentEdit.selectionStart.coerceIn(0, text.length)

        val searchFrom = (cursor - 1).coerceAtLeast(0)
        val nlBefore = if (cursor == 0) -1 else text.lastIndexOf('\n', searchFrom)
        val paraStart = nlBefore + 1
        var paraEnd = text.indexOf('\n', cursor)
        if (paraEnd == -1) paraEnd = text.length
        if (paraStart > paraEnd) return

        val existingSpans = editable.getSpans(paraStart, paraEnd, AlignmentSpan::class.java)
        val alreadyCentered = existingSpans.any { it.alignment == Layout.Alignment.ALIGN_CENTER }
        existingSpans.forEach { editable.removeSpan(it) }

        if (!alreadyCentered) {
            editable.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                paraStart,
                paraEnd,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        scheduleSave()
        refreshHeadingPositions()
    }

    /** 把带居中格式的 Editable 序列化成纯文本存进数据库，居中段落前后包裹隐藏标记字符 */
    private fun serializeContent(editable: Editable): String {
        val text = editable.toString()
        val result = StringBuilder()
        var paraStart = 0
        while (true) {
            val nlIndex = text.indexOf('\n', paraStart)
            val paraEnd = if (nlIndex == -1) text.length else nlIndex
            val spans = editable.getSpans(paraStart, paraEnd, AlignmentSpan::class.java)
            val centered = spans.any { it.alignment == Layout.Alignment.ALIGN_CENTER }
            val paragraph = text.substring(paraStart, paraEnd)
            if (centered && paragraph.isNotEmpty()) {
                result.append(CENTER_MARK_START).append(paragraph).append(CENTER_MARK_END)
            } else {
                result.append(paragraph)
            }
            if (nlIndex == -1) break
            result.append('\n')
            paraStart = nlIndex + 1
        }
        return result.toString()
    }

    /**
     * 从存储的纯文本还原出带居中格式的 Spannable。
     * 同时兼容旧版本里用可见 "[c]" 前缀标记居中的段落——自动识别、去掉前缀、转换成新格式，
     * 下次保存后就会变成隐藏标记，老数据不会显示异常也不会丢失格式。
     */
    private fun deserializeContent(raw: String): CharSequence {
        val builder = SpannableStringBuilder()
        val lines = raw.split('\n')
        for ((index, line) in lines.withIndex()) {
            val hiddenMarked = line.length >= 2 &&
                line.first() == CENTER_MARK_START &&
                line.last() == CENTER_MARK_END
            val legacyMarked = !hiddenMarked && line.startsWith(LEGACY_CENTER_PREFIX)

            val clean = when {
                hiddenMarked -> line.substring(1, line.length - 1)
                legacyMarked -> line.removePrefix(LEGACY_CENTER_PREFIX)
                else -> line
            }

            val start = builder.length
            builder.append(clean)
            if (hiddenMarked || legacyMarked) {
                builder.setSpan(
                    AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                    start,
                    builder.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
            if (index != lines.lastIndex) builder.append('\n')
        }
        return builder
    }

    private fun scheduleSave() {
        saveRunnable?.let { saveHandler.removeCallbacks(it) }
        val runnable = Runnable { persistNow() }
        saveRunnable = runnable
        saveHandler.postDelayed(runnable, AUTO_SAVE_DELAY_MS)
    }

    private fun persistNow() {
        val content = binding.contentEdit.text?.let { serializeContent(it) } ?: ""
        viewModel.save(noteId, binding.titleEdit.text?.toString(), content)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.note.value?.let { note ->
                    viewModel.delete(note) { finish() }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        saveRunnable?.let { saveHandler.removeCallbacks(it) }

        val title = binding.titleEdit.text?.toString()?.trim().orEmpty()
        val content = binding.contentEdit.text?.let { serializeContent(it) }.orEmpty()

        // 只有在真正离开这个页面（而不是短暂切到后台）且标题正文都是空的情况下，
        // 才会把这条从没填过内容的记录直接丢弃，不产生一个空白文件。
        // 如果只是临时切到后台（isFinishing 还是 false），照常保存，避免用户回来时输入丢失。
        if (isFinishing && title.isEmpty() && content.isEmpty()) {
            viewModel.discardIfEmpty(noteId)
        } else {
            viewModel.save(noteId, title, content)
        }
    }
}
