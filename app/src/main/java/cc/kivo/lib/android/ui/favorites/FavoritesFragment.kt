package cc.kivo.lib.android.ui.favorites

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import cc.kivo.lib.android.R
import cc.kivo.lib.android.databinding.FragmentFavoritesBinding
import cc.kivo.lib.android.model.DataBaseHandler
import cc.kivo.lib.android.model.MyBaseActivity
import cc.kivo.lib.android.model.db.ArtInfo
import cc.kivo.lib.android.model.db.ArtInfoDao
import cc.kivo.lib.android.model.db.DaoSession
import cc.kivo.lib.android.ui.main.DatabaseCopyThread
import cc.kivo.lib.android.ui.main.MainActivity
import cc.kivo.lib.android.util.ContentUriUtil
import cc.kivo.lib.android.util.FavoriteArticleUtil
import cc.kivo.lib.android.util.PageSizeUtil
import cc.kivo.lib.android.util.SettingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.liangguo.androidkit.app.ToastUtil
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

@SuppressLint("InflateParams")
class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null

    private val binding get() = _binding!!

    private lateinit var _favoritesViewModel: FavoritesViewModel

    private val favoritesViewModel get() = _favoritesViewModel

    private var daoSession: DaoSession? = null

    private var pageData = mutableListOf<Int>()

    private var isShowing = false

    private val activityResultLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            try {
                MaterialAlertDialogBuilder(
                    requireContext()
                ).setTitle(getString(R.string.import_favorites))
                    .setMessage(getString(R.string.import_hint))
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        try {
                            val file = File(ContentUriUtil.getAbsolutePath(requireContext(), it))
                            val fileReader = FileInputStream(file).bufferedReader()
                            val text = fileReader.readText()
                            JSONArray(text)
                            FavoriteArticleUtil.getSharedPreferences().edit()
                                .putString(FavoriteArticleUtil.fileName, text).apply()
                            ToastUtil.success(getString(R.string.import_success))
                            favoritesViewModel.currentPage.value = 1
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (e is JSONException) {
                                ToastUtil.error(getString(R.string.import_fail) + getString(R.string.import_format_fail))
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtil.error(getString(R.string.import_fail))
            }
        }

    private val mPageSelectorDialog by lazy {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_page_selector, null)
        val pageSizeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(10, 20, 30, 50)
        )
        view.findViewById<RecyclerView>(R.id.page_recycler_view)?.adapter =
            favoritesViewModel.pageSelectorAdapter
        val inputText = view.findViewById<TextInputEditText>(R.id.page_jumper_input_text)
        inputText.doOnTextChanged { text, start, _, count ->
            if (text.toString().isNotBlank()) {
                val page = text.toString().toInt()
                if (page > favoritesViewModel.pageLen.value!!) {
                    inputText.setText(text.toString().replaceRange(start, start + count, ""))
                }
            }
        }
        inputText.setOnEditorActionListener { textView, i, _ ->
            if (i == EditorInfo.IME_ACTION_NEXT) {
                val page = textView.text.toString().toInt()
                favoritesViewModel.currentPage.postValue(page)
                with(favoritesViewModel) {
                    checkedButton.value?.isChecked = false
                    checkedButton.value = null
                    //清除数组再重新记录状态
                    favoritesViewModel.checkedList.clear()
                    for (n in 0 until favoritesViewModel.pageLen.value!!) {
                        if (n + 1 != favoritesViewModel.currentPage.value) favoritesViewModel.checkedList.add(
                            false
                        )
                        else favoritesViewModel.checkedList.add(true)
                    }
                    checkedList[page - 1] = true
                    selectedPage.value = page
                }
                inputText.setText("")
                isShowing = true
            }
            false
        }
        val pageSizeSelector =
            view.findViewById<AutoCompleteTextView>(R.id.page_size_selector_input)
        pageSizeSelector.setAdapter(pageSizeAdapter)
        pageSizeSelector.hint =
            "${getString(R.string.page_size_selector_label)}-当前：${PageSizeUtil.getSize()}"
        pageSizeSelector.setOnItemClickListener { adapterView, _, i, _ ->
            PageSizeUtil.setSize(adapterView.adapter.getItem(i).toString().toInt())
            favoritesViewModel.currentPage.postValue(1)
            isShowing = true
        }
        MaterialAlertDialogBuilder(
            requireActivity(),
            com.google.android.material.R.style.MaterialAlertDialog_Material3
        )
            .setTitle("${getString(R.string.select)}${getString(R.string.page_num)}")
            .setPositiveButton(
                R.string.confirm,
            ) { _, _ ->
                favoritesViewModel.currentPage.postValue(favoritesViewModel.selectedPage.value)
            }
            .setNegativeButton(R.string.cancel, null)
            .setView(view)
            .create()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _favoritesViewModel =
            ViewModelProvider(this)[FavoritesViewModel::class.java]

        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        favoritesViewModel.currentPage.observe(viewLifecycleOwner) {
            favoritesViewModel.checkedList.clear()
            for (i in 0 until favoritesViewModel.pageLen.value!!) {
                if (i + 1 != favoritesViewModel.currentPage.value) favoritesViewModel.checkedList.add(
                    false
                )
                else favoritesViewModel.checkedList.add(true)
            }
            if (isShowing) {
                mPageSelectorDialog.hide()
                isShowing = false
            }
            loadArticles(
                it,
                PageSizeUtil.getSize()
            )
            binding.lastPageBtn.isEnabled = it > 1
            binding.nextPageBtn.isEnabled = it < favoritesViewModel.pageLen.value!!
            binding.pageNumBtn.text = "$it/${favoritesViewModel.pageLen.value} 页"
        }

        with(favoritesViewModel) {
            viewModelScope.launch {
                pageLen.observe(viewLifecycleOwner) {
                    viewModelScope.launch {
                        favoritesViewModel.checkedList.clear()
                        for (i in 0 until it + 1) {
                            if (i + 1 != favoritesViewModel.currentPage.value) favoritesViewModel.checkedList.add(
                                false
                            )
                            else favoritesViewModel.checkedList.add(true)
                        }
                        this@FavoritesFragment.pageData = mutableListOf()
                        for (i in 1 until it + 1) {
                            this@FavoritesFragment.pageData.add(i)
                        }
                        pageData.emit(this@FavoritesFragment.pageData)
                    }
                }
            }
        }

        initViews()

        return root
    }

    private fun initViews() {
        with(binding) {
            nextPageBtn.setOnClickListener {
                favoritesViewModel.currentPage.postValue(favoritesViewModel.currentPage.value!! + 1)
            }
            lastPageBtn.setOnClickListener {
                favoritesViewModel.currentPage.postValue(favoritesViewModel.currentPage.value!! - 1)
            }
            recyclerView.adapter = favoritesViewModel.articleDataAdapter
            pageNumBtn.setOnClickListener { mPageSelectorDialog.show() }

            layout.apply {
                val uri = SettingUtil.getImageBackground(SettingUtil.INDEX_BG)
                if (null != uri) background = Drawable.createFromPath(uri.path)
            }
            exportBtn.setOnClickListener {
                XXPermissions.with(requireContext())
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                FavoriteArticleUtil.exportFavorites(activity as MyBaseActivity)
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                ToastUtil.error("被永久拒绝授权，请手动授予读写手机储存权限")
                                XXPermissions.startPermissionActivity(context, permissions)
                            } else {
                                ToastUtil.error("获取读写手机储存权限失败")
                            }
                        }
                    })
            }
            importBtn.setOnClickListener {
                XXPermissions.with(requireContext())
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                activityResultLauncher.launch("application/json")
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                ToastUtil.error("被永久拒绝授权，请手动授予读写手机储存权限")
                                XXPermissions.startPermissionActivity(context, permissions)
                            } else {
                                ToastUtil.error("获取读写手机储存权限失败")
                            }
                        }
                    })
            }
        }
    }

    private fun loadArticles(page: Int?, pageSize: Int) {
        val artList = FavoriteArticleUtil.getFavorites()
        if (artList.length() < 1) {
            ToastUtil.info(getString(R.string.no_data))
        }
        favoritesViewModel.pageLen.value = artList.length() / pageSize + 1
        val handler = DataBaseHandler(activity as MyBaseActivity) {
            daoSession = it.obj as DaoSession
            if (null != daoSession) {
                val artInfoDao: ArtInfoDao = daoSession!!.artInfoDao
                val offset = if (page != null) {
                    (page - 1) * pageSize
                } else {
                    0
                }
                val list = mutableListOf<JSONObject>()
                val lastIndex =
                    if (offset + pageSize > artList.length()) artList.length() else offset + pageSize
                for (i in offset until lastIndex) {
                    list.add(artList.getJSONObject(i))
                }
                val data = mutableListOf<ArtInfo>()
                list.forEach { json ->
                    val art = artInfoDao.queryBuilder().where(
                        ArtInfoDao.Properties.Name.eq(json.getString("name")),
                        ArtInfoDao.Properties.Author.eq(json.getString("author")),
                        ArtInfoDao.Properties.Translator.eq(json.getString("translator")),
                    ).unique()
                    if (null != art) data.add(art)
                }
                data.reverse()
                favoritesViewModel.loadArticles(data)
            }
        }
        (activity as MainActivity).shapeLoadingDialog?.show()
        DatabaseCopyThread.addHandler(handler)
    }

    override fun onResume() {
        super.onResume()
        loadArticles(
            favoritesViewModel.currentPage.value,
            PageSizeUtil.getSize()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}