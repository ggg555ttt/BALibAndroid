package cc.kivo.lib.android.ui.recommend

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.kivo.lib.android.R
import cc.kivo.lib.android.databinding.FragmentRecommendBinding
import cc.kivo.lib.android.model.DataBaseHandler
import cc.kivo.lib.android.model.db.DaoSession
import cc.kivo.lib.android.model.db.Dict
import cc.kivo.lib.android.model.db.DictDao
import cc.kivo.lib.android.model.db.Rec
import cc.kivo.lib.android.model.db.RecDao
import cc.kivo.lib.android.ui.main.DatabaseCopyThread
import cc.kivo.lib.android.ui.main.MainActivity
import cc.kivo.lib.android.ui.recommend.model.RecInfo
import cc.kivo.lib.android.util.SettingUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.liangguo.androidkit.app.ToastUtil
import org.greenrobot.greendao.query.QueryBuilder

val typeMap = listOf(
    listOf(0, 1),
    listOf(2, 3),
    listOf(4),
    listOf(5)
)

@SuppressLint("InflateParams")
class RecommendFragment : Fragment() {

    private var _binding: FragmentRecommendBinding? = null

    private val binding get() = _binding!!

    private var _recommendViewModel: RecommendViewModel? = null

    private val recommendViewModel get() = _recommendViewModel!!

    private var daoSession: DaoSession? = null

    @SuppressLint("SetTextI18n", "UseRequireInsteadOfGet")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _recommendViewModel =
            ViewModelProvider(this)[RecommendViewModel::class.java]
        recommendViewModel.activity = activity as MainActivity

        _binding = FragmentRecommendBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initView()

        with(recommendViewModel) {
            type.observe(viewLifecycleOwner) {
                loadRec(it)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initView() {
        with(binding) {
            layout.apply {
                val uri = SettingUtil.getImageBackground(SettingUtil.INDEX_BG)
                if (null != uri) background = Drawable.createFromPath(uri.path)
            }
            recTab.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    recommendViewModel.type.value = tab?.position
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            recyclerView.adapter = recommendViewModel.recDataAdapter
        }
    }

    /**
     * 查询推荐详情
     */
    @SuppressLint("SetTextI18n")
    private fun loadRec(type: Int) {
        val handler = DataBaseHandler(activity as MainActivity) {
            daoSession = it.obj as DaoSession
            val count: Long
            if (null != daoSession) {
                val recDao: RecDao = daoSession!!.recDao

                val query: QueryBuilder<Rec> = recDao.queryBuilder()
                query.where(RecDao.Properties.Type.`in`(typeMap[type]))

                count = query.count()
                if (count == 0L) {
                    ToastUtil.info(getString(R.string.no_data))
                }

                // 重置保存列表渲染状态的数组
                recommendViewModel.collapsedList.clear()
                recommendViewModel.notShowJumpButtonList.clear()
                for (i in 0 until count) {
                    recommendViewModel.collapsedList.add(true)
                    recommendViewModel.notShowJumpButtonList.add(false)
                }

                val list = query.build().listLazy()
                val map = mutableMapOf<Long, RecInfo>()
                list.map { rec ->
                    if (map.containsKey(rec.refId)) {
                        map[rec.refId]?.data = map[rec.refId]?.data?.apply {
                            add(rec)
                        } ?: mutableListOf(rec)
                    } else {
                        val recInfo = RecInfo.fromRec(rec, mutableListOf(rec), type)
                        map[rec.refId] = recInfo
                    }
                }

                val data = mutableListOf<RecInfo>()
                map.forEach { (_, u) ->
                    data.add(u)
                }
                recommendViewModel.loadRecs(data)
                list.close()
            } else {
                recommendViewModel.loadRecs(null)
            }
        }
        (activity as MainActivity).shapeLoadingDialog?.show()
        DatabaseCopyThread.addHandler(handler)
    }

    /**
     * 查询解释字典
     */
    @SuppressLint("SetTextI18n")
    private fun loadDict() {
        val handler = DataBaseHandler(activity as MainActivity) {
            daoSession = it.obj as DaoSession
            val count: Long
            if (null != daoSession) {
                val dictDao: DictDao = daoSession!!.dictDao
                val query: QueryBuilder<Dict> = dictDao.queryBuilder()

                count = query.count()
                if (count == 0L) {
                    ToastUtil.info(getString(R.string.no_data))
                }
                val list = query.build().listLazy()

//                recommendViewModel.loadArticles(list)
                list.close()
            } else {
                recommendViewModel.loadRecs(null)
            }
        }
        (activity as MainActivity).shapeLoadingDialog?.show()
        DatabaseCopyThread.addHandler(handler)
    }

    override fun onResume() {
        super.onResume()
        // 恢复选中的tab
        binding.recTab.selectTab(binding.recTab.getTabAt(recommendViewModel.type.value!!))
    }
}