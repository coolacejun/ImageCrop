package ace.jun.imagecroper

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CropRepo @Inject constructor() {
    val cropImage = MutableStateFlow(Uri.EMPTY)

    companion object {
        private const val PAGE_SIZE = 30
    }
}