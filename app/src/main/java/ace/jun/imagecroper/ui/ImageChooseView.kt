package ace.jun.imagecroper.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ImageChooseView(navHostController: NavHostController) {
    val viewModel: ImageChooseViewModel = hiltViewModel()
    val cropImageUri by viewModel.cropImageUri.collectAsState()

    val context = LocalContext.current
    val contentsPickDialog = remember { mutableStateOf(false) }
    val previewImageUri = remember { mutableStateOf<Uri?>(null) }
    val resultPreview = remember { mutableStateOf(false) }
    val launcherPreview =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            resultPreview.value = it
        }

    val resultGallery = remember { mutableStateOf<Uri?>(null) }
    val launcherGallery =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            resultGallery.value = it
        }

    resultPreview.value.let { isTake ->
        if (isTake) {
            navHostController.navigate("CROP_ROUTE/${Uri.encode(previewImageUri.value.toString())}")
            resultPreview.value = false
        }
    }

    resultGallery.value?.let {
        navHostController.navigate("CROP_ROUTE/${Uri.encode(it.toString())}")
        resultGallery.value = null
    }

    Column(Modifier.fillMaxSize()) {
        Image(
            rememberImagePainter(cropImageUri),
            contentDescription = null,
            modifier = Modifier.weight(1F)
        )
        Button(
            onClick = { contentsPickDialog.value = true },
        ) {
            Text("이미지선택")
        }
    }

    if (contentsPickDialog.value) {
        Dialog(
            onDismissRequest = { contentsPickDialog.value = false },
            DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                elevation = 10.dp,
                shape = MaterialTheme.shapes.medium.copy(CornerSize(15))
            ) {
                Row {
                    Column(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(15.dp)
                    ) {
                        IconButton(
                            onClick = {
                                launcherGallery.launch("image/*")
                                contentsPickDialog.value = false
                            },
                            modifier = Modifier
                                .weight(1F)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                tint = MaterialTheme.colors.primary,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            )
                        }
                        Text(
                            "갤러리",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(15.dp)
                    ) {
                        IconButton(
                            onClick = {
                                context.getCacheFileUri().let {
                                    previewImageUri.value = it
                                    launcherPreview.launch(it)
                                }

                                contentsPickDialog.value = false
                            },
                            modifier = Modifier
                                .weight(1F)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                tint = MaterialTheme.colors.primary,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            )
                        }
                        Text(
                            "카메라",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}