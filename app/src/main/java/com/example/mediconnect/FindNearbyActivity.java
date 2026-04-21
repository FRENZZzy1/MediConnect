package com.example.mediconnect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FindNearbyActivity extends AppCompatActivity {

    private static final String TAG = "FindNearbyActivity";
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private double currentLat = 0;
    private double currentLng = 0;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    private Polyline currentRoute = null;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Configuration.getInstance().setUserAgentValue(getPackageName());
            setContentView(R.layout.activity_find_nearby);

            mapView = findViewById(R.id.map_view);

            if (mapView == null) {
                Toast.makeText(this, "Layout error: map_view not found", Toast.LENGTH_LONG).show();
                return;
            }

            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(12.0);

            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndSearch();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST);
            }

        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            Toast.makeText(this, "Startup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────
    // LOCATION
    // ─────────────────────────────────────────────────────────

    private void getUserLocationAndSearch() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        showLocationOnMap(location.getLatitude(), location.getLongitude());
                    } else {
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getCurrentLocation failed: " + e.getMessage());
                    requestFreshLocation();
                });
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                fusedLocationClient.removeLocationUpdates(this);
                if (!result.getLocations().isEmpty()) {
                    android.location.Location loc = result.getLocations().get(0);
                    showLocationOnMap(loc.getLatitude(), loc.getLongitude());
                } else {
                    showLocationOnMap(14.5995, 120.9842);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, getMainLooper());
        } catch (Exception e) {
            showLocationOnMap(14.5995, 120.9842);
        }
    }

    // ─────────────────────────────────────────────────────────
    // MAP
    // ─────────────────────────────────────────────────────────

    private void showLocationOnMap(double lat, double lng) {
        currentLat = lat;
        currentLng = lng;

        GeoPoint userPoint = new GeoPoint(lat, lng);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(userPoint);

        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(userPoint);
        userMarker.setTitle("📍 You are here");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(userMarker);
        mapView.invalidate();

        retryCount = 0;
        mapView.postDelayed(() -> searchNearbyClinics(lat, lng), 2000);
    }

    // ─────────────────────────────────────────────────────────
    // OVERPASS API - FIND CLINICS
    // ─────────────────────────────────────────────────────────

    private void searchNearbyClinics(double lat, double lng) {
        Log.d(TAG, "Calling Overpass API... attempt " + (retryCount + 1));

        String query = "[out:json][timeout:25];" +
                "(" +
                "node[amenity=clinic](around:3000," + lat + "," + lng + ");" +
                "node[amenity=hospital](around:3000," + lat + "," + lng + ");" +
                "node[amenity=doctors](around:3000," + lat + "," + lng + ");" +
                ");" +
                "out body;";

        String url = "https://overpass-api.de/api/interpreter?data="
                + android.net.Uri.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MediConnectApp/1.0 (student project)")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        Toast.makeText(FindNearbyActivity.this,
                                "Retrying... (" + retryCount + "/" + MAX_RETRIES + ")",
                                Toast.LENGTH_SHORT).show();
                        mapView.postDelayed(() -> searchNearbyClinics(currentLat, currentLng), 3000);
                    } else {
                        Toast.makeText(FindNearbyActivity.this,
                                "Could not load clinics. Check internet.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;

                String json = response.body().string();

                // 🔥 ALWAYS LOG RESPONSE
                Log.d(TAG, "Response received");

                runOnUiThread(() -> {

                    // 🚨 If NOT JSON → log real error
                    if (!response.isSuccessful() || json.trim().startsWith("<")) {

                        logServerError(response, json);

                        if (retryCount < MAX_RETRIES) {
                            retryCount++;
                            Toast.makeText(FindNearbyActivity.this,
                                    "Server busy... retrying (" + retryCount + "/" + MAX_RETRIES + ")",
                                    Toast.LENGTH_SHORT).show();

                            mapView.postDelayed(() ->
                                    searchNearbyClinics(currentLat, currentLng), 3000);
                        } else {
                            Toast.makeText(FindNearbyActivity.this,
                                    "Server unavailable. Check Logcat.",
                                    Toast.LENGTH_LONG).show();
                        }

                    } else {
                        parseClinicsAndAddMarkers(json);
                    }
                });
            }
        });
    }

    private void parseClinicsAndAddMarkers(String json) {
        if (json.trim().startsWith("<")) {
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                Toast.makeText(this,
                        "Server busy, retrying... (" + retryCount + "/" + MAX_RETRIES + ")",
                        Toast.LENGTH_SHORT).show();
                mapView.postDelayed(() -> searchNearbyClinics(currentLat, currentLng), 3000);
            } else {
                Toast.makeText(this, "Server unavailable. Please try again later.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        try {
            JSONObject root = new JSONObject(json);
            JSONArray elements = root.getJSONArray("elements");

            if (elements.length() == 0) {
                Toast.makeText(this, "No clinics found nearby.", Toast.LENGTH_LONG).show();
                return;
            }

            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                if (!element.has("lat") || !element.has("lon")) continue;

                double lat = element.getDouble("lat");
                double lng = element.getDouble("lon");

                JSONObject tags = element.optJSONObject("tags");
                String name = (tags != null && tags.has("name"))
                        ? tags.getString("name") : "Clinic";
                String type = (tags != null && tags.has("amenity"))
                        ? tags.getString("amenity") : "clinic";
                String address = (tags != null && tags.has("addr:street"))
                        ? tags.getString("addr:street") : "";

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lng));
                marker.setTitle("🏥 " + name);
                marker.setSnippet("Type: " + type
                        + (address.isEmpty() ? "" : "\n📍 " + address)
                        + "\n\nTap to get directions →");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                final double clinicLat = lat;
                final double clinicLng = lng;
                final String clinicName = name;

                // Tap marker → draw route
                marker.setOnMarkerClickListener((m, mapV) -> {
                    m.showInfoWindow();
                    Toast.makeText(this,
                            "Getting route to " + clinicName + "...",
                            Toast.LENGTH_SHORT).show();
                    getRoute(currentLat, currentLng, clinicLat, clinicLng);
                    return true;
                });

                mapView.getOverlays().add(marker);
            }

            mapView.invalidate();
            Toast.makeText(this,
                    "✅ " + elements.length() + " clinics found! Tap a marker for directions.",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage(), e);
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                mapView.postDelayed(() -> searchNearbyClinics(currentLat, currentLng), 3000);
            } else {
                Toast.makeText(this, "Error loading clinics.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // OSRM ROUTING - FREE, NO API KEY NEEDED
    // ─────────────────────────────────────────────────────────

    private void getRoute(double fromLat, double fromLng, double toLat, double toLng) {

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + fromLng + "," + fromLat + ";"
                + toLng + "," + toLat
                + "?overview=full&geometries=geojson";

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Routing network failure", e);

                runOnUiThread(() ->
                        Toast.makeText(FindNearbyActivity.this,
                                "Routing failed: no internet or server unreachable",
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if (response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(FindNearbyActivity.this,
                                    "Empty routing response",
                                    Toast.LENGTH_SHORT).show());
                    return;
                }

                String json = response.body().string();

                runOnUiThread(() -> {

                    // 🔥 IMPORTANT FIX
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "HTTP Error: " + response.code());
                        Toast.makeText(FindNearbyActivity.this,
                                "Routing server error: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject root = new JSONObject(json);

                        if (!root.optString("code").equals("Ok")) {
                            Toast.makeText(FindNearbyActivity.this,
                                    "No route found",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        drawRoute(json);

                    } catch (Exception e) {
                        Log.e(TAG, "Invalid routing JSON", e);
                        Toast.makeText(FindNearbyActivity.this,
                                "Routing parse error",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void drawRoute(String json) {
        try {
            JSONObject root = new JSONObject(json);
            String code = root.getString("code");

            if (!code.equals("Ok")) {
                Toast.makeText(this, "No route found.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject route = root.getJSONArray("routes").getJSONObject(0);

            // Extract coordinates from GeoJSON
            JSONArray coordinates = route
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            List<GeoPoint> routePoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lng = coord.getDouble(0); // GeoJSON = [lng, lat]
                double lat = coord.getDouble(1);
                routePoints.add(new GeoPoint(lat, lng));
            }

            // Remove old route
            if (currentRoute != null) {
                mapView.getOverlays().remove(currentRoute);
            }

            // Draw route line
            currentRoute = new Polyline();
            currentRoute.setPoints(routePoints);
            currentRoute.getOutlinePaint().setColor(Color.parseColor("#1565C0")); // blue
            currentRoute.getOutlinePaint().setStrokeWidth(12f);
            currentRoute.getOutlinePaint().setAlpha(210);

            mapView.getOverlays().add(1, currentRoute); // add below markers
            mapView.invalidate();

            // Show distance + duration
            double distanceMeters = route.getDouble("distance");
            double durationSeconds = route.getDouble("duration");

            String distText = distanceMeters >= 1000
                    ? String.format("%.1f km", distanceMeters / 1000)
                    : String.format("%.0f m", distanceMeters);
            String durText = String.format("%.0f mins", durationSeconds / 60);

            Toast.makeText(this,
                    "🗺 " + distText + " • ~" + durText + " by car",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "drawRoute error: " + e.getMessage(), e);
            Toast.makeText(this, "Could not draw route.", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────
    // PERMISSIONS & LIFECYCLE
    // ─────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndSearch();
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("Location permission is needed to find nearby clinics.")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            Intent intent = new Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (d, w) -> finish())
                        .show();
            }
        }
    }

    private void logServerError(Response response, String body) {
        try {
            // Log HTTP status
            Log.e(TAG, "🔴 HTTP Code: " + response.code());
            Log.e(TAG, "🔴 Message: " + response.message());

            // Log headers (very useful for rate limit info)
            Log.e(TAG, "🔴 Headers:\n" + response.headers().toString());

            // Log raw response body (THIS is what you need)
            Log.e(TAG, "🔴 Raw Response:\n" + body);

            // Optional: detect common errors
            if (body.contains("Too Many Requests")) {
                Log.e(TAG, "⚠️ Rate limit exceeded (429)");
            } else if (body.contains("server load")) {
                Log.e(TAG, "⚠️ Server overloaded");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error logging server response", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}