package cn.umafan.lib.android.ui.home.model

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import cn.umafan.lib.android.R
import cn.umafan.lib.android.beans.ArtInfo
import cn.umafan.lib.android.databinding.ItemArticleCardBinding
import cn.umafan.lib.android.model.MyApplication
import cn.umafan.lib.android.ui.reader.ReaderActivity
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.google.android.material.snackbar.Snackbar
import com.liangguo.androidkit.app.startNewActivity
import java.text.SimpleDateFormat
import java.util.*

class ArticleInfoItem(
    private val articleInfo: ArtInfo
) : DslAdapterItem() {
    override var itemLayoutId = R.layout.item_article_card
    private val timeStampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    init {
        itemData = articleInfo
        thisAreContentsTheSame = { fromItem, newItem, _, _ ->
            fromItem?.itemData == newItem.itemData
        }
        thisAreItemsTheSame = { fromItem, newItem, _, _ ->
            fromItem?.itemData == newItem.itemData
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.view(R.id.item_article_card)?.let {
            DataBindingUtil.bind<ItemArticleCardBinding>(it)?.apply {
                articleName.text = articleInfo.name
                articleNote.text = articleInfo.note
                articleAuthor.text = articleInfo.author
                articleTranslator.text = String.format("译者：%s", articleInfo.translator)
                articleUploadTime.text = articleInfo.uploadTime.let { date ->
                    timeStampFormatter.format(date.toLong() * 1000)
                }
                articleTags.text =
                    articleInfo.taggedList.map { tagged -> tagged.tag }
                        .sortedWith { a, b ->
                            if (a.type == b.type)
                                a.name.compareTo(b.name)
                            else
                                b.type.compareTo(a.type)
                        }.joinToString("，") { tag -> tag.name }
                itemArticleCardBox.setOnClickListener { view ->
                    ReaderActivity::class.startNewActivity() {
                        putExtra("id", articleInfo.id.toInt())
                    }
                    Snackbar.make(
                        view,
                        "作品[${articleInfo.id}] ${articleInfo.name}",
                        Snackbar.LENGTH_SHORT
                    )
                        .setAction("Action", null).show()
                }
                invalidateAll()
            }
        }
    }
}