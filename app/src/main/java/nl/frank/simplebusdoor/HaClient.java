package nl.frank.simplebusdoor;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class HaClient {
    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int READ_TIMEOUT_MS = 3500;
    static final String ENTRY_DOOR = "binary_sensor.simplebus2_bridge_entry_door";
    static final String FLOOR_DOOR = "binary_sensor.simplebus2_bridge_floor_door";
    static final String HEARTBEAT = "binary_sensor.simplebus2_bridge_heartbeat";
    static final String RING_OPENED = "binary_sensor.simplebus2_bridge_ring_to_open_opened";
    static final String RING_SWITCH = "switch.simplebus2_bridge_ring_to_open";
    static final String OPEN_DOOR = "button.simplebus2_bridge_open_door";
    static final String CONFIG_MODE = "button.simplebus2_bridge_start_config_mode";

    private HaClient() {}

    static JSONObject getState(String baseUrl, String token, String entityId) throws Exception {
        HttpURLConnection con = request(baseUrl + "/api/states/" + entityId, token, "GET");
        int code = con.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("HA status " + code);
        return new JSONObject(new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    static JSONObject getJson(String url) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpURLConnection con = request(url, null, "GET");
                int code = con.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                return new JSONObject(new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                last = e;
                if (attempt == 0) Thread.sleep(1200);
            }
        }
        throw last;
    }

    static JSONObject postForm(String url, Map<String, String> fields) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpURLConnection con = request(url, null, "POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                StringBuilder body = new StringBuilder();
                for (Map.Entry<String, String> field : fields.entrySet()) {
                    if (body.length() > 0) body.append('&');
                    body.append(URLEncoder.encode(field.getKey(), "UTF-8"));
                    body.append('=');
                    body.append(URLEncoder.encode(field.getValue() == null ? "" : field.getValue(), "UTF-8"));
                }
                try (OutputStream out = con.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = con.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                return new JSONObject(new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                last = e;
                if (attempt == 0) Thread.sleep(1200);
            }
        }
        throw last;
    }

    static void callService(String baseUrl, String token, String domain, String service, String entityId) throws Exception {
        callServiceJson(baseUrl, token, domain, service, "{\"entity_id\":\"" + entityId + "\"}");
    }

    static void callServiceJson(String baseUrl, String token, String domain, String service, String jsonBody) throws Exception {
        HttpURLConnection con = request(baseUrl + "/api/services/" + domain + "/" + service, token, "POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = con.getOutputStream()) {
            out.write(body);
        }
        int code = con.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("HA service status " + code);
    }

    private static HttpURLConnection request(String url, String token, String method) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(CONNECT_TIMEOUT_MS);
        con.setReadTimeout(READ_TIMEOUT_MS);
        con.setRequestMethod(method);
        if (token != null && !token.isEmpty()) con.setRequestProperty("Authorization", "Bearer " + token);
        con.setRequestProperty("Accept", "application/json");
        return con;
    }
}
