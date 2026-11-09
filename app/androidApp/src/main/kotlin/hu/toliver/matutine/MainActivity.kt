package hu.toliver.matutine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.matutine.store.androidDataRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        MatutineActivityHolder.context = applicationContext

        setContent {
            MatutineApp(dataRoot = androidDataRoot(this))
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            MatutineActivityHolder.context = applicationContext
        }
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}