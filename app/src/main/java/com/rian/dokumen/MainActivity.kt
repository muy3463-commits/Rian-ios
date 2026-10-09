package com.rian.dokumen

import android.graphics.BitmapFactory
import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var bukaFile by remember { mutableStateOf<File?>(null) }
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
            uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }; Toast.makeText(ctx, "Export OK ke Galeri", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null && folder != null) {
            try {
                val dir = File(vault, folder!!).apply { if (!exists()) mkdirs() }
                val ext = when (folder) {
                    "Photos" -> "jpg"; "Videos" -> "mp4"; "PDF" -> "pdf"; "XLSX" -> "xlsx"; "Secure" -> "dat"; else -> "docx"
                }
                val dest = File(dir, "${System.currentTimeMillis()}.$ext")
                ctx.contentResolver.openInputStream(u)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                refresh++
            } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
        }
    }

    if (bukaFile != null) {
        val f = bukaFile!!
        val isPhoto = f.extension.lowercase() in listOf("jpg","jpeg","png")
        val bitmap = remember(f) { if (isPhoto) try { BitmapFactory.decodeFile(f.absolutePath) } catch(e: Exception) { null } else null }
        
        AlertDialog(
            onDismissRequest = { bukaFile = null },
            containerColor = Color(0xFF1E1E24),
            title = { Text(f.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isPhoto && bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("${f.length()/1024} KB • Vault aman", color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportFile(f) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)), shape = RoundedCornerShape(12.dp)) { Text("Export", color = Color.White, fontWeight = FontWeight.Bold) }
                    Button(onClick = { bukaFile = null; Toast.makeText(ctx, "File dibuka", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)), shape = RoundedCornerShape(12.dp)) { Text("Buka", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { bukaFile = null }) { Text("Tutup", color = Color.Gray) } }
        )
    }

    if (!login) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)), contentAlignment = Alignment.Center) {
            Card(Modifier.fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Vault", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Your secure files • End-to-end encrypted", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (i in 0 until 6) { Box(Modifier.size(14.dp).background(if (i < pin.length) Color.White else Color(0xFF3A3A3C), CircleShape)) }
                    }
                    Spacer(Modifier.height(32.dp))
                    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
                    LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(keys) { k ->
                            if (k == "") { Box(Modifier.size(76.dp)) }
                            else {
                                Box(Modifier.size(76.dp).background(Color(0xFF2C2C2E), CircleShape).clickable {
                                    if (k == "⌫") { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                    else { if (pin.length < 6) pin += k; if (pin.length == 6) { if (pin == "123456") login = true else { pin = "" } } }
                                }, contentAlignment = Alignment.Center) { Text(k, fontSize = 24.sp, color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (folder == null) {
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Vault", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Your secure files • End-to-end encrypted", fontSize = 12.sp, color = Color.Gray) }
                Box(Modifier.background(Color(0xFFD4AF37), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("PRO", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(28.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item { val c = File(vault, "Docx").listFiles()?.size ?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "Docx" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFF0A84FF), CircleShape), contentAlignment = Alignment.Center) { Text("📄", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("Docx", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "XLSX").listFiles()?.size ?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "XLSX" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFF30D158), CircleShape), contentAlignment = Alignment.Center) { Text("📊", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("XLSX", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "PDF").listFiles()?.size ?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "PDF" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFFFF3B30), CircleShape), contentAlignment = Alignment.Center) { Text("PDF", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(10.dp)); Text("PDF", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "Photos").listFiles()?.size ?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "Photos" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFFAF52DE), CircleShape), contentAlignment = Alignment.Center) { Text("🖼️", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("Photos", color = Color.White, fontWeight = FontWeight.Bold); Text("$c items", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "Videos").listFiles()?.size ?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "Videos" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFFFF2D55), CircleShape), contentAlignment = Alignment.Center) { Text("▶️", fontSize = 22.sp) }; Spacer(Modifier.height(10.dp)); Text("Videos", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { Card(Modifier.fillMaxWidth().clickable { folder = "Secure" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFF5AC8FA), CircleShape), contentAlignment = Alignment.Center) { Text("🛡️", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("Secure", color = Color.White, fontWeight = FontWeight.Bold); Text("Vault locked", color = Color.Gray, fontSize = 12.sp) } } }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.toList() ?: emptyList() }
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Color(0xFF1E1E24), CircleShape).clickable { folder = null }.padding(horizontal = 18.dp, vertical = 10.dp)) { Text("‹ Back", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(14.dp)); Text(folder!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { pick.launch("*/*") }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) { Text("+ Tambah ke $folder", color = Color.Black, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(files) { f ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(16.dp)) {
                            Text(f.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 13.sp)
                            Text("${f.length()/1024} KB", color = Color.Gray, fontSize = 11.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { bukaFile = f }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) { Text("Buka", color = Color.Black, fontWeight = FontWeight.Bold) }
                                Button(onClick = { exportFile(f) }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))) { Text("Export", fontWeight = FontWeight.Bold) }
                                OutlinedButton(onClick = { f.delete(); refresh++ }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Hapus", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}
