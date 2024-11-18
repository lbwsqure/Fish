package fish.app1;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NetworkUtils {

    private static final String SPECIES_API_BASE_URL = "https://api.gbif.org/v1/species";
    private static final String OCCURRENCE_API_BASE_URL = "https://api.gbif.org/v1/occurrence/search";

    /**
     * Fetch fish distribution data from GBIF API.
     *
     * @param fishName The scientific name of the fish.
     * @return A JsonArray containing distribution data.
     * @throws Exception If there is an error during the process.
     */
    public static JsonArray fetchFishDistribution(String fishName) throws Exception {
        Integer taxonKey = fetchTaxonKey(fishName);
        if (taxonKey == null) {
            throw new Exception("TaxonKey not found for species: " + fishName);
        }

        String queryUrl = OCCURRENCE_API_BASE_URL + "?taxonKey=" + taxonKey + "&limit=2000";
        URL url = new URL(queryUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP Error: " + conn.getResponseCode() + " at URL: " + queryUrl);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        return jsonResponse.getAsJsonArray("results");
    }

    private static Integer fetchTaxonKey(String fishName) throws Exception {
        String queryUrl = SPECIES_API_BASE_URL + "?name=" + URLEncoder.encode(fishName, "UTF-8");
        URL url = new URL(queryUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP Error: " + conn.getResponseCode() + " at URL: " + queryUrl);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        JsonArray results = jsonResponse.getAsJsonArray("results");
        if (results != null && results.size() > 0) {
            JsonObject firstResult = results.get(0).getAsJsonObject();
            if (firstResult.has("key")) {
                return firstResult.get("key").getAsInt();
            } else {
                throw new Exception("Key not found in the response for species: " + fishName);
            }
        } else {
            throw new Exception("No results found for species: " + fishName);
        }
    }
}
