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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }; Toast.makeText(ctx, "Export OK", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null && folder != null) {
            val dir = File(vault, folder!!).apply { if (!exists()) mkdirs() }
            val ext = when (folder) {
                "Photos" -> "jpg"
                "Videos" -> "mp4"
                "PDF" -> "pdf"
                "XLSX" -> "xlsx"
                "Secure" -> "dat"
                else -> "docx"
            }
            val dest = File(dir, System.currentTimeMillis().toString() + "." + ext)
            ctx.contentResolver.openInputStream(u)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            refresh++
        }
    }

    if (!login) {
        // iOS PIN STYLE - GA KELIATAN PIN NYA
        Column(Modifier.fillMaxSize().background(Color(0xFFF2F0F7)).padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rian Dokumen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Private Vault • iOS Style", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 0 until 6) {
                            Box(Modifier.size(16.dp).background(if (i < pin.length) Color.Black else Color(0xFFE0DDE5), CircleShape))
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    // Keypad 1-9 0 X
                    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","x")
                    LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(keys) { k ->
                            if (k == "") {
                                Box(Modifier.size(80.dp))
                            } else {
                                Box(Modifier.size(80.dp).background(Color(0xFFF2F0F7), CircleShape).clickable {
                                    if (k == "x") { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                    else { if (pin.length < 6) pin += k; if (pin.length == 6) { if (pin == "123456") login = true else { Toast.makeText(ctx, "PIN salah!", Toast.LENGTH_SHORT).show(); pin = "" } } }
                                }, contentAlignment = Alignment.Center) {
                                    Text(k, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (folder == null) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF2F0F7)).padding(16.dp)) {
            Text("Rian Dokumen", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Offline • Docx Xlsx Pdf Foto Video", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(listOf("Docx" to "📄", "XLSX" to "📊", "PDF" to "📕", "Photos" to "🖼️", "Videos" to "🎬", "Secure" to "🔒")) { (name, icon) ->
                    Card(Modifier.fillMaxWidth().clickable { folder = name }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(icon, fontSize = 42.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.toList() ?: emptyList() }
        Column(Modifier.fillMaxSize().background(Color(0xFFF2F0F7)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { folder = null }, shape = RoundedCornerShape(10.dp)) { Text("< Back") }
                Spacer(Modifier.width(12.dp))
                Column { Text(folder!!, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(files.size.toString() + " file", fontSize = 12.sp, color = Color.Gray) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { pick.launch("*/*") }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("+ Tambah File") }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(files) { f ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(f.name, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text((f.length() / 1024).toString() + " KB", fontSize = 12.sp, color = Color.Gray)
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
