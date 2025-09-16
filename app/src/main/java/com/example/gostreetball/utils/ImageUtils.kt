package com.example.gostreetball.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import androidx.core.graphics.toColorInt
import com.example.gostreetball.R
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap
import com.example.gostreetball.ui.theme.Orange
import com.example.gostreetball.ui.theme.OrangeMild
import kotlin.math.roundToInt

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

fun imageBitmapToByteArray(bitmap: ImageBitmap): ByteArray {
    val androidBitmap = bitmap.asAndroidBitmap()
    val stream = ByteArrayOutputStream()
    androidBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    return stream.toByteArray()
}

fun createCourtMarkerBitmap(context: Context, size: Int = 120): Bitmap {
    val bitmap = createBitmap(size, size * 2)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        color = OrangeMild.toArgb()
    }

    val path = Path().apply {
        moveTo(0f, size / 1.5f)
        lineTo(size / 2f, 2f * size)
        lineTo(size.toFloat(), size / 1.5f)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawCircle(size / 2f, size / 1.5f, size / 2f, paint)

    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_notification)!!
    val iconSize = size / 1.3f
    val left = (size - iconSize) / 2
    val top = (size * 2f / 1.5f - iconSize) / 2
    drawable.setBounds(
        left.roundToInt(),
        top.roundToInt(),
        (left + iconSize).roundToInt(),
        (top + iconSize).roundToInt()
    )
    drawable.draw(canvas)

    return bitmap
}

fun createCourtMarkerIcon(context: Context): BitmapDescriptor {
    val bitmap = createCourtMarkerBitmap(context)
    return try {
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
    }
}

@Preview(showBackground = true)
@Composable
fun CourtMarkerPreview() {
    val context = LocalContext.current
    val bitmap = remember { createCourtMarkerBitmap(context, size = 200) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Court marker preview",
        modifier = Modifier.size(100.dp)
    )
}