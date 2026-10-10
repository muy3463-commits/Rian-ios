package com.rian.dokumen

import android.content.ContentValues
import android.graphics.BitmapFactory
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
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var login by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    var fullList by remember { mutableStateOf<List<File>>(emptyList()) }
    var fullIndex by remember { mutableStateOf(0) }
    var showFull by remember { mutableStateOf(false) }
    val vault = File(ctx.filesDir, "vault").apply { if (!exists()) mkdirs() }
    var refresh by remember { mutableStateOf(0) }

    fun exportFile(f: File) {
        try {
            val ext = f.extension.lowercase()
            val isImg = ext in listOf("jpg","jpeg","png")
            val isVid = ext == "mp4"
            val coll = if (isImg) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else if (isVid) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isImg) Environment.DIRECTORY_PICTURES + "/RianDokumen" else if (isVid) Environment.DIRECTORY_MOVIES + "/RianDokumen" else Environment.DIRECTORY_DOWNLOADS + "/RianDokumen")
            }
            val uri = ctx.contentResolver.insert(coll, cv)
            if (uri!= null) {
                ctx.contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { i -> i.copyTo(o) } }
                Toast.makeText(ctx, "Export OK ke Galeri/Downloads", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u!= null && folder!= null) {
            try {
                val dir = File(vault, folder!!).apply { if (!exists()) mkdirs() }
                val ext = when (folder) { "Photos" -> "jpg"; "Videos" -> "mp4"; "PDF" -> "pdf"; "XLSX" -> "xlsx"; else -> "docx" }
                File(dir, "${System.currentTimeMillis()}.$ext").also { dest -> ctx.contentResolver.openInputStream(u)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } } }
                refresh++
            } catch (e: Exception) {}
        }
    }

    if (showFull && fullList.isNotEmpty()) {
        val pagerState = rememberPagerState(initialPage = fullIndex, pageCount = { fullList.size })
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val f = fullList[page]
                val bmp = remember(f) { try { BitmapFactory.decodeFile(f.absolutePath) } catch(e: Exception){ null } }
                if (bmp!= null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("▶️ VIDEO", color = Color.White, fontSize = 24.sp) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.background(Color(0x66000000), CircleShape).clickable { showFull = false }.padding(12.dp, 18.dp)) { Text("✕", color = Color.White, fontWeight = FontWeight.Bold) }
                Box(Modifier.background(Color(0x66000000), RoundedCornerShape(20.dp)).padding(10.dp, 6.dp)) { Text("${pagerState.currentPage + 1}/${fullList.size}", color = Color.White, fontSize = 12.sp) }
            }
        }
        return
    }

    if (!login) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("Vault", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Your secure files • End-to-end encrypted", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { for (i in 0..5) Box(Modifier.size(10.dp).background(if (i < pin.length) Color.White else Color(0xFF333333), CircleShape)) }
                Spacer(Modifier.height(24.dp))
                val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(keys.size) { idx ->
                        val k = keys[idx]
                        if (k == "") Box(Modifier.size(72.dp))
                        else Box(Modifier.size(72.dp).background(Color(0xFF1E1E24), CircleShape).clickable {
                            if (k == "⌫") { if (pin.isNotEmpty()) pin = pin.dropLast(1) } else { if (pin.length < 6) pin += k; if (pin.length == 6) { if (pin == "123456") login = true else pin = "" } }
                        }, contentAlignment = Alignment.Center) { Text(k, color = Color.White, fontSize = 22.sp) }
                    }
                }
            }
        }
        return
    }

    if (folder == null) {
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Vault", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text("Your secure files • End-to-end encrypted", color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
                Box(Modifier.background(Color(0xFFD4AF37), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("PRO", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            Spacer(Modifier.height(28.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    val c = File(vault, "Docx").listFiles()?.size?:0
                    Card(Modifier.fillMaxWidth().clickable { folder = "Docx" }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(vertical = 22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(Color(0xFF0A84FF), CircleShape), contentAlignment = Alignment.Center) { Text("📄", fontSize = 26.sp) }
                            Spacer(Modifier.height(10.dp)); Text("Docx", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("$c files", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                }
                item {
                    val c = File(vault, "XLSX").listFiles()?.size?:0
                    Card(Modifier.fillMaxWidth().clickable { folder = "XLSX" }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(vertical = 22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(Color(0xFF30D158), CircleShape), contentAlignment = Alignment.Center) { Text("📊", fontSize = 26.sp) }
                            Spacer(Modifier.height(10.dp)); Text("XLSX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("$c files", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                }
                item {
                    val c = File(vault, "PDF").listFiles()?.size?:0
                    Card(Modifier.fillMaxWidth().clickable { folder = "PDF" }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(vertical = 22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(Color(0xFFFF3B30), CircleShape), contentAlignment = Alignment.Center) { Text("PDF", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(10.dp)); Text("PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("$c files", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                }
                item {
                    val c = File(vault, "Photos").listFiles()?.size?:0
                    Card(Modifier.fillMaxWidth().clickable { folder = "Photos" }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(vertical = 22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(Color(0xFFAF52DE), CircleShape), contentAlignment = Alignment.Center) { Text("🖼️", fontSize = 26.sp) }
                            Spacer(Modifier.height(10.dp)); Text("Photos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("$c items", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                }
                item {
                    val c = File(vault, "Videos").listFiles()?.size?:0
                    Card(Modifier.fillMaxWidth().clickable { folder = "Videos" }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(vertical = 22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(Color(0xFFFF2D55), CircleShape), contentAlignment = Alignment.Center) { Text("▶️", fontSize = 20.sp) }
                            Spacer(Modifier.height(10.dp)); Text("Videos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("$c files", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth().clickable { folder = "Secure" }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))) {
                        Column(Modifier.padding(vertical = 22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).background(Color(0xFF5AC8FA), CircleShape), contentAlignment = Alignment.Center) { Text("🛡️", fontSize = 26.sp) }
                            Spacer(Modifier.height(10.dp)); Text("Secure", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Vault locked", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    } else {
        val dir = File(vault, folder!!)
        val files = remember(refresh, folder) { dir.listFiles()?.sortedByDescending { it.lastModified() }?: emptyList() }
        val isPhotos = folder == "Photos" || folder == "Videos"

        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.background(Color(0xFF222222), CircleShape).clickable { folder = null }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("‹ Back", color = Color.White, fontSize = 14.sp) }
                    Spacer(Modifier.width(12.dp)); Text(folder!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Box(Modifier.background(Color.White, RoundedCornerShape(20.dp)).clickable { pick.launch("*/*") }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("+ Tambah", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }

            if (isPhotos) {
                Column {
                    Text("Foto", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
                    Text("Hari ini - Kemarin", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 10.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(1.dp), verticalArrangement = Arrangement.spacedBy(1.dp), horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxSize()) {
                        items(files.size) { idx ->
                            val f = files[idx]
                            val bmp = remember(f) { if (f.extension.lowercase() in listOf("jpg","jpeg","png")) try { BitmapFactory.decodeFile(f.absolutePath) } catch(e: Exception){ null } else null }
                            Box(Modifier.aspectRatio(1f).background(Color(0xFF222222)).clickable {
                                fullList = files
                                fullIndex = idx
                                showFull = true
                            }) {
                                if (bmp!= null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) { Text("▶️", fontSize = 16.sp) }
                                if (f.extension.lowercase() == "mp4") {
                                    Box(Modifier.align(Alignment.BottomEnd).padding(2.dp).background(Color(0x88000000), RoundedCornerShape(4.dp)).padding(3.dp, 1.dp)) { Text("0:15", color = Color.White, fontSize = 8.sp) }
                                }
                            }
                        }
                    }
                }
            } else {
                // INI YANG KEMARIN KEPOTONG - DOCX/PDF/XLSX LENGKAP
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(files) { f ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Text("${f.length()/1024} KB • ${java.text.SimpleDateFormat("dd MMM").format(Date(f.lastModified()))}", color = Color.Gray, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.background(Color.White, RoundedCornerShape(20.dp)).clickable { exportFile(f) }.padding(horizontal = 14.dp, vertical = 6.dp)) { Text("Buka", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    Box(Modifier.background(Color(0xFF30D158), RoundedCornerShape(20.dp)).clickable { exportFile(f) }.padding(horizontal = 14.dp, vertical = 6.dp)) { Text("Export", color = Color.White, fontSize = 11.sp) }
                                    Box(Modifier.background(Color(0xFF3A3A3C), CircleShape).clickable { f.delete(); refresh++ }.padding(8.dp)) { Text("🗑️", fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
