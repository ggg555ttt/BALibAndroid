package cc.kivo.lib.android.ui.reader

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.kivo.lib.android.util.ReaderSettingUtil


/**
 * @ClassName ReaderViewModel
 * @author Forever-DdB everddb@gmail.com
 * @Description
 * @createTime 2022年 08月07日 19:36
 **/
class ReaderViewModel : ViewModel() {
    val collected = MutableLiveData(true)
    val fontSize: MutableLiveData<String>
    val segmentSpace: MutableLiveData<String>

    init {
        val setting = ReaderSettingUtil.getSetting("default")
        fontSize = MutableLiveData(setting.getString("fontSize"))
        segmentSpace = MutableLiveData(setting.getString("segmentSpace"))
    }
}