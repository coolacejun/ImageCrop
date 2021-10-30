package ace.jun.imagecroper

import ace.jun.imagecroper.ui.ImageChooseView
import ace.jun.imagecroper.ui.ImageCropArgs.IMAGE_URI
import ace.jun.imagecroper.ui.ImageCropView
import ace.jun.imagecroper.ui.theme.ImageCroperTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCroperTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val rootNavHostController = rememberAnimatedNavController()

                    AnimatedNavHost(
                        navController = rootNavHostController,
                        startDestination = "HOME_ROUTE"
                    ) {
                        composable("HOME_ROUTE") {
                            ImageChooseView(rootNavHostController)
                        }

                        composable(
                            "CROP_ROUTE"
                                    + "/{${IMAGE_URI}}",
                            arguments = listOf(navArgument(IMAGE_URI) { type = NavType.StringType })
                        ) {
                            ImageCropView(rootNavHostController)
                        }
                    }
                }
            }
        }
    }
}

