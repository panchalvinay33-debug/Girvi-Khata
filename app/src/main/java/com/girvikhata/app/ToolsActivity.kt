package com.girvikhata.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

class ToolsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToolsScreen(
                    openReports = { startActivity(Intent(this, ReportsActivity::class.java)) },
                    openBackup = { startActivity(Intent(this, BackupActivity::class.java)) },
                    close = ::finish,
                )
            }
        }
    }
}

@Composable
private fun ToolsScreen(openReports: () -> Unit, openBackup: () -> Unit, close: () -> Unit) {
    val navy = Color(0xFF171752)
    val purple = Color(0xFF5146B8)
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Lock, null, tint = navy)
        Spacer(Modifier.height(10.dp))
        Text("Girvi Khata Tools", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = navy)
        Text("Reports aur encrypted backup", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = openReports,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = purple),
                ) {
                    Icon(Icons.Default.Assessment, null)
                    Text("  Reports & Customer Khata")
                }
                Button(
                    onClick = openBackup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = navy),
                ) {
                    Icon(Icons.Default.Backup, null)
                    Text("  Encrypted Backup Banaye")
                }
                OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
