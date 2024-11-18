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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // 日志标签
    private WebView webView;

    // 热门北美鱼类名称
    private static final String[][] fishSpecies = {
            {"Micropterus salmoides", "Largemouth Bass"},
            {"Micropterus dolomieu", "Smallmouth Bass"},
            {"Oncorhynchus mykiss", "Rainbow Trout"},
            {"Salmo trutta", "Brown Trout"},
            {"Salvelinus fontinalis", "Brook Trout"},
            {"Ictalurus punctatus", "Channel Catfish"},
            {"Sander vitreus", "Walleye"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinnerFish = findViewById(R.id.spinnerFish);
        Button buttonFetchData = findViewById(R.id.buttonFetchData);
        webView = findViewById(R.id.webView);

        // 设置 Spinner 数据
        String[] fishNames = new String[fishSpecies.length];
        for (int i = 0; i < fishSpecies.length; i++) {
            fishNames[i] = fishSpecies[i][1]; // 使用俗名
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fishNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFish.setAdapter(adapter);

        // 启用 WebView 的 JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // 按钮点击事件
        buttonFetchData.setOnClickListener(view -> {
            int selectedPosition = spinnerFish.getSelectedItemPosition();
            String fishScientificName = fishSpecies[selectedPosition][0]; // 获取学名
            String fishCommonName = fishSpecies[selectedPosition][1];

            // 调用网络工具类获取数据
            new Thread(() -> {
                try {
                    Log.d(TAG, "Fetching data for: " + fishScientificName);

                    JsonArray distribution = NetworkUtils.fetchFishDistribution(fishScientificName);

                    // 加载地图到 WebView
                    runOnUiThread(() -> {
                        if (distribution != null && distribution.size() > 0) {
                            Log.d(TAG, "Data fetched successfully: " + distribution.size() + " points");

                            // 显示 WebView 并加载地图
                            webView.setVisibility(View.VISIBLE);
                            loadLeafletMap(distribution, fishCommonName);
                        } else {
                            Log.w(TAG, "No distribution data found for " + fishCommonName);
                            Toast.makeText(this, "No distribution data found for " + fishCommonName, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error fetching data for " + fishCommonName, e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }

    private void loadLeafletMap(JsonArray locations, String fishName) {
        try {
            // 构建标记点
            StringBuilder markers = new StringBuilder();
            for (int i = 0; i < locations.size(); i++) {
                JsonObject location = locations.get(i).getAsJsonObject();
                double latitude = location.has("decimalLatitude") ? location.get("decimalLatitude").getAsDouble() : 0.0;
                double longitude = location.has("decimalLongitude") ? location.get("decimalLongitude").getAsDouble() : 0.0;

                if (latitude != 0.0 && longitude != 0.0) {
                    markers.append("L.marker([").append(latitude).append(", ").append(longitude).append("]).addTo(map);\n");
                    Log.d(TAG, "Marker added: Lat=" + latitude + ", Lng=" + longitude);
                } else {
                    Log.w(TAG, "Invalid marker data: Lat=" + latitude + ", Lng=" + longitude);
                }
            }

            // 构建 HTML 内容
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "  <title>Fish Distribution</title>\n" +
                    "  <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                    "  <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                    "  <style>#map { width: 100%; height: 300px; }</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <h3>Distribution for " + fishName + "</h3>\n" +
                    "  <div id=\"map\"></div>\n" +
                    "  <script>\n" +
                    "    var map = L.map('map').setView([39.8283, -98.5795], 4);\n" +
                    "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                    "      maxZoom: 19,\n" +
                    "      attribution: '&copy; OpenStreetMap contributors'\n" +
                    "    }).addTo(map);\n" +
                    markers.toString() +
                    "  </script>\n" +
                    "</body>\n" +
                    "</html>";

            // 加载 HTML 到 WebView
            Log.d(TAG, "Generated HTML:\n" + html);
            webView.setWebViewClient(new WebViewClient());
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        } catch (Exception e) {
            Log.e(TAG, "Error loading map for " + fishName, e);
        }
    }
}
