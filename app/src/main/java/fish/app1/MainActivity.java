package fish.app1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
/**
 * MainActivity is the entry point of the fish distribution application.
 * It fetches fish data from the server and displays a distribution map in a WebView.
 *
 * @author Zhiyang Zhang
 * @andrewid zhiyang3
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SERVER_URL = "https://reimagined-space-palm-tree-q79vw5p7g977f4qg4-8080.app.github.dev/fishSelector/";
    private WebView webView;
    private Spinner spinnerFish;
    /**
     * List of popular North American fish species with their scientific and common names.
     */
    private static final String[][] fishSpecies = {
            {"Micropterus salmoides", "Largemouth Bass"},
            {"Micropterus dolomieu", "Smallmouth Bass"},
            {"Oncorhynchus mykiss", "Rainbow Trout"},
            {"Salmo trutta", "Brown Trout"},
            {"Salvelinus fontinalis", "Brook Trout"},
            {"Ictalurus punctatus", "Channel Catfish"},
            {"Sander vitreus", "Walleye"}
    };
    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerFish = findViewById(R.id.spinnerFish);
        Button buttonFetchData = findViewById(R.id.buttonFetchData);
        webView = findViewById(R.id.webView);

        setupSpinner();
        configureWebView();

        buttonFetchData.setOnClickListener(v -> fetchFishData());
    }


    /**
     * Configures the WebView settings for displaying HTML content.
     */
    private void configureWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setVisibility(View.GONE); // 初始隐藏 WebView
    }

    /**
     * Sets up the Spinner with fish species data and configures the adapter.
     */
    private void setupSpinner() {
        String[] fishNames = new String[fishSpecies.length];
        for (int i = 0; i < fishSpecies.length; i++) {
            fishNames[i] = fishSpecies[i][1]; // 使用俗名
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fishNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFish.setAdapter(adapter);
    }

    /**
     * Fetches data for the selected fish species and sends a GET request to the server.
     */
    private void fetchFishData() {
        int selectedPosition = spinnerFish.getSelectedItemPosition();
        String fishScientificName = fishSpecies[selectedPosition][0];

        String url = SERVER_URL + fishScientificName.replace(" ", "%20");

        Log.d(TAG, "Requesting URL: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage(), e);
                runOnUiThread(() -> showErrorToast("Failed to connect to the server. Check if the server is running.\nError: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : null;

                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());
                Log.d(TAG, "Response body: " + responseData);

                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Response body:\n" + responseData,
                        Toast.LENGTH_LONG).show());

                if (response.isSuccessful() && responseData != null) {
                    try {
                        parseServerResponse(responseData);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response JSON: " + e.getMessage(), e);
                        runOnUiThread(() -> showErrorToast("Error parsing server response. Please check the server output format.\nError: " + e.getMessage()));
                    }
                } else {
                    runOnUiThread(() -> showErrorToast("Server error: " + response.message() + "\nError code: " + response.code()));
                }
            }
        });
    }

    /**
     * Parses the server response JSON and displays the distribution map in a WebView.
     *
     * @param responseData JSON response from the server
     * @throws Exception if parsing the JSON fails
     */
    private void parseServerResponse(String responseData) throws Exception {
        JsonObject jsonResponse = JsonParser.parseString(responseData).getAsJsonObject();
        String mapHtml = jsonResponse.get("mapHtml").getAsString();
        JsonArray distributionData = jsonResponse.getAsJsonArray("distributionData");

        Log.d(TAG, "Map HTML: " + mapHtml);
        Log.d(TAG, "Distribution Data Size: " + distributionData.size());

        runOnUiThread(() -> {
            // 显示 WebView 并加载 HTML 内容
            webView.setVisibility(View.VISIBLE);
            webView.loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null);

            Toast.makeText(MainActivity.this,
                    "Found " + distributionData.size() + " distribution points",
                    Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Displays an error message using a Toast.
     *
     * @param message The error message to display
     */
    private void showErrorToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
