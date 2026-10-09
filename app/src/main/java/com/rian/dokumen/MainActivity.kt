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
import androidx.compose.ui.draw.shadow
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
            uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }; Toast.makeText(ctx, "Export Premium OK", Toast.LENGTH_SHORT).show() }
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
        // PREMIUM PIN SCREEN
        Box(Modifier.fillMaxSize().background(Color(0xFFF8F7FB)), contentAlignment = Alignment.Center) {
            Card(Modifier.fillMaxWidth().padding(20.dp).shadow(20.dp, RoundedCornerShape(32.dp)), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).background(Color(0xFF111111), CircleShape), contentAlignment = Alignment.Center) { Text("🔐", fontSize = 34.sp) }
                    Spacer(Modifier.height(16.dp))
                    Text("Rian Dokumen", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text("PREMIUM • Private Vault", fontSize = 12.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (i in 0 until 6) {
                            Box(Modifier.size(14.dp).background(if (i < pin.length) Color(0xFF111111) else Color(0xFFE5E5EA), CircleShape))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
                    LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        items(keys) { k ->
                            if (k == "") { Box(Modifier.size(76.dp)) }
                            else {
                                Box(Modifier.size(76.dp).background(Color(0xFFF2F2F7), CircleShape).clickable {
                                    if (k == "⌫") { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                    else { if (pin.length < 6) pin += k; if (pin.length == 6) { if (pin == "123456") login = true else { Toast.makeText(ctx, "PIN Salah", Toast.LENGTH_SHORT).show(); pin = "" } } }
                                }, contentAlignment = Alignment.Center) { Text(k, fontSize = 24.sp, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (folder == null) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF8F7FB)).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Rian Dokumen", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp); Text("PREMIUM EDITION", fontSize = 11.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
                Box(Modifier.background(Color(0xFF111111), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("PRO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            }
            Spacer(Modifier.height(6.dp))
            Text("Offline • Encrypted • Premium", fontSize = 13.sp, color = Color(0xFF8E8E93))
            Spacer(Modifier.height(24.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(listOf(
                    Triple("Docx", "📄", Color(0xFF007AFF)),
                    Triple("XLSX", "📊", Color(0xFF34C759)),
                    Triple("PDF", "📕", Color(0xFFFF3B30)),
                    Triple("Photos", "🖼️", Color(0xFFFF9500)),
                    Triple("Videos", "🎬", Color(0xFFAF52DE)),
                    Triple("Secure", "🔒", Color(0xFF111111))
                )) { (name, icon, col) ->
                    Card(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp)).clickable { folder = name }, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(col.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) { Text(icon, fontSize = 28.sp) }
                            Spacer(Modifier.height(12.dp))
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Encrypted", fontSize = 11.sp, color = Color(0xFF8E8E93))
                        }
                    }
                }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.toList() ?: emptyList() }
        Column(Modifier.fillMaxSize().background(Color(0xFFF8F7FB)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Color.White, CircleShape).shadow(2.dp, CircleShape).clickable { folder = null }.padding(12.dp)) { Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(14.dp))
                Column { Text(folder!!, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(files.size.toString() + " files • Premium Vault", fontSize = 12.sp, color = Color(0xFF8E8E93)) }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { pick.launch("*/*") }, modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))) { Text("+ Tambah File Premium", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(files) { f ->
                    Card(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("📄", fontSize = 20.sp) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) { Text(f.name, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp); Text((f.length() / 1024).toString() + " KB • Encrypted", fontSize = 11.sp, color = Color(0xFF8E8E93)) }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { setDataAndType(uri, "*/*"); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                    ctx.startActivity(android.content.Intent.createChooser(intent, "Buka"))
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))) { Text("Buka") }
                                Button(onClick = { exportFile(f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))) { Text("Export") }
                                OutlinedButton(onClick = { f.delete(); refresh++ }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
        }
    }
}
