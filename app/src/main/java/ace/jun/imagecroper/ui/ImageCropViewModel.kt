package ace.jun.imagecroper.ui

import ace.jun.imagecroper.CropRepo
import ace.jun.imagecroper.JLog.logD
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.P
import android.provider.MediaStore
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject


object ImageCropArgs {
    const val IMAGE_URI = "IMAGE_URI"
}

@HiltViewModel
class ImageCropViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cropRepo: CropRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val imageUriArg = savedStateHandle.get<String>(ImageCropArgs.IMAGE_URI) ?: ""

    private val imageUri = MutableStateFlow(Uri.parse(imageUriArg))
    private val imageSize = MutableStateFlow(Size(0F, 0F))

    private val top = MutableStateFlow(0.dp)
    private val start = MutableStateFlow(0.dp)
    private val end = MutableStateFlow(0.dp)
    private val bottom = MutableStateFlow(0.dp)

    private val _state = MutableStateFlow(ImageCropState())
    val state: StateFlow<ImageCropState> = _state

    private val _cropState = MutableStateFlow(CropPositionState())
    val cropState: StateFlow<CropPositionState> = _cropState

    private var topF = 0f
    private var startF = 0f
    private var endF = 0f
    private var bottomF = 0f
    private var cropSize = IntSize.Zero
    private var containerSize = IntSize.Zero

    init {
        viewModelScope.launch(Default) {
            combine(
                imageUri,
                imageSize
            ) { imageUri, imageSize ->
                ImageCropState(imageUri, imageSize)
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch(Default) {
            combine(
                top,
                start,
                end,
                bottom
            ) { top,
                start,
                end,
                bottom ->
                CropPositionState(
                    top,
                    start,
                    end,
                    bottom
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _cropState.value = it
            }
        }

        viewModelScope.launch {
            imageSize.value = context.getImageSize(imageUri.value)
            logD(imageSize.value.toString())
        }
    }

    fun setLeftTopValue(cropXY: DpOffset, x: Float, y: Float) {
        top.value = cropXY.y
        start.value = cropXY.x
        startF = x
        topF = y
    }

    fun setRightTopValue(cropXY: DpOffset, x: Float, y: Float) {
        top.value = cropXY.y
        end.value = cropXY.x
        topF = y
        endF = x
    }

    fun setLeftBottomValue(cropXY: DpOffset, x: Float, y: Float) {
        bottom.value = cropXY.y
        start.value = cropXY.x
        bottomF = y
        startF = x
    }

    fun setRightBottomValue(cropXY: DpOffset, x: Float, y: Float) {
        bottom.value = cropXY.y
        end.value = cropXY.x
        bottomF = y
        endF = x
    }

    fun setContainerSize(containerSize: IntSize, cropSize: IntSize) {
        this.containerSize = containerSize
        this.cropSize = cropSize
    }

    fun onCrop(onDone: (Uri) -> Unit) {
        logD(cropSize.toString())
        logD(containerSize.toString())
        logD(topF.toString())
        logD(startF.toString())
        logD(endF.toString())
        logD(bottomF.toString())

        viewModelScope.launch {
            runCatching {
                context.getBitmapImage(imageUri.value)
            }.onSuccess {
                val cropBitmap = withContext(Default) {
                    val cropX = startF / containerSize.width.toFloat() * 100
                    val cropY = topF / containerSize.height.toFloat() * 100
                    val cropWidth = cropSize.width.toFloat() / containerSize.width.toFloat() * 100
                    val cropHeight =
                        cropSize.height.toFloat() / containerSize.height.toFloat() * 100

                    logD(cropX.toString())
                    logD(cropWidth.toString())
                    logD(cropY.toString())
                    logD(cropHeight.toString())

                    val x = (it.width * cropX / 100).toInt()
                    val y = (it.height * cropY / 100).toInt()
                    val width = (it.width * cropWidth / 100).toInt()
                    val height = (it.height * cropHeight / 100).toInt()

                    logD("===================================")

                    logD(x.toString())
                    logD(width.toString())
                    logD(y.toString())
                    logD(height.toString())

                    val bitmap = Bitmap.createBitmap(it, x, y, width, height)

                    when {
                        bitmap.width > bitmap.height && bitmap.width > 1000 -> {
                            Bitmap.createScaledBitmap(
                                bitmap,
                                1000,
                                bitmap.height * 1000 / bitmap.width,
                                true
                            ).apply {
                                logD(this.height.toString())
                                logD(this.width.toString())
                            }
                        }
                        bitmap.height > bitmap.width && bitmap.height > 100 -> {
                            Bitmap.createScaledBitmap(
                                bitmap,
                                bitmap.width * 1000 / bitmap.height,
                                1000,
                                true
                            ).apply {
                                logD(this.height.toString())
                                logD(this.width.toString())
                            }
                        }
                        else -> {
                            bitmap.apply {
                                logD(this.height.toString())
                                logD(this.width.toString())
                            }
                        }
                    }
                }

                val uri = withContext(IO) { context.saveBitmapToPng(cropBitmap) }
                logD(uri.toString())

                cropRepo.cropImage.value = uri
                onDone(uri)
            }.onFailure {
                logD(it.message)
            }
        }
    }
}

fun Context.saveBitmapToPng(bitmap: Bitmap): Uri {
    runCatching {
        val tempFile = File.createTempFile(
            "crop_image_",
            ".png",
            cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }

        FileOutputStream(tempFile).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        FileProvider.getUriForFile(
            applicationContext,
            "${packageName}.provider",
            tempFile
        )
    }.onSuccess {
        return it
    }.onFailure {
        logD(it.message)
    }

    return Uri.EMPTY
}

fun Context.getImageSize(uri: Uri): Size {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options)
    val imageHeight = options.outHeight
    val imageWidth = options.outWidth

    return Size(imageWidth.toFloat(), imageHeight.toFloat())
}

suspend fun Context.getBitmapImage(selectedPhotoUri: Uri): Bitmap {
    val loader = Coil.imageLoader(this)
    val request = ImageRequest.Builder(this)
        .data(selectedPhotoUri)
        .build()

    return (loader.execute(request).drawable as BitmapDrawable).bitmap
}

fun Context.getCacheFileUri(): Uri {
    val tmpFile = File.createTempFile(
        "preview_image",
        ".png",
        cacheDir
    ).apply {
        createNewFile()
        deleteOnExit()
    }

    return FileProvider.getUriForFile(
        applicationContext,
        "${packageName}.provider",
        tmpFile
    )
}

data class ImageCropState(
    val imageUri: Uri = Uri.EMPTY,
    val realImageSize: Size = Size(0F, 0F),
)

data class CropPositionState(
    val top: Dp = 0.dp,
    val start: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)