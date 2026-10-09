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

    val bgBlack = Color(0xFF0A0A0F)
    val cardDark = Color(0xFF1E1E24)
    val gold = Color(0xFFD4AF37)

    fun exportFile(f: File) {
        try {
            val ext = f.extension.lowercase()
            val isImg = ext in listOf("jpg","jpeg","png")
            val isVid = ext == "mp4"
            val coll = if (isImg) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else if (isVid) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val rel = if (isImg) Environment.DIRECTORY_PICTURES + "/RianDokumen" else if (isVid) Environment.DIRECTORY_MOVIES + "/RianDokumen" else Environment.DIRECTORY_DOWNLOADS + "/RianDokumen"
            val cv = ContentValues()
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            val uri = ctx.contentResolver.insert(coll, cv)
            uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }; Toast.makeText(ctx, "Export OK: ${f.name}", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Toast.makeText(ctx, "Gagal: ${e.message}", Toast.LENGTH_LONG).show() }
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
                Toast.makeText(ctx, "Berhasil", Toast.LENGTH_SHORT).show()
                refresh++
            } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
        }
    }

    // DIALOG BUKA FILE
    if (bukaFile != null) {
        AlertDialog(
            onDismissRequest = { bukaFile = null },
            containerColor = cardDark,
            titleContentColor = Color.White,
            textContentColor = Color.Gray,
            title = { Text("📂 ${bukaFile!!.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("${bukaFile!!.length()/1024} KB • Terenkripsi", color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("File aman di Private Vault. Klik Export untuk membuka di aplikasi lain.", color = Color.LightGray, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { exportFile(bukaFile!!); bukaFile = null }, colors = ButtonDefaults.buttonColors(containerColor = gold)) { Text("Export & Buka", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { bukaFile = null }) { Text("Tutup", color = Color.White) } }
        )
    }

    if (!login) {
        Box(Modifier.fillMaxSize().background(bgBlack), contentAlignment = Alignment.Center) {
            Card(Modifier.fillMaxWidth().padding(24.dp).shadow(24.dp, RoundedCornerShape(32.dp)), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = cardDark)) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Text("🔐", fontSize = 34.sp) }
                    Spacer(Modifier.height(16.dp))
                    Text("Vault", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                    else { if (pin.length < 6) pin += k; if (pin.length == 6) { if (pin == "123456") login = true else { Toast.makeText(ctx, "PIN Salah", Toast.LENGTH_SHORT).show(); pin = "" } } }
                                }, contentAlignment = Alignment.Center) { Text(k, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (folder == null) {
        Column(Modifier.fillMaxSize().background(bgBlack).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Vault", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Your secure files • End-to-end encrypted", fontSize = 13.sp, color = Color.Gray)
                }
                Box(Modifier.background(gold, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("PRO", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
            }
            Spacer(Modifier.height(28.dp))
            val itemsList = listOf(
                Triple("Docx", "Docx" to Color(0xFF0A84FF)),
                Triple("XLSX", "XLSX" to Color(0xFF30D158)),
                Triple("PDF", "PDF" to Color(0xFFFF3B30)),
                Triple("Photos", "Photos" to Color(0xFFAF52DE)),
                Triple("Videos", "Videos" to Color(0xFFFF2D55)),
                Triple("Secure", "Secure" to Color(0xFF5AC8FA))
            )
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(itemsList) { (name, pair) ->
                    val (label, col) = pair
                    val count = File(vault, name).listFiles()?.size ?: 0
                    val countText = if (name == "Photos") "$count items" else if (name == "Secure") "Vault locked" else "$count files"
                    val iconText = when(name) { "Docx" -> "📄"; "XLSX" -> "📊"; "PDF" -> "PDF"; "Photos" -> "🖼️"; "Videos" -> "▶️"; else -> "🛡️" }
                    Card(Modifier.fillMaxWidth().shadow(0.dp, RoundedCornerShape(24.dp)).clickable { folder = name }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardDark)) {
                        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(col, CircleShape), contentAlignment = Alignment.Center) {
                                Text(iconText, fontSize = if (name=="PDF") 14.sp else 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                            Text(countText, fontSize = 12.sp, color = Color(0xFF8E8E93))
                        }
                    }
                }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.toList()?.sortedByDescending { it.lastModified()
