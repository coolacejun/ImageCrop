package ace.jun.imagecroper.ui

import ace.jun.imagecroper.CropRepo
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class ImageChooseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cropRepo: CropRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _cropImageUri = cropRepo.cropImage
    val cropImageUri: StateFlow<Uri> = _cropImageUri
}