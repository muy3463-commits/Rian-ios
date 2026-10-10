package com.rian.jajancrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var level by remember { mutableStateOf(1) }
            var score by remember { mutableStateOf(0) }
            var grid by remember { mutableStateOf(List(36){ listOf("K","O","S","L").random() }) }
            var sel by remember { mutableStateOf<Int?>(null) }

            Column(Modifier.fillMaxSize().background(Color(0xFFFFF3E0)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rian's Jajan Crush", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Seblak di Dalam! Level $level Skor $score", fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.size(340.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(3.dp, Color.Gray, RoundedCornerShape(12.dp)).padding(4.dp)) {
                    items(36){ i ->
                        val isSel = sel==i
                        Box(Modifier.padding(2.dp).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(if(isSel) Color.Yellow else Color(0xFFFFCCBC)).clickable {
                            if(sel==null) sel=i else {
                                val tmp = grid[sel!!]
                                val newGrid = grid.toMutableList()
                                newGrid[sel!!] = grid[i]
                                newGrid[i] = tmp
                                grid = newGrid
                                sel=null
                                score+=10
                            }
                        }, contentAlignment = Alignment.Center){
                            val txt = when(grid[i]){ "K"->"🟢"; "O"->"🟡"; "S"->"🌶️"; "L"->"🟤"; else->"⚪" }
                            Text(txt, fontSize = 22.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("K=Klepon O=Onde S=SEBLAK L=Lapis", fontSize = 11.sp)
                Button(onClick = { grid = List(36){ listOf("K","O","S","L").random() }; score=0 }){ Text("Acak Lagi") }
            }
        }
    }
}
