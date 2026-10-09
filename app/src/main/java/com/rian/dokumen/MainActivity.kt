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
    Box(Modifier.fillMaxSize().background(Color(0xFFF2F2F7)), contentAlignment=Alignment.Center){
        if(!unlocked){
            Column(Modifier.background(Color.White, RoundedCornerShape(32.dp)).padding(28.dp), horizontalAlignment=Alignment.CenterHorizontally){
                Text("Rian Dokumen", fontWeight=FontWeight.Bold, fontSize=22.sp)
                Text("Private Vault • iOS Style", color=Color.Gray, fontSize=12.sp)
                Spacer(Modifier.height(24.dp))
                Row{ repeat(6){ i -> Box(Modifier.padding(6.dp).size(14.dp).background(if(i<pin.length) Color(0xFF007AFF) else Color(0xFFE5E5EA), RoundedCornerShape(50))) } }
                Spacer(Modifier.height(16.dp))
                val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","x"))
                keys.forEach{ row ->
                    Row{ row.forEach{ k ->
                        if(k=="") Spacer(Modifier.size(84.dp).padding(6.dp))
                        else Button(onClick={
                            if(k=="x"){ if(pin.isNotEmpty()) pin=pin.dropLast(1) }
                            else if(pin.length<6){ pin+=k; if(pin.length==6){ if(pin=="123456") unlocked=true; else pin="" } }
                        }, modifier=Modifier.padding(6.dp).size(72.dp), shape=RoundedCornerShape(36.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFEFEFF4), contentColor=Color.Black)){
                            Text(k, fontSize=22.sp)
                        }
                    }}
                }
            
            }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp)){
                Text("Rian Dokumen", fontSize=32.sp, fontWeight=FontWeight.Bold)
                Text("Offline • Docx Xlsx Pdf Foto Video", color=Color.Gray)
                Spacer(Modifier.height(24.dp))
                val items = listOf("Docx","XLSX","PDF","Photos","Videos","Secure")
                items.chunked(2).forEach{ row ->
                    Row{ row.forEach{ item ->
                        Card(Modifier.weight(1f).padding(8.dp).height(110.dp), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(Color.White)){
                            Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){ Text(item) }
                        }
                    }}
                }
            }
        }
    }
}
