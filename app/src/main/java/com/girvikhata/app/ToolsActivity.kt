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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.RecordStoreLoadState

class ToolsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storeState = EncryptedRecordStore(applicationContext).loadState()
        val recordsAvailable = storeState is RecordStoreLoadState.Ready
        val warning = (storeState as? RecordStoreLoadState.Corrupt)?.let {
            "Local encrypted records verify nahi hue. Reports/backup blocked hain; pehle verified .gkb restore karein."
        }
        setContent {
            MaterialTheme {
                ToolsScreen(
                    recordsAvailable = recordsAvailable,
                    warning = warning,
                    openSafety = { startActivity(Intent(this, DataSafetyActivity::class.java)) },
                    openOwnerSettings = { startActivity(Intent(this, OwnerSettingsActivity::class.java)) },
                    openMasters = { startActivity(Intent(this, MasterCatalogActivity::class.java)) },
                    openMasterWorkflow = { startActivity(Intent(this, MasterWorkflowActivity::class.java)) },
                    openAdvancedWorkflow = { startActivity(Intent(this, AdvancedWorkflowActivity::class.java)) },
                    openSettlement = { startActivity(Intent(this, SettlementCenterActivity::class.java)) },
                    openReports = { startActivity(Intent(this, ReportsActivity::class.java)) },
                    openBackup = { startActivity(Intent(this, BackupActivity::class.java)) },
                    openRestore = { startActivity(Intent(this, RestoreActivity::class.java)) },
                    openPinRecovery = { startActivity(Intent(this, PinRecoveryActivity::class.java)) },
                    close = ::finish,
                )
            }
        }
    }
}

@Composable
private fun ToolsScreen(
    recordsAvailable: Boolean,
    warning: String?,
    openSafety: () -> Unit,
    openOwnerSettings: () -> Unit,
    openMasters: () -> Unit,
    openMasterWorkflow: () -> Unit,
    openAdvancedWorkflow: () -> Unit,
    openSettlement: () -> Unit,
    openReports: () -> Unit,
    openBackup: () -> Unit,
    openRestore: () -> Unit,
    openPinRecovery: () -> Unit,
    close: () -> Unit,
) {
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
        Text("Daily entry, masters, safety, settlement, reports aur backup", color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                warning?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                Button(onClick = openSafety, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF138A4A))) {
                    Icon(Icons.Default.Security, null); Text("  Data Safety Status")
                }
                Button(onClick = openOwnerSettings, enabled = recordsAvailable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))) {
                    Icon(Icons.Default.Settings, null); Text("  Owner Settings")
                }
                Button(onClick = openMasters, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))) {
                    Icon(Icons.Default.Tune, null); Text("  Business Masters")
                }
                Button(onClick = openAdvancedWorkflow, enabled = recordsAvailable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) {
                    Icon(Icons.Default.EditNote, null); Text("  Advanced Multi-Item & Custom Split")
                }
                OutlinedButton(onClick = openMasterWorkflow, enabled = recordsAvailable, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EditNote, null); Text("  Classic Master-Assisted Entry")
                }
                Button(onClick = openSettlement, enabled = recordsAvailable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4D00))) {
                    Icon(Icons.Default.ReceiptLong, null); Text("  Settlement & Release Center")
                }
                Button(onClick = openReports, enabled = recordsAvailable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = purple)) {
                    Icon(Icons.Default.Assessment, null); Text("  Reports & Customer Khata")
                }
                Button(onClick = openBackup, enabled = recordsAvailable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = navy)) {
                    Icon(Icons.Default.Backup, null); Text("  Encrypted Backup Banaye")
                }
                Button(onClick = openRestore, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E))) {
                    Icon(Icons.Default.Restore, null); Text("  Backup Restore / Import")
                }
                OutlinedButton(onClick = openPinRecovery, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.LockReset, null); Text("  Purana PIN Kaam Nahi Kar Raha")
                }
                Text("Critical entry ke baad external verified backup banayein.", color = Color.Gray, fontSize = 12.sp)
                OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
