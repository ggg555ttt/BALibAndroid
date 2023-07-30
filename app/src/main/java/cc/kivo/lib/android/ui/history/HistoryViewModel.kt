package cc.kivo.lib.android.ui.history

import androidx.lifecycle.viewModelScope
import cc.kivo.lib.android.model.PageSelectorViewModel
import cc.kivo.lib.android.model.db.ArtInfo
import cc.kivo.lib.android.ui.home.model.ArticleInfoItem
import com.angcyo.dsladapter.DslAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : PageSelectorViewModel() {

    private val articleData = MutableStateFlow(listOf<ArtInfo>())

    val articleDataAdapter = DslAdapter()

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
    }

    fun loadArticles(data: MutableList<ArtInfo>) {
        viewModelScope.launch {
            articleData.emit(data)
        }
    }

}