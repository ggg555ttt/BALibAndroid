package cc.kivo.lib.android.ui.recommend.model

import androidx.databinding.DataBindingUtil
import cc.kivo.lib.android.R
import cc.kivo.lib.android.databinding.ItemJumpButtonBinding
import cc.kivo.lib.android.model.SearchBean
import cc.kivo.lib.android.ui.main.MainActivity
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder


/**
 * @ClassName RecJumpItem
 * @author ForeverDdB 835236331@qq.com
 * @Description
 * @createTime 2023年 06月12日 23:36
 **/
class RecJumpItem(
    val name: String,
    val searchBean: SearchBean,
    val activity: MainActivity
) : DslAdapterItem() {
    override var itemLayoutId = R.layout.item_jump_button
    init {
        itemData = name
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
        itemHolder.view(R.id.item_jump_button)?.let {
            DataBindingUtil.bind<ItemJumpButtonBinding>(it)?.apply {
                jumpButton.text = name
                jumpButton.setOnClickListener {
                    activity.searchByOption(searchBean)
                }
                invalidateAll()
            }
        }
    }
}