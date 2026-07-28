package com.girvikhata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GirviNavy = Color(0xFF171752)
private val GirviPurple = Color(0xFF5146B8)
private val SecureGreen = Color(0xFF138A4A)
private val ScreenBackground = Color(0xFFF6F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GirviKhataApp() }
    }
}

@Composable
private fun GirviKhataApp() {
    var unlocked by remember { mutableStateOf(false) }

    MaterialTheme {
        if (unlocked) {
            DashboardScreen()
        } else {
            LockScreen(onUnlock = { unlocked = true })
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GirviNavy)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = Color(0xFFFFC54D),
            modifier = Modifier.size(72.dp),
        )
        Text("Girvi Khata", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Aapki digital tijori", color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.height(36.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Lock, null, tint = GirviPurple, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(12.dp))
                Text("App Unlock Kare", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Text("Secure PIN module agle milestone mein judega", color = Color.Gray)
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onUnlock,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
                ) {
                    Icon(Icons.Default.Fingerprint, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Prototype Unlock")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen() {
    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GirviNavy),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Namaste, Malik Ji", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Single-owner secure mode", color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = SecureGreen)
                    Text(" Protected", color = SecureGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, null, tint = GirviPurple)
                    Spacer(Modifier.size(10.dp))
                    Text("Customer, mobile ya girvi number khoje", color = Color.Gray)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Naya Girvi", Icons.Default.AddCircle, Modifier.weight(1f))
                QuickAction("Payment Lein", Icons.Default.Payments, Modifier.weight(1f))
            }

            Text("Aaj ka summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Active Girvi", "0", Modifier.weight(1f))
                SummaryCard("Total Principal", "₹0", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Aaj Received", "₹0", Modifier.weight(1f))
                SummaryCard("Due Accounts", "0", Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF7EF), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "Encrypted backup foundation planned • real business data GitHub par kabhi nahi jayega",
                    color = SecureGreen,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Button(
        onClick = {},
        modifier = modifier.height(92.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GirviNavy),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = GirviPurple)
            Spacer(Modifier.height(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GirviNavy)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    MaterialTheme { DashboardScreen() }
}
