package com.rian.dokumen
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RianIOSApp() }
    }
}

@Composable
fun RianIOSApp(){
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }

    // File Picker Launcher
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                val type = selected ?: "Docx"
                val ext = when(type){
                    "Docx" -> "docx"; "XLSX" -> "xlsx"; "PDF" -> "pdf"
                    "Photos" -> "jpg"; "Videos" -> "mp4"; else -> "dat"
                }
                val fileName = "${type}_${System.currentTimeMillis()}.$ext"
                context.contentResolver.openInputStream(it)?.use { input ->
                    context.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use { out ->
                        input.copyTo(out)
                    }
                }
                refresh++
            } catch (e: Exception) {}
        }
    }

    fun getFiles(type: String): List<File> {
        return context.fileList().filter { it.startsWith(type) }.map { File(context.filesDir, it) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF2F2F7))){
        if(!unlocked){
            Column(Modifier.background(Color.White).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center){
                Text("Rian Dokumen", fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("Private Vault • iOS Style", color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                Row{ repeat(6){ i -> Box(Modifier.size(12.dp).padding(2.dp).background(if(i<pin.length) Color.Black else Color.LightGray, RoundedCornerShape(50)))}}
                Spacer(Modifier.height(16.dp))
                val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","x"))
                keys.forEach{ row ->
                    Row{ row.forEach{ k ->
                        if(k=="") Spacer(Modifier.size(80.dp).padding(8.dp))
                        else Button(onClick={
                            if(k=="x"){ if(pin.isNotEmpty()) pin=pin.dropLast(1) }
                            else if(pin.length<6){ pin+=k; if(pin=="123456") unlocked=true }
                        }, modifier=Modifier.padding(8.dp).size(80.dp), shape = RoundedCornerShape(40.dp)){
                            Text(k, fontSize=22.sp)
                        }
                    }}
                }
            }
        } else {
            if(selected == null){
                Column(Modifier.fillMaxSize().padding(16.dp)){
                    Text("Rian Dokumen", fontSize=32.sp, fontWeight = FontWeight.Bold)
                    Text("Offline • Tap untuk tambah file", color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    val items = listOf(
                        Triple("Docx", "📄", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                        Triple("XLSX", "📊", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                        Triple("PDF", "📕", "application/pdf"),
                        Triple("Photos", "🖼️", "image/*"),
                        Triple("Videos", "🎬", "video/*"),
                        Triple("Secure", "🔒", "*/*")
                    )
                    items.chunked(2).forEach{ row ->
                        Row(Modifier.fillMaxWidth()){
                            row.forEach{ item ->
                                Card(Modifier.weight(1f).padding(8.dp).height(130.dp).clickable {
                                    selected = item.first
                                    val mime = arrayOf(item.third)
                                    launcher.launch(mime)
                                }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)){
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                                        Column(horizontalAlignment = Alignment.CenterHorizontally){
                                            Text(item.second, fontSize = 48.sp)
                                            Text(item.first, fontWeight = FontWeight.Medium)
                                            // hitung jumlah file
                                            val count = context.fileList().count { it.startsWith(item.first) }
                                            if(count>0) Text("$count file", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // HALAMAN LIST FILE
                val type = selected!!
                val files = remember(refresh) { getFiles(type) }
                Column(Modifier.fillMaxSize().background(Color.White).padding(16.dp)){
                    Row(verticalAlignment = Alignment.CenterVertically){
                        Button(onClick = { selected = null }) { Text("← Back") }
                        Spacer(Modifier.width(12.dp))
                        Text(type, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val mime = when(type){
                            "Docx" -> arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/msword")
                            "XLSX" -> arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel")
                            "PDF" -> arrayOf("application/pdf")
                            "Photos" -> arrayOf("image/*")
                            "Videos" -> arrayOf("video/*")
                            else -> arrayOf("*/*")
                        }
                        launcher.launch(mime)
                    }, modifier = Modifier.fillMaxWidth()) { Text("+ Tambah File $type") }
                    Spacer(Modifier.height(16.dp))
                    if(files.isEmpty()){
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                            Text("Belum ada file\nTap + untuk tambah", color = Color.Gray)
                        }
                    } else {
                        LazyColumn{
                            items(files){ f ->
                                Card(Modifier.fillMaxWidth().padding(vertical=4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))){
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically){
                                        Text(when(type){ "Docx"->"📄"; "XLSX"->"📊"; "PDF"->"📕"; "Photos"->"🖼️"; "Videos"->"🎬"; else->"🔒"}, fontSize=24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)){
                                            Text(f.name, maxLines=1)
                                            Text("${f.length()/1024} KB", fontSize=12.sp, color=Color.Gray)
                                        }
                                        TextButton(onClick = { f.delete(); refresh++ }) { Text("Hapus", color=Color.Red) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
