package com.freetime.wallet

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
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import wallet.core.jni.CoinType
import wallet.core.jni.HDWallet

class DashboardActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("TrustWalletCore")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreetimeWalletTheme {
                DashboardScreen()
            }
        }
    }
}

interface BlockchairApi {
    @GET("bitcoin/dashboards/address/{address}")
    fun getBitcoinBalance(@Path("address") address: String): Call<BlockchairResponse>
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var addresses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var balance by remember { mutableStateOf("Loading balance...") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val mnemonic = (context as DashboardActivity).intent.getStringExtra("mnemonic") ?: return@LaunchedEffect
            val wallet = HDWallet(mnemonic, "")
            
            val addressMap = mapOf(
                "BTC" to wallet.getAddressForCoin(CoinType.BITCOIN),
                "ETH" to wallet.getAddressForCoin(CoinType.ETHEREUM),
                "LTC" to wallet.getAddressForCoin(CoinType.LITECOIN),
                "BCH" to wallet.getAddressForCoin(CoinType.BITCOINCASH),
                "USDT" to wallet.getAddressForCoin(CoinType.ETHEREUM) // USDT on Ethereum
            )
            
            addresses = addressMap
            
            // Fetch BTC balance using Retrofit
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.blockchair.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val api = retrofit.create(BlockchairApi::class.java)
            val btcAddress = addressMap["BTC"] ?: return@LaunchedEffect
            
            api.getBitcoinBalance(btcAddress).enqueue(object : Callback<BlockchairResponse> {
                override fun onResponse(call: Call<BlockchairResponse>, response: Response<BlockchairResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val satoshis = response.body()!!.data[btcAddress]?.address?.balance ?: 0L
                        val btc = satoshis / 100_000_000.0
                        balance = "Balance: $btc BTC"
                    } else {
                        balance = "Failed to fetch balance"
                    }
                    isLoading = false
                }
                
                override fun onFailure(call: Call<BlockchairResponse>, t: Throwable) {
                    balance = "Failed to fetch balance: ${t.message}"
                    isLoading = false
                    Toast.makeText(context, "Failed to fetch balance", Toast.LENGTH_SHORT).show()
                }
            })
            
        } catch (e: Exception) {
            balance = "Error: ${e.message}"
            isLoading = false
        }
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
            text = "Wallet Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            addresses.forEach { (coin, address) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "$coin Address:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = address,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = balance,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
