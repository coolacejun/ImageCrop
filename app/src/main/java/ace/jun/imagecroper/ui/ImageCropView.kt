package ace.jun.imagecroper.ui

import ace.jun.imagecroper.ui.theme.BlackAlpha
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CropValue(
    val setLeftTopValue: (DpOffset, Float, Float) -> Unit,
    val setRightTopValue: (DpOffset, Float, Float) -> Unit,
    val setLeftBottomValue: (DpOffset, Float, Float) -> Unit,
    val setRightBottomValue: (DpOffset, Float, Float) -> Unit,
    val setContainerSize: (IntSize, IntSize) -> Unit
)

@Composable
fun ImageCropView(navHostController: NavHostController) {
    val viewModel: ImageCropViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val cropState by viewModel.cropState.collectAsState()

    Scaffold {
        Views(
            state,
            cropState,
            viewModel.viewModelScope,
            CropValue(
                viewModel::setLeftTopValue,
                viewModel::setRightTopValue,
                viewModel::setLeftBottomValue,
                viewModel::setRightBottomValue,
                viewModel::setContainerSize
            )
        ) {
            viewModel.onCrop {
                navHostController.navigateUp()
            }
        }
    }
}

@Composable
private fun Views(
    state: ImageCropState,
    cropState: CropPositionState,
    coroutineScope: CoroutineScope,
    onCropValueValue: CropValue,
    onCrop: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        CropView(
            Modifier
                .weight(1F)
                .fillMaxSize()
                .padding(20.dp),
            state,
            cropState,
            coroutineScope,
            onCropValueValue
        )
        Text(
            stringResource(id = android.R.string.ok),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable {
                    onCrop()
                }
                .padding(20.dp)
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun CropView(
    modifier: Modifier,
    state: ImageCropState,
    cropState: CropPositionState,
    coroutineScope: CoroutineScope,
    onCropValueValue: CropValue
) {
    var parentRatio by remember { mutableStateOf(0F) }

    var topMargin by remember { mutableStateOf(0F) }
    var startMargin by remember { mutableStateOf(0F) }
    var endMargin by remember { mutableStateOf(0F) }
    var bottomMargin by remember { mutableStateOf(0F) }

    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var cropSize by remember { mutableStateOf(IntSize.Zero) }
    val minSize = remember { 65.dp }

    Box(modifier) {
        ConstraintLayout(
            Modifier
                .fillMaxSize()
                .onSizeChanged {
                    coroutineScope.launch {
                        withContext(Default) {
                            parentRatio = if (it != IntSize.Zero) {
                                it.height.toFloat() / it.width.toFloat() * 100F
                            } else 0F
                        }
                    }
                }
                .align(Alignment.Center)
        ) {
            val imageRatio = state.realImageSize.height / state.realImageSize.width * 100
            val isWidth = parentRatio >= imageRatio
            val (image, crop,
                leftTop, rightTop, leftBottom, rightBottom,
                topBg, bottomBg, leftBg, rightBg) = createRefs()

            val imagePainter = rememberImagePainter(data = state.imageUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }

            Image(
                painter = imagePainter,
                contentDescription = null,
                contentScale = if (isWidth) {
                    ContentScale.FillWidth
                } else {
                    ContentScale.FillHeight
                },
                modifier = if (isWidth) {
                    Modifier
                        .background(color = Color.DarkGray)
                        .requiredSizeIn(minWidth = 65.dp, minHeight = 65.dp)
                        .fillMaxWidth()
                        .onSizeChanged {
                            imageSize = it
                            onCropValueValue.setContainerSize(imageSize, cropSize)
                        }
                } else {
                    Modifier
                        .background(color = Color.DarkGray)
                        .requiredSizeIn(minWidth = 65.dp, minHeight = 65.dp)
                        .fillMaxHeight()
                        .onSizeChanged {
                            onCropValueValue.setContainerSize(imageSize, cropSize)
                            imageSize = it
                        }
                }.constrainAs(image) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )

            Box(modifier = Modifier
                .constrainAs(crop) {
                    height = fillToConstraints
                    width = fillToConstraints

                    start.linkTo(image.start, margin = cropState.start)
                    top.linkTo(image.top, margin = cropState.top)
                    end.linkTo(image.end, margin = cropState.end)
                    bottom.linkTo(image.bottom, margin = cropState.bottom)
                }
                .onSizeChanged {
                    cropSize = it
                    onCropValueValue.setContainerSize(imageSize, cropSize)
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        coroutineScope.launch {
                            change.consumeAllChanges()

                            coroutineScope.launch {
                                withContext(Default) {
                                    if (startMargin < 0) startMargin = 0F
                                    else if (endMargin > 0) startMargin += dragAmount.x

                                    if (endMargin < 0 && startMargin > 0) endMargin = 0F
                                    else if (startMargin > 0) endMargin -= dragAmount.x

                                    if (topMargin < 0) topMargin = 0F
                                    else if (bottomMargin > 0) topMargin += dragAmount.y

                                    if (bottomMargin < 0) bottomMargin = 0F
                                    else if (topMargin > 0) bottomMargin -= dragAmount.y

                                    if (startMargin >= 0 && topMargin >= 0) {
                                        onCropValueValue.setLeftTopValue(
                                            DpOffset(startMargin.toDp(), topMargin.toDp()),
                                            startMargin, topMargin
                                        )
                                    }

                                    if (endMargin >= 0 && topMargin >= 0) {
                                        onCropValueValue.setRightTopValue(
                                            DpOffset(
                                                endMargin.toDp(),
                                                topMargin.toDp()
                                            ),
                                            endMargin, topMargin
                                        )
                                    }

                                    if (startMargin >= 0 && bottomMargin >= 0) {
                                        onCropValueValue.setLeftBottomValue(
                                            DpOffset(
                                                startMargin.toDp(),
                                                bottomMargin.toDp()
                                            ),
                                            startMargin, bottomMargin
                                        )
                                    }

                                    if (endMargin >= 0 && bottomMargin >= 0) {
                                        onCropValueValue.setRightBottomValue(
                                            DpOffset(
                                                endMargin.toDp(),
                                                bottomMargin.toDp()
                                            ),
                                            endMargin, bottomMargin
                                        )
                                    }
                                }
                            }
                        }
                    }
                }) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BlackAlpha)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(horizontal = 1.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(horizontal = 1.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BlackAlpha)
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(BlackAlpha)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1F)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .padding(vertical = 1.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1F)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .padding(vertical = 1.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1F)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(Color.White)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(BlackAlpha)
                    )
                }
            }

            Spacer(modifier = Modifier
                .constrainAs(topBg) {
                    height = fillToConstraints
                    width = fillToConstraints

                    top.linkTo(image.top)
                    start.linkTo(image.start)
                    end.linkTo(image.end)
                    bottom.linkTo(crop.top)
                }
                .background(BlackAlpha))

            Spacer(modifier = Modifier
                .constrainAs(rightBg) {
                    height = fillToConstraints
                    width = fillToConstraints

                    top.linkTo(crop.top)
                    start.linkTo(crop.end)
                    end.linkTo(image.end)
                    bottom.linkTo(crop.bottom)
                }
                .background(BlackAlpha)
            )

            Spacer(modifier = Modifier
                .constrainAs(leftBg) {
                    height = fillToConstraints
                    width = fillToConstraints

                    top.linkTo(crop.top)
                    start.linkTo(image.start)
                    end.linkTo(crop.start)
                    bottom.linkTo(crop.bottom)
                }
                .background(BlackAlpha)
            )

            Spacer(modifier = Modifier
                .constrainAs(bottomBg) {
                    height = fillToConstraints
                    width = fillToConstraints

                    top.linkTo(crop.bottom)
                    start.linkTo(image.start)
                    end.linkTo(image.end)
                    bottom.linkTo(image.bottom)
                }
                .background(BlackAlpha)
            )

            LeftTop(modifier = Modifier
                .constrainAs(leftTop) {
                    start.linkTo(crop.start)
                    top.linkTo(crop.top)
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        coroutineScope.launch {
                            change.consumeAllChanges()

                            fun isMinWidth() =
                                startMargin < imageSize.width - (minSize.toPx() + endMargin)

                            fun isMinHeight() =
                                topMargin < imageSize.height - (minSize.toPx() + bottomMargin)

                            withContext(Default) {
                                if (startMargin < 0) startMargin = 0F
                                else {
                                    if (isMinWidth())
                                        startMargin += dragAmount.x
                                    else startMargin =
                                        imageSize.width - (minSize.toPx() + endMargin) - 1
                                }

                                if (topMargin < 0) topMargin = 0F
                                else {
                                    if (isMinHeight())
                                        topMargin += dragAmount.y
                                    else topMargin =
                                        imageSize.height - (minSize.toPx() + bottomMargin) - 1
                                }

                                if (startMargin >= 0 && topMargin >= 0) {
                                    if (isMinWidth() && isMinHeight())
                                        onCropValueValue.setLeftTopValue(
                                            DpOffset(startMargin.toDp(), topMargin.toDp()),
                                            startMargin, topMargin
                                        )
                                }
                            }
                        }
                    }
                }
            )

            RightTop(modifier = Modifier
                .constrainAs(rightTop) {
                    end.linkTo(crop.end)
                    top.linkTo(crop.top)
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        coroutineScope.launch {
                            change.consumeAllChanges()

                            fun isMinWidth() =
                                endMargin < imageSize.width - (minSize.toPx() + startMargin)

                            fun isMinHeight() =
                                topMargin < imageSize.height - (minSize.toPx() + bottomMargin)

                            withContext(Default) {
                                if (endMargin < 0) endMargin = 0F
                                else {
                                    if (isMinWidth())
                                        endMargin -= dragAmount.x
                                    else endMargin =
                                        imageSize.width - (minSize.toPx() + startMargin) - 1
                                }

                                if (topMargin < 0) topMargin = 0F
                                else {
                                    if (isMinHeight())
                                        topMargin += dragAmount.y
                                    else topMargin =
                                        imageSize.height - (minSize.toPx() + bottomMargin) - 1
                                }

                                if (endMargin >= 0 && topMargin >= 0) {
                                    if (isMinWidth() && isMinHeight())
                                        onCropValueValue.setRightTopValue(
                                            DpOffset(
                                                endMargin.toDp(),
                                                topMargin.toDp()
                                            ),
                                            endMargin, topMargin
                                        )
                                }
                            }
                        }
                    }
                })

            LeftBottom(modifier = Modifier
                .constrainAs(leftBottom) {
                    start.linkTo(crop.start)
                    bottom.linkTo(crop.bottom)
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        coroutineScope.launch {
                            change.consumeAllChanges()

                            fun isMinWidth() =
                                startMargin < imageSize.width - (minSize.toPx() + endMargin)

                            fun isMinHeight() =
                                bottomMargin < imageSize.height - (minSize.toPx() + topMargin)

                            withContext(Default) {
                                if (startMargin < 0) startMargin = 0F
                                else {
                                    if (isMinWidth())
                                        startMargin += dragAmount.x
                                    else startMargin =
                                        imageSize.width - (minSize.toPx() + endMargin) - 1
                                }

                                if (bottomMargin < 0) bottomMargin = 0F
                                else {
                                    if (isMinHeight())
                                        bottomMargin -= dragAmount.y
                                    else bottomMargin =
                                        imageSize.height - (minSize.toPx() + topMargin) - 1
                                }

                                if (startMargin >= 0 && bottomMargin >= 0) {
                                    if (isMinWidth() && isMinHeight())
                                        onCropValueValue.setLeftBottomValue(
                                            DpOffset(
                                                startMargin.toDp(),
                                                bottomMargin.toDp()
                                            ),
                                            startMargin, bottomMargin
                                        )
                                }
                            }
                        }
                    }
                })

            RightBottom(modifier = Modifier
                .constrainAs(rightBottom) {
                    end.linkTo(crop.end)
                    bottom.linkTo(crop.bottom)
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        coroutineScope.launch {
                            change.consumeAllChanges()

                            fun isMinWidth() =
                                endMargin < imageSize.width - (minSize.toPx() + startMargin)

                            fun isMinHeight() =
                                bottomMargin < imageSize.height - (minSize.toPx() + topMargin)

                            withContext(Default) {
                                if (endMargin < 0) endMargin = 0F
                                else {
                                    if (isMinWidth())
                                        endMargin -= dragAmount.x
                                    else endMargin =
                                        imageSize.width - (minSize.toPx() + startMargin) - 1
                                }
                                if (bottomMargin < 0) bottomMargin = 0F
                                else {
                                    if (isMinHeight())
                                        bottomMargin -= dragAmount.y
                                    else bottomMargin =
                                        imageSize.height - (minSize.toPx() + topMargin) - 1
                                }

                                if (endMargin >= 0 && bottomMargin >= 0) {
                                    if (isMinWidth() && isMinHeight())
                                        onCropValueValue.setRightBottomValue(
                                            DpOffset(
                                                endMargin.toDp(),
                                                bottomMargin.toDp()
                                            ),
                                            endMargin, bottomMargin
                                        )
                                }
                            }
                        }
                    }
                })
        }
    }
}

@Composable
private fun LeftTop(modifier: Modifier) {
    Box(modifier.size(30.dp)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(color = Color.White)
                .align(Alignment.TopStart)
        )
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(color = Color.White)
                .align(Alignment.TopStart)
        )
    }
}

@Composable
private fun RightTop(modifier: Modifier) {
    Box(modifier.size(30.dp)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(color = Color.White)
                .align(Alignment.TopStart)
        )
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(color = Color.White)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun LeftBottom(modifier: Modifier) {
    Box(modifier.size(30.dp)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(color = Color.LightGray)
                .align(Alignment.BottomStart)
        )
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(color = Color.LightGray)
                .align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun RightBottom(modifier: Modifier) {
    Box(modifier.size(30.dp)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(color = Color.LightGray)
                .align(Alignment.BottomEnd)
        )
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(color = Color.LightGray)
                .align(Alignment.BottomEnd)
        )
    }
}