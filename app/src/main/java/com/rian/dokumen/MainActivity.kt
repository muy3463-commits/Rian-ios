package com.rian.dokumen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RianIOSApp() }
    }
}
@Composable
fun RianIOSApp(){
    var pin by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
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
            Column(Modifier.fillMaxSize().padding(16.dp)){
                Text("Rian Dokumen", fontSize=32.sp, fontWeight = FontWeight.Bold)
                Text("Offline • Docx Xlsx Pdf Foto Video", color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                // DATA + ICON + WARNA
                val items = listOf(
                    Triple("Docx", "📄", Color(0xFF0A84FF)),
                    Triple("XLSX", "📊", Color(0xFF34C759)),
                    Triple("PDF", "📕", Color(0xFFFF3B30)),
                    Triple("Photos", "🖼️", Color(0xFF5AC8FA)),
                    Triple("Videos", "🎬", Color(0xFFAF52DE)),
                    Triple("Secure", "🔒", Color(0xFF8E8E93))
                )
                items.chunked(2).forEach{ row ->
                    Row(Modifier.fillMaxWidth()){
                        row.forEach{ item ->
                            Card(Modifier.weight(1f).padding(8.dp).height(120.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)){
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                                    Column(horizontalAlignment = Alignment.CenterHorizontally){
                                        Text(item.second, fontSize = 48.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(item.first, fontWeight = FontWeight.Medium)
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
