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
        setContent { RiansJajanCrushGame() }
    }
}

@Composable
fun RiansJajanCrushGame() {
    // DALAM GAME UDAH SEBLAK LUR!
    // 🟢 Klepon, 🟡 Onde, 🌶️ Seblak, 🟤 Lapis, ⚪ Cireng, 🟠 Batagor
    val jajanan = listOf("🟢", "🟡", "🌶️", "🟤", "⚪", "🟠")
    val namaJajan = listOf("Klepon", "Onde", "Seblak", "Lapis", "Cireng", "Batagor")

    var level by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf(500) }
    var moves by remember { mutableStateOf(25) }
    var grid by remember { mutableStateOf(generateGrid(jajanan, level)) }
    var selected by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var showWin by remember { mutableStateOf(false) }
    var lastMatch by remember { mutableStateOf("") }

    val bgColor = Color(0xFFFFF3E0)

    Column(
        modifier = Modifier.fillMaxSize().background(bgColor).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER - FOTO LU JADI BACKGROUND DISINI NANTI
        Text("Rian's Jajan Crush", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
        Text("Level $level/100 | Dalam: Klepon, Onde, SEBLAK, Lapis", fontSize = 11.sp, color = Color(0xFF6D4C41))
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF4E342E)).padding(10.dp),
            horizontalArrangement = Arrangement.SpaceAround) {
            Text("🎯 $target", color = Color.White, fontWeight = FontWeight.Bold)
            Text("❤️ $moves", color = Color.White, fontWeight = FontWeight.Bold)
            Text("⭐ $score", color = Color(0xFFFFCA28), fontWeight = FontWeight.Bold)
        }

        if(lastMatch.isNotEmpty()){
            Text("Match: $lastMatch!", fontSize = 14.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        // PAPAN 6x6
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFD7CCC8)).border(4.dp, Color(0xFF8D6E63), RoundedCornerShape(16.dp)).padding(8.dp)) {
            LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.size(360.dp)) {
                items(36) { index ->
                    val r = index / 6
                    val c = index % 6
                    val isSelected = selected == Pair(r,c)
                    val item = grid[r][c]
                    // Warna beda kalo seblak biar keliatan pedes
                    val bg = when{
                        isSelected -> Color.Yellow
                        item == "🌶️" -> Color(0xFFFFCDD2)
                        else -> Color.White
                    }
                    Box(
                        Modifier.padding(2.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                          .background(bg)
                          .border(1.dp, Color(0xFFBCAAA4), RoundedCornerShape(8.dp))
                          .clickable {
                                if(moves <= 0) return@clickable
                                if(selected == null) selected = Pair(r,c)
                                else {
                                    val (sr, sc) = selected!!
                                    if(kotlin.math.abs(sr-r)+kotlin.math.abs(sc-c)==1) {
                                        val newGrid = grid.map { it.toMutableList() }.toMutableList()
                                        val tmp = newGrid[sr][sc]
                                        newGrid[sr][sc] = newGrid[r][c]
                                        newGrid[r][c] = tmp

                                        val matched = findMatches(newGrid)
                                        if(matched.isNotEmpty()) {
                                            // Deteksi apa yang di match
                                            val firstMatch = newGrid[matched.first().first][matched.first().second]
                                            val idx = jajanan.indexOf(firstMatch)
                                            lastMatch = if(idx>=0) namaJajan[idx] else ""

                                            grid = newGrid
                                            score += matched.size * 10 * level
                                            moves--

                                            var tempGrid = newGrid
                                            for((mr,mc) in matched) tempGrid[mr][mc] = ""
                                            tempGrid = fillEmpty(tempGrid, jajanan, level)
                                            grid = tempGrid

                                            if(score >= target) {
                                                if(level==100) showWin=true
                                                else { level++; target+=300+level*50; moves=25+level/5; score=0; grid=generateGrid(jajanan, level); lastMatch="" }
                                            }
                                        }
                                    }
                                    selected=null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(item, fontSize = 28.sp) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🟢Klepon 🟡Onde 🌶️Seblak 🟤Lapis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("⚪Cireng 🟠Batagor - 100 Level Offline", fontSize = 10.sp, color = Color.Gray)
            Text("Background: Pasangan Batik + Desa 🇮🇩", fontSize = 10.sp, color = Color.Gray)
        }

        if(showWin) {
            AlertDialog(onDismissRequest = {}, title = {Text("TAMAT 100 LEVEL! 🎉🌶️")},
            text = {Text("Lu jagoan Seblak Crush! Semua jajanan abis!")},
            confirmButton = { Button(onClick = {level=1; target=500; score=0; moves=25; grid=generateGrid(jajanan, level); showWin=false; lastMatch=""}){Text("Main Lagi")} })
        }
    }
}

fun generateGrid(items: List<String>, level: Int): List<List<String>> {
    val diff = if(level<15) items.take(4) else if(level<40) items.take(5) else items
    return List(6){ List(6){ diff[Random.nextInt(diff.size)] } }
}
fun findMatches(grid: List<MutableList<String>>): Set<Pair<Int,Int>> {
    val matches = mutableSetOf<Pair<Int,Int>>()
    for(r in 0..5) for(c in 0..3) if(grid[r][c]!="" && grid[r][c]==grid[r][c+1] && grid[r][c]==grid[r][c+2]) { matches.add(r to c); matches.add(r to c+1); matches.add(r to c+2) }
    for(c in 0..5) for(r in 0..3) if(grid[r][c]!="" && grid[r][c]==grid[r+1][c] && grid[r][c]==grid[r+2][c]) { matches.add(r to c); matches.add(r+1 to c); matches.add(r+2 to c) }
    return matches
}
fun fillEmpty(grid: List<MutableList<String>>, items: List<String>, level: Int): List<MutableList<String>> {
    val diff = if(level<15) items.take(4) else if(level<40) items.take(5) else items
    for(r in 0..5) for(c in 0..5) if(grid[r][c]=="") grid[r][c]=diff[Random.nextInt(diff.size)]
    return grid
}
