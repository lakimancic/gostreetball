package com.example.gostreetball.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import java.io.InputStream

@Composable
fun rememberImagePicker(
    onImagePicked: (Bitmap) -> Unit
): ActivityResultLauncher<String> {
    val ctx = LocalContext.current
    val resolver = ctx.contentResolver

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = uriToBitmap(resolver, uri)
        if (bitmap != null) {
            onImagePicked(bitmap)
        }
    }

    return remember { launcher }
}

@Composable
fun rememberCameraCapture(
    onImagePicked: (Bitmap) -> Unit
): ActivityResultLauncher<Void?> {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onImagePicked(bitmap)
        }
    }

    return remember { launcher }
}

private fun uriToBitmap(resolver: ContentResolver, uri: Uri): Bitmap? {
    var input: InputStream? = null
    return try {
        input = resolver.openInputStream(uri)
        BitmapFactory.decodeStream(input)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    } finally {
        try {
            input?.close()
        } catch (ignored: IOException) {}
    }
}