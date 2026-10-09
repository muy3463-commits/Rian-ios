package com.rian.dokumen

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

fun loadBitmapFixMiring(f: File): Bitmap? {
    try {
        var bmp = BitmapFactory.decodeFile(f.absolutePath)?: return null
        val exif = ExifInterface(f.absolutePath)
        val ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotation = when (ori) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation!= 0f) {
            val mat = Matrix()
            mat.postRotate(rotation)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mat, true)
        }
        return bmp
    } catch (e: Exception) { return null }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var login by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    var bukaFile by remember { mutableStateOf<File?>(null) }
    var fullList by remember { mutableStateOf<List<File>>(emptyList()) }
    var fullIndex by remember { mutableStateOf(0) }
    var showFull by remember { mutableStateOf(false) }
    val vault = File(ctx.filesDir, "vault").apply { if (!exists()) mkdirs() }
    var refresh by remember { mutableStateOf(0) }

    fun exportAndOpen(f: File, openAfter: Boolean) {
        try {
            val ext = f.extension.lowercase()
            val isImg = ext in listOf("jpg","jpeg","png")
            val isVid = ext == "mp4"
            val mime = when {
                isImg -> "image/*"
                isVid -> "video/*"
                ext == "pdf" -> "application/pdf"
                else -> "*/*"
            }
            val coll = if (isImg) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else if (isVid) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val rel = if (isImg) Environment.DIRECTORY_PICTURES + "/RianDokumen" else if (isVid) Environment.DIRECTORY_MOVIES + "/RianDokumen" else Environment.DIRECTORY_DOWNLOADS + "/RianDokumen"
            val cv = ContentValues()
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime)
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            val uri = ctx.contentResolver.insert(coll, cv)
            if (uri!= null) {
                ctx.contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }
                Toast.makeText(ctx, "Export OK", Toast.LENGTH_SHORT).show()
                if (openAfter) {
                    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    try { ctx.startActivity(Intent.createChooser(intent, "Buka dengan")) } catch(e: Exception) {}
                }
            }
        } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u!= null && folder!= null) {
            try {
                val dir = File(vault, folder!!).apply { if (!exists()) mkdirs() }
                val ext = when (folder) { "Photos" -> "jpg"; "Videos" -> "mp4"; "PDF" -> "pdf"; "XLSX" -> "xlsx"; "Secure" -> "dat"; else -> "docx" }
                val dest = File(dir, "${System.currentTimeMillis()}.$ext")
                ctx.contentResolver.openInputStream(u)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                refresh++
            } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
        }
    }

    // FULL LAYAR SWIPEABLE
    if (showFull && fullList.isNotEmpty()) {
        val pagerState = rememberPagerState(initialPage = fullIndex, pageCount = { fullList.size })
        val currentFile = fullList[pagerState.currentPage]
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val f = fullList[page]
                val bitmap = remember(f) { loadBitmapFixMiring(f) }
                if (bitmap!= null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ga bisa load", color = Color.White) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(20.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Color(0x88000000), CircleShape).clickable { showFull = false }.padding(horizontal = 20.dp, vertical = 10.dp)) { Text("✕ Tutup", color = Color.White, fontWeight = FontWeight.Bold) }
                Box(Modifier.background(Color(0x88000000), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("${pagerState.currentPage + 1} / ${fullList.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Box(Modifier.background(Color(0xFFD4AF37), RoundedCornerShape(20.dp)).clickable { exportAndOpen(currentFile, false) }.padding(horizontal = 20.dp, vertical = 10.dp)) { Text("Export", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
            Text(currentFile.name, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp).background(Color(0x88000000), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontSize = 11.sp)
        }
        return
    }

    if (bukaFile!= null) {
        val f = bukaFile!!
        val ext = f.extension.lowercase()
        if (ext in listOf("jpg","jpeg","png")) {
            // Ini ga kepake lagi, langsung ke swipe
            bukaFile = null
        } else {
            AlertDialog(
                onDismissRequest = { bukaFile = null },
                containerColor = Color(0xFF1E1E24),
                title = { Text(f.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1) },
                text = { Box(Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(if (ext == "mp4") "▶️ VIDEO\nSwipe juga bisa!" else "📄 ${ext.uppercase()}", color = Color.White, fontWeight = FontWeight.Bold) } },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportAndOpen(f, false) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)), shape = RoundedCornerShape(12.dp)) { Text("Export", color = Color.White) }
                        Button(onClick = { exportAndOpen(f, true); bukaFile = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)), shape = RoundedCornerShape(12.dp)) { Text("Buka", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                },
                dismissButton = { TextButton(onClick = { bukaFile = null }) { Text("Tutup", color = Color.Gray) } }
            )
        }
    }

    if (!login) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)), contentAlignment = Alignment.Center) {
            Card(Modifier.fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Vault", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { for (i in 0 until 6) { Box(Modifier.size(14.dp).background(if (i < pin.length) Color.White else Color(0xFF3A3A3C), CircleShape)) } }
                    Spacer(Modifier.height(32.dp))
                    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
                    LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(keys) { k ->
                            if (k == "") { Box(Modifier.size(76.dp)) }
                            else { Box(Modifier.size(76.dp).background(Color(0xFF2C2C2E), CircleShape).clickable { if (k == "⌫") { if (pin.isNotEmpty()) pin = pin.dropLast(1) } else { if (pin.length < 6) pin += k; if (pin.length == 6) { if (pin == "123456") login = true else { pin = "" } } } }, contentAlignment = Alignment.Center) { Text(k, fontSize = 24.sp, color = Color.White) } }
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
                Column { Text("Vault", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Swipe foto kaya iPhone", fontSize = 12.sp, color = Color.Gray) }
                Box(Modifier.background(Color(0xFFD4AF37), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("PRO", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(28.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item { val c = File(vault, "Docx").listFiles()?.size?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "Docx" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFF0A84FF), CircleShape), contentAlignment = Alignment.Center) { Text("📄", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("Docx", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "XLSX").listFiles()?.size?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "XLSX" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFF30D158), CircleShape), contentAlignment = Alignment.Center) { Text("📊", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("XLSX", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "PDF").listFiles()?.size?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "PDF" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFFFF3B30), CircleShape), contentAlignment = Alignment.Center) { Text("PDF", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(10.dp)); Text("PDF", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "Photos").listFiles()?.size?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "Photos" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFFAF52DE), CircleShape), contentAlignment = Alignment.Center) { Text("🖼️", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("Photos", color = Color.White, fontWeight = FontWeight.Bold); Text("$c items", color = Color.Gray, fontSize = 12.sp) } } }
                item { val c = File(vault, "Videos").listFiles()?.size?: 0; Card(Modifier.fillMaxWidth().clickable { folder = "Videos" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFFFF2D55), CircleShape), contentAlignment = Alignment.Center) { Text("▶️", fontSize = 22.sp) }; Spacer(Modifier.height(10.dp)); Text("Videos", color = Color.White, fontWeight = FontWeight.Bold); Text("$c files", color = Color.Gray, fontSize = 12.sp) } } }
                item { Card(Modifier.fillMaxWidth().clickable { folder = "Secure" }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(56.dp).background(Color(0xFF5AC8FA), CircleShape), contentAlignment = Alignment.Center) { Text("🛡️", fontSize = 26.sp) }; Spacer(Modifier.height(10.dp)); Text("Secure", color = Color.White, fontWeight = FontWeight.Bold); Text("Vault locked", color = Color.Gray, fontSize = 12.sp) } } }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() }?: emptyList() }
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Color(0xFF1E1E24), CircleShape).clickable { folder = null }.padding(horizontal = 18.dp, vertical = 10.dp)) { Text("‹ Back", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(14.dp)); Text(folder!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { pick.launch("*/*") }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) { Text("+ Tambah ke $folder", color = Color.Black, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(files.size) { idx ->
                    val f = files[idx]
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(16.dp)) {
                            Text(f.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 13.sp)
                            Text("${f.length()/1024} KB", color = Color.Gray, fontSize = 11.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val ext = f.extension.lowercase()
                                    if (ext in listOf("jpg","jpeg","png")) {
                                        fullList = files.filter { it.extension.lowercase() in listOf("jpg","jpeg","png") }
                                        fullIndex = fullList.indexOf(f).coerceAtLeast(0)
                                        showFull = true
                                    } else {
                                        bukaFile = f
                                    }
                                }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) { Text("Buka", color = Color.Black, fontWeight = FontWeight.Bold) }
                                Button(onClick = { exportAndOpen(f, false) }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))) { Text("Export") }
                                OutlinedButton(onClick = { f.delete(); refresh++ }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Hapus", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}
