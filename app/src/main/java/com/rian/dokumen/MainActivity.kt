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
    val vaultDir = File(context.filesDir, "vault").apply { if(!exists()) mkdirs() }
    var refresh by remember { mutableStateOf(0) }

    fun exportFile(f: File) {
        try {
            val ext = f.extension.lowercase()
            val mime = when(ext){ "jpg","jpeg"->"image/jpeg" "png"->"image/png" "mp4"->"video/mp4" "pdf"->"application/pdf" else->"application/octet-stream" }
            val collection = when(ext){ "jpg","jpeg","png"->MediaStore.Images.Media.EXTERNAL_CONTENT_URI "mp4","mkv","mov"->MediaStore.Video.Media.EXTERNAL_CONTENT_URI else->MediaStore.Downloads.EXTERNAL_CONTENT_URI }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, when(ext){ "jpg","jpeg","png"->Environment.DIRECTORY_PICTURES+"/RianDokumen" "mp4"->Environment.DIRECTORY_MOVIES+"/RianDokumen" else->Environment.DIRECTORY_DOWNLOADS+"/RianDokumen" })
            }
            context.contentResolver.insert(collection, values)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { inp -> inp.copyTo(out) } }
                Toast.makeText(context, "✅ Export Berhasil!", Toast.LENGTH_SHORT).show()
            }
        } catch(e: Exception){ Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null && selectedFolder!=null){
            val folder = File(vaultDir, selectedFolder!!).apply{ if(!exists()) mkdirs() }
            val ext = when(selectedFolder){ "Photos"->"jpg" "Videos"->"mp4" "Pdf"->"pdf" else->"docx" }
            val dest = File(folder, "${selectedFolder}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            refresh++
        }
    }

    if(viewingFile!=null){
        Box(Modifier.fillMaxSize().background(Color.Black)){
            val file = viewingFile!!
            if(file.extension.lowercase() in listOf("jpg","jpeg","png","webp")){
                val bmp = remember(file){ BitmapFactory.decodeFile(file.absolutePath) }
                bmp?.let{ Image(bitmap = it.asImageBitmap(), contentDescription=null, modifier=Modifier.fillMaxSize()) }
            } else {
                AndroidView(factory={ VideoView(it).apply{ setVideoURI(Uri.fromFile(file)); setOnPreparedListener{mp-> mp.isLooping=true; start()} } }, modifier=Modifier.fillMaxSize())
            }
            FilledTonalButton(onClick={viewingFile=null}, modifier=Modifier.align(Alignment.TopStart).padding(16.dp)){ Text("✕ Tutup") }
        }
        return
    }

    if(!isLogin){
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement=Arrangement.Center, horizontalAlignment=Alignment.CenterHorizontally){
            Card(shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer), modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally){
                    Text("🔐", style=MaterialTheme.typography.displayMedium)
                    Text("Rian Dokumen", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value=pinInput, onValueChange={pinInput=it}, label={Text("PIN")}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick={if(pinInput=="123456") isLogin=true}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp)){ Text("Masuk") }
                }
            }
        }
        return
    }

    if(selectedFolder==null){
        Column(Modifier.fillMaxSize().padding(16.dp)){
            Text("Vault Kamu", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            val folders = listOf(Triple("Docx","📄","Docx"), Triple("Pdf","📕","PDF"), Triple("Photos","🖼️","Foto"), Triple("Videos","🎬","Video"))
            LazyVerticalGrid(columns=GridCells.Fixed(2)){
                items(folders){ (id,icon,label) ->
                    Card(modifier=Modifier.padding(8.dp).clickable{selectedFolder=id}, shape=RoundedCornerShape(20.dp), elevation=CardDefaults.cardElevation(4.dp)){
                        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally){
                            Text(icon, style=MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(label, fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    } else {
        val folder = File(vaultDir, selectedFolder!!)
        val files = remember(refresh, selectedFolder){ folder.listFiles()?.toList()?: emptyList() }
        Column(Modifier.fillMaxSize().padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                FilledTonalButton(onClick={selectedFolder=null}, shape=RoundedCornerShape(12.dp)){ Text("←") }
                Spacer(Modifier.width(12.dp))
                Text(selectedFolder!!, style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${files.size} file", style=MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick={picker.launch("*/*")}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp)){ Text("＋ Tambah File") }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
                items(files){ f ->
                    ElevatedCard(shape=RoundedCornerShape(16.dp), modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(14.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text(when(f.extension.lowercase()){ "jpg","jpeg","png"->"🖼️" "mp4","mkv"->"🎬" "pdf"->"📕" else->"📄" }, modifier=Modifier.padding(end=8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(f.name, fontWeight=FontWeight.Medium, maxLines=1)
                                    Text("${f.length()/1024} KB", style=MaterialTheme.typography.bodySmall, color=Color.Gray)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                FilledButton(onClick={
                                    if(f.extension.lowercase() in listOf("jpg","jpeg","png","webp","mp4","mkv","mov")) viewingFile=f
                                    else{
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply{ setDataAndType(uri,"*/*"); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                        context.startActivity(android.content.Intent.createChooser(intent,"Buka"))
                                    }
                                }, modifier=Modifier.weight(1f), shape=RoundedCornerShape(10.dp)){ Text("👁️ Buka") }
                                FilledTonalButton(onClick={exportFile(f)}, modifier=Modifier.weight(1f), shape=RoundedCornerShape(10.dp)){ Text("⬇️ Export") }
                                OutlinedButton(onClick={f.delete(); refresh++}, modifier=Modifier.weight(1f), shape=RoundedCornerShape(10.dp), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.Red)){ Text("🗑️") }
                            }
                        }
                    }
                }
            }
        }
    }
}
