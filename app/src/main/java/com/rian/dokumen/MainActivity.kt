package com.rian.dokumen

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var login by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    val vault = File(ctx.filesDir, "vault").apply { if (!exists()) mkdirs() }
    var refresh by remember { mutableStateOf(0) }

    fun exportFile(f: File) {
        try {
            val ext = f.extension.lowercase()
            val isImg = ext == "jpg" || ext == "jpeg" || ext == "png"
            val isVid = ext == "mp4"
            val coll = if (isImg) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else if (isVid) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val rel = if (isImg) Environment.DIRECTORY_PICTURES + "/RianDokumen" else if (isVid) Environment.DIRECTORY_MOVIES + "/RianDokumen" else Environment.DIRECTORY_DOWNLOADS + "/RianDokumen"
            val cv = ContentValues()
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            val uri = ctx.contentResolver.insert(coll, cv)
            uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }; Toast.makeText(ctx, "Export OK ke " + rel, Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null && folder != null) {
            val dir = File(vault, folder!!).apply { if (!exists()) mkdirs() }
            val ext = if (folder == "Photos") "jpg" else if (folder == "Videos") "mp4" else if (folder == "Pdf") "pdf" else "docx"
            val dest = File(dir, System.currentTimeMillis().toString() + "." + ext)
            ctx.contentResolver.openInputStream(u)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            refresh++
        }
    }

    if (!login) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rian Dokumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Vault Aman", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN 123456") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { if (pin == "123456") login = true else Toast.makeText(ctx, "PIN salah!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Masuk", fontWeight = FontWeight.Bold) }
                }
            }
        }
        return
    }

    if (folder == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Vault Kamu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Pilih folder", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf("Docx", "Pdf", "Photos", "Videos")) { n ->
                    val icon = if (n == "Photos") "🖼️" else if (n == "Videos") "🎬" else if (n == "Pdf") "📕" else "📄"
                    Card(Modifier.clickable { folder = n }, shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(icon, style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(n, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.toList() ?: emptyList() }
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { folder = null }, shape = RoundedCornerShape(10.dp)) { Text("< Back") }
                Spacer(Modifier.width(12.dp))
                Column { Text(folder!!, fontWeight = FontWeight.Bold); Text(files.size.toString() + " file", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { pick.launch("*/*") }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("+ Tambah File ke " + folder!!) }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(files) { f ->
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(f.name, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text((f.length() / 1024).toString() + " KB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { setDataAndType(uri, "*/*"); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                    ctx.startActivity(android.content.Intent.createChooser(intent, "Buka"))
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Buka") }
                                Button(onClick = { exportFile(f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Export") }
                                OutlinedButton(onClick = { f.delete(); refresh++ }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
        }
    }
}
