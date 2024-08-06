package com.automacao.avanacada40;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class RouteCalculator {

    private static final String TAG = RouteCalculator.class.getSimpleName();

    private GoogleMap map;
    private LatLng startLatLng;
    private LatLng destinationLatLng;
    private Context context;

    public RouteCalculator(GoogleMap map, LatLng startLatLng, LatLng destinationLatLng, Context context) {
        this.map = map;
        this.startLatLng = startLatLng;
        this.destinationLatLng = destinationLatLng;
        this.context = context;
    }

    public void calculateRoute() {
        AsyncTask<Void, Integer, Boolean> task = new AsyncTask<Void, Integer, Boolean>() {
            private static final String TOAST_ERR_MSG = "Unable to calculate route";

            private final List<ArrayList<LatLng>> routes = new ArrayList<>();
            private final List<Float> routeDistances = new ArrayList<>();
            private final List<Long> routeTimes = new ArrayList<>();

            @Override
            protected Boolean doInBackground(Void... params) {
                HttpURLConnection urlConnection = null;
                try {
                    String apiKey = "AIzaSyBCyVwhUeBZrcFLX8-PqsjYzvYMaVQvS_4";
                    String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                            startLatLng.latitude + "," + startLatLng.longitude +
                            "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                            "&alternatives=true" +
                            "&key=" + apiKey;

                    URL url = new URL(urlStr);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    String response = new Scanner(inputStream).useDelimiter("\\A").next();

                    JSONObject jsonResponse = new JSONObject(response);
                    String status = jsonResponse.getString("status");
                    if (!"OK".equals(status)) {
                        return false;
                    }

                    JSONArray routesArray = jsonResponse.getJSONArray("routes");
                    Log.d(TAG, "Number of routes: " + routesArray.length());
                    for (int r = 0; r < routesArray.length(); r++) {
                        JSONObject route = routesArray.getJSONObject(r);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPoints = overviewPolyline.getString("points");

                        ArrayList<LatLng> lstLatLng = new ArrayList<>();
                        decodePolylines(encodedPoints, lstLatLng);
                        routes.add(lstLatLng);

                        JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                        String distanceText = leg.getJSONObject("distance").getString("text");
                        String durationText = leg.getJSONObject("duration").getString("text");

                        Log.e(TAG, "Distancia da rota: " + distanceText);
                        Log.e(TAG, "Tempo da rota: " + durationText);
                        routeDistances.add(getDistanceFromText(distanceText)); // Convert to km
                        routeTimes.add(getDurationFromText(durationText)); // Convert to MIN
                    }

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating route", e);
                    return false;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            private float getDistanceFromText(String text) {
                String numericText = text.replaceAll("[^\\d.]", "");
                return Float.parseFloat(numericText);
            }

            private long getDurationFromText(String text) {
                long totalMinutes = 0;
                String[] parts = text.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].endsWith("hour") || parts[i].endsWith("hours")) {
                        totalMinutes += Long.parseLong(parts[i - 1]) * 60;
                    } else if (parts[i].endsWith("min") || parts[i].endsWith("mins")) {
                        totalMinutes += Long.parseLong(parts[i - 1]);
                    }
                }
                return totalMinutes;
            }

            private void decodePolylines(String encodedPoints, ArrayList<LatLng> lstLatLng) {
                int index = 0;
                int lat = 0, lng = 0;

                while (index < encodedPoints.length()) {
                    int b, shift = 0, result = 0;
                    do {
                        b = encodedPoints.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);

                    int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lat += dlat;
                    shift = 0;
                    result = 0;

                    do {
                        b = encodedPoints.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);

                    int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lng += dlng;

                    lstLatLng.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) {
                    Toast.makeText(context, TOAST_ERR_MSG, Toast.LENGTH_SHORT).show();
                } else {
                    int[] colors = {Color.BLUE, Color.GREEN, Color.RED};

                    String[] routeOptions = new String[routes.size()];
                    for (int i = 0; i < routes.size(); i++) {
                        String distanceText = String.format(Locale.getDefault(), "%.2f km", routeDistances.get(i));
                        String durationText = String.format(Locale.getDefault(), "%d min", routeTimes.get(i));
                        routeOptions[i] = "Rota " + (i + 1) + ": " + distanceText + ", Tempo: " + durationText;
                    }

                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Escolha uma rota")
                            .setItems(routeOptions, (dialog, which) -> {
                                map.clear();  // Limpa o mapa antes de desenhar a nova rota

                                // Adiciona o marcador de partida
                                map.addMarker(new MarkerOptions()
                                        .position(startLatLng)
                                        .title("Partida")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                                // Adiciona o marcador de destino
                                map.addMarker(new MarkerOptions()
                                        .position(destinationLatLng)
                                        .title("Destino")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                                // Desenha a rota selecionada
                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.color(colors[which % colors.length]);
                                for (LatLng latLng : routes.get(which)) {
                                    polylineOptions.add(latLng);
                                }
                                map.addPolyline(polylineOptions);

                                // Ajusta a câmera para mostrar a rota completa
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng latLng : routes.get(which)) {
                                    builder.include(latLng);
                                }
                                builder.include(startLatLng);
                                builder.include(destinationLatLng);
                                LatLngBounds bounds = builder.build();
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

                                String distanceMessage = String.format(Locale.getDefault(), "Distância total: %.2f km", routeDistances.get(which));
                                String timeMessage = String.format(Locale.getDefault(), "Tempo total: %d minutos", routeTimes.get(which));
                                String combinedMessage = distanceMessage + "\n" + timeMessage;

                                Toast.makeText(context, combinedMessage, Toast.LENGTH_SHORT).show();
                                addGeofences(routes.get(which));
                            })
                            .show();
                }
            }

            private void addGeofences(List<LatLng> route) {
                int geofenceCount = 6;
                float[] intervals = new float[geofenceCount - 1];
                float totalDistance = 0;
                LatLng prevLatLng = route.get(0);

                for (int i = 1; i < route.size(); i++) {
                    LatLng currentLatLng = route.get(i);
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(
                            prevLatLng.latitude, prevLatLng.longitude,
                            currentLatLng.latitude, currentLatLng.longitude,
                            results);
                    totalDistance += results[0];
                    prevLatLng = currentLatLng;
                }

                float interval = totalDistance / (geofenceCount - 1);

                for (int i = 0; i < geofenceCount - 1; i++) {
                    intervals[i] = interval * (i + 1);
                }

                intervals[geofenceCount - 2] = totalDistance;

                prevLatLng = route.get(0);
                int intervalIndex = 0;
                float accumulatedDistance = 0;

                for (int i = 1; i < route.size(); i++) {
                    LatLng currentLatLng = route.get(i);
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(
                            prevLatLng.latitude, prevLatLng.longitude,
                            currentLatLng.latitude, currentLatLng.longitude,
                            results);
                    accumulatedDistance += results[0];
                    if (accumulatedDistance >= intervals[intervalIndex]) {
                        CircleOptions circleOptions = new CircleOptions()
                                .center(currentLatLng)
                                .radius(30)
                                .strokeColor(Color.MAGENTA)
                                .fillColor(Color.argb(64, 128, 0, 128)); // cor roxa com transparência
                        map.addCircle(circleOptions);

                        intervalIndex++;
                        if (intervalIndex >= intervals.length) {
                            break;
                        }
                    }

                    prevLatLng = currentLatLng;
                }

                // Adiciona geocercas para o ponto de partida e destino
                map.addCircle(new CircleOptions()
                        .center(route.get(0))
                        .radius(30)
                        .strokeColor(Color.MAGENTA)
                        .fillColor(Color.argb(64, 128, 0, 128)));
                map.addCircle(new CircleOptions()
                        .center(route.get(route.size() - 1))
                        .radius(30)
                        .strokeColor(Color.MAGENTA)
                        .fillColor(Color.argb(64, 128, 0, 128)));
            }
        };

        task.execute();
    }
}
