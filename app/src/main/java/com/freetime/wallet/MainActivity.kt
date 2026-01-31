package com.freetime.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import wallet.core.jni.CoinType
import wallet.core.jni.HDWallet
import wallet.core.jni.Mnemonic
import java.util.Currency
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("TrustWalletCore")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreetimeWalletTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun FreetimeWalletTheme(content: @Composable () -> Unit) {
    Material3Theme {
        content()
    }
}

@Composable
fun MainScreen() {
    var passphrase by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Address will appear here") }
    var price by remember { mutableStateOf("Price will appear here") }
    var wallet by remember { mutableStateOf<HDWallet?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun generateWallet() {
        try {
            val newWallet = HDWallet(128, "")
            val mnemonic = newWallet.mnemonic()
            
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Mnemonic", mnemonic))
            
            Toast.makeText(context, "Passphrase copied to clipboard!", Toast.LENGTH_SHORT).show()
            passphrase = mnemonic
            wallet = newWallet
            
            val btcAddress = newWallet.getAddressForCoin(CoinType.BITCOIN)
            address = "BTC Address:\n$btcAddress"
            
            scope.launch {
                fetchCryptoPrice("bitcoin") { newPrice ->
                    price = newPrice
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error generating wallet: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginWithPassphrase() {
        try {
            if (!Mnemonic.isValid(passphrase.trim())) {
                Toast.makeText(context, "Invalid passphrase", Toast.LENGTH_SHORT).show()
                return
            }
            
            val newWallet = HDWallet(passphrase.trim(), "")
            val btcAddress = newWallet.getAddressForCoin(CoinType.BITCOIN)
            address = "BTC Address:\n$btcAddress"
            wallet = newWallet
            
            scope.launch {
                fetchCryptoPrice("bitcoin") { newPrice ->
                    price = newPrice
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error logging in: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun performLogout() {
        wallet = null
        passphrase = ""
        address = "Address will appear here"
        price = "Price will appear here"
        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Freetime Wallet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Enter Passphrase") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { generateWallet() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Generate Wallet")
            }
            
            Button(
                onClick = { loginWithPassphrase() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Login")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = address,
                    fontSize = 14.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = price,
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = { performLogout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Logout")
        }
    }
}

suspend fun fetchCryptoPrice(coinId: String, onPriceReceived: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val currencyCode = Currency.getInstance(Locale.getDefault()).currencyCode
            val client = OkHttpClient()
            val url = "https://api.coingecko.com/api/v3/simple/price?ids=$coinId&vs_currencies=${currencyCode.lowercase()}"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext
            val jsonObject = JSONObject(json)
            val price = jsonObject.getJSONObject(coinId).getDouble(currencyCode.lowercase())
            
            withContext(Dispatchers.Main) {
                onPriceReceived("${coinId.uppercase()} Price: $price $currencyCode")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onPriceReceived("Price unavailable")
            }
        }
    }
}
