package com.freetime.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.Currency;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.Mnemonic;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("TrustWalletCore");
    }

    private EditText inputPassphrase;
    private TextView addressDisplay, priceDisplay;
    private HDWallet wallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputPassphrase = findViewById(R.id.inputPassphrase);
        addressDisplay = findViewById(R.id.addressDisplay);
        priceDisplay = findViewById(R.id.priceDisplay);

        MaterialButton btnGen = findViewById(R.id.btnGen);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        btnGen.setOnClickListener(v -> generateWallet());
        btnLogin.setOnClickListener(v -> loginWithPassphrase());

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                performLogout();
                return true;
            }
            return false;
        });
    }

    private void generateWallet() {
        wallet = new HDWallet(128, "");
        String mnemonic = wallet.mnemonic();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Mnemonic", mnemonic));

        Toast.makeText(this, "Passphrase copied to clipboard!", Toast.LENGTH_SHORT).show();
        inputPassphrase.setText(mnemonic);

        String btcAddress = wallet.getAddressForCoin(CoinType.BITCOIN);
        addressDisplay.setText("BTC Address:\n" + btcAddress);

        fetchCryptoPrice("bitcoin");
    }

    private void loginWithPassphrase() {
        String mnemonic = inputPassphrase.getText().toString().trim();
        if (!Mnemonic.isValid(mnemonic)) {
            Toast.makeText(this, "Invalid passphrase", Toast.LENGTH_SHORT).show();
            return;
        }
        wallet = new HDWallet(mnemonic, "");
        String btcAddress = wallet.getAddressForCoin(CoinType.BITCOIN);
        addressDisplay.setText("BTC Address:\n" + btcAddress);
        fetchCryptoPrice("bitcoin");
    }

    private void fetchCryptoPrice(String coinId) {
        String currencyCode = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=" + currencyCode.toLowerCase();

        new Thread(() -> {
            try {
                Response response = client.newCall(new Request.Builder().url(url).build()).execute();
                String json = response.body().string();
                JSONObject jsonObject = new JSONObject(json);
                double price = jsonObject.getJSONObject(coinId).getDouble(currencyCode.toLowerCase());

                runOnUiThread(() -> priceDisplay.setText(
                        coinId.toUpperCase() + " Price: " + price + " " + currencyCode
                ));
            } catch (Exception e) {
                runOnUiThread(() -> priceDisplay.setText("Price unavailable"));
            }
        }).start();
    }

    private void performLogout() {
        wallet = null;
        inputPassphrase.setText("");
        addressDisplay.setText("Address will appear here");
        priceDisplay.setText("Price will appear here");
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
    }
}