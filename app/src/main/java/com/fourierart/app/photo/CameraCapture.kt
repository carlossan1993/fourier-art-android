package com.fourierart.app.photo

import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fourierart.app.R
import java.io.File
import java.util.UUID

@Composable
fun CameraCapture(folder: File, onCaptured: (File)->Unit, onClose: ()->Unit, onError: ()->Unit) {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val view=remember {PreviewView(context).apply {implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FIT_CENTER}}
    var capture by remember {mutableStateOf<ImageCapture?>(null)}
    var front by remember {mutableStateOf(false)}
    var canSwitch by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    val latestCaptured by rememberUpdatedState(onCaptured);val latestError by rememberUpdatedState(onError)
    var active by remember {mutableStateOf(true)}
    DisposableEffect(owner,front) {
        var alive=true;active=true
        var provider: ProcessCameraProvider?=null;var boundPreview: Preview?=null;var boundCapture: ImageCapture?=null
        capture=null
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(alive) try {
                val p=future.get();provider=p
                val backAvailable=p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                val frontAvailable=p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                canSwitch=backAvailable && frontAvailable
                val useFront=front || !backAvailable
                val selector=if(useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val preview=Preview.Builder().build().also {it.setSurfaceProvider(view.surfaceProvider)}
                val image=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(view.display?.rotation ?: Surface.ROTATION_0).build()
                p.bindToLifecycle(owner,selector,preview,image)
                boundPreview=preview;boundCapture=image;capture=image
            } catch(_:Exception) {latestError()}
        },ContextCompat.getMainExecutor(context))
        onDispose {alive=false;active=false;capture=null
            val p=boundPreview;val c=boundCapture
            if(p!=null && c!=null) provider?.unbind(p,c)
        }
    }
    BackHandler {onClose()}
    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.camera_title),color=Color.White,style=MaterialTheme.typography.titleLarge)
        AndroidView(factory={view},modifier=Modifier.fillMaxWidth().weight(1f))
        Text(stringResource(R.string.camera_prepare_hint),color=Color.White,style=MaterialTheme.typography.bodySmall)
        if(capture==null || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onClose,modifier=Modifier.weight(1f).testTag("camera_cancel")) {Text(stringResource(R.string.cancel))}
            if(canSwitch) OutlinedButton(onClick={front=!front},enabled=!busy,modifier=Modifier.weight(1f).testTag("camera_switch")) {Text(stringResource(R.string.camera_switch))}
        }
        Button(onClick={
            val camera=capture ?: return@Button
            busy=true
            val file=File(folder,UUID.randomUUID().toString()+".jpg")
            val options=ImageCapture.OutputFileOptions.Builder(file).build()
            camera.targetRotation=view.display?.rotation ?: Surface.ROTATION_0
            try {camera.takePicture(options,ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    if(active) {busy=false;latestCaptured(file)} else file.delete()
                }
                override fun onError(exception: ImageCaptureException) {file.delete();if(active) {busy=false;latestError()}}
            })} catch(_:Exception) {file.delete();busy=false;latestError()}
        },enabled=capture!=null && !busy,modifier=Modifier.fillMaxWidth().testTag("camera_shutter")) {Text(stringResource(R.string.camera_shutter))}
    }
}
