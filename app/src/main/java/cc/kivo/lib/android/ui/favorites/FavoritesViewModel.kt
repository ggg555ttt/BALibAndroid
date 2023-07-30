package cc.kivo.lib.android.ui.favorites

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.kivo.lib.android.model.PageSelectorViewModel
import cc.kivo.lib.android.model.db.ArtInfo
import cc.kivo.lib.android.ui.home.model.ArticleInfoItem
import cc.kivo.lib.android.ui.home.model.PageItem
import com.angcyo.dsladapter.DslAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel : PageSelectorViewModel() {

    private val articleData = MutableStateFlow(listOf<ArtInfo>())

    val currentPage = MutableLiveData(1)

    val articleDataAdapter = DslAdapter()

    val pageSelectorAdapter = DslAdapter()

    val pageData = MutableStateFlow(listOf<Int>())

    val pageLen = MutableLiveData(1)

    init {
        viewModelScope.launch {
            articleData.collect {
                articleDataAdapter.changeDataItems { adapterItems ->
                    adapterItems.clear()
                    it.forEach {
                        adapterItems.add(ArticleInfoItem(it))
                    }
                }
            }
        }
        viewModelScope.launch {
            pageData.collect {
                pageSelectorAdapter.changeDataItems { adapterItems ->
                    adapterItems.clear()
                    it.forEach {
                        adapterItems.add(PageItem(it, this@FavoritesViewModel))
                    }
                }
                pageSelectorAdapter.notifyDataChanged()
            }
        }
    }

    fun loadArticles(data: MutableList<ArtInfo>) {
        viewModelScope.launch {
            articleData.emit(data)
        }
    }

}