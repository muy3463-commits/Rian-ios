package com.rian.dokumen

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var viewingFile by remember { mutableStateOf<File?>(null) }
    val vaultDir = File(context.filesDir, "vault")
    if (!vaultDir.exists()) vaultDir.mkdirs()
    var refresh by remember { mutableStateOf(0) }

    fun exportFile(f: File) {
        try {
            val ext = f.extension.lowercase()
            val mime = if (ext == "jpg" || ext == "jpeg") "image/jpeg" else if (ext == "png") "image/png" else if (ext == "mp4") "video/mp4" else "application/octet-stream"
            val collection = if (ext == "jpg" || ext == "jpeg" || ext == "png") MediaStore.Images.Media.EXTERNAL_CONTENT_URI else if (ext == "mp4") MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val path = if (ext == "jpg" || ext == "jpeg" || ext == "png") Environment.DIRECTORY_PICTURES + "/RianDokumen" else if (ext == "mp4") Environment.DIRECTORY_MOVIES + "/RianDokumen" else Environment.DIRECTORY_DOWNLOADS + "/RianDokumen"
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            val uri = context.contentResolver.insert(collection, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { inp -> inp.copyTo(out) } }
                Toast.makeText(context, "Export Berhasil!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && selectedFolder != null) {
            val folder = File(vaultDir, selectedFolder!!)
            if (!folder.exists()) folder.mkdirs()
            var ext = "docx"
            if (selectedFolder == "Photos") ext = "jpg"
            if (selectedFolder == "Videos") ext = "mp4"
            if (selectedFolder == "Pdf") ext = "pdf"
            val dest = File(folder, selectedFolder + "_" + System.currentTimeMillis() + "." + ext)
            context.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            refresh++
        }
    }

    if (viewingFile != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val file = viewingFile!!
            val isImage = file.extension.lowercase() == "jpg" || file.extension.lowercase() == "jpeg" || file.extension.lowercase() == "png"
            if (isImage) {
                val bmp = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
                if (bmp != null) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            } else {
                AndroidView(factory = { ctx -> VideoView(ctx).apply { setVideoURI(Uri.fromFile(file)); setOnPreparedListener { it.isLooping = true; start() } } }, modifier = Modifier.fillMaxSize())
            }
            Button(onClick = { viewingFile = null }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Text("X Tutup") }
        }
        return
    }

    if (!isLogin) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rian Dokumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = pinInput, onValueChange = { pinInput = it }, label = { Text("PIN 123456") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { if (pinInput == "123456") isLogin = true }, modifier = Modifier.fillMaxWidth()) { Text("Masuk") }
                }
            }
        }
        return
    }

    if (selectedFolder == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Vault Kamu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(listOf("Docx", "Pdf", "Photos", "Videos")) { name ->
                    Card(modifier = Modifier.padding(8.dp).clickable { selectedFolder = name }, shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (name == "Photos") "🖼️" else if (name == "Videos") "🎬" else if (name == "Pdf") "📕" else "📄", style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(name, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    } else {
        val folder = File(vaultDir, selectedFolder!!)
        val files = remember(refresh, selectedFolder) { folder.listFiles()?.toList() ?: emptyList() }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { selectedFolder = null }) { Text("<") }
                Spacer(modifier = Modifier.width(12.dp))
                Text(selectedFolder!!, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(files.size.toString() + " file", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
