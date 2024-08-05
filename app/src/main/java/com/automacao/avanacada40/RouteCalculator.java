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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class RouteCalculator {

    private static final String TAG = RouteCalculator.class.getSimpleName();

    private GoogleMap map;
    private LatLng startLatLng;
    private LatLng destinationLatLng;
    private Context context;
    private float totalDistance;
    private long totalTime;

    public RouteCalculator(GoogleMap map, LatLng startLatLng, LatLng destinationLatLng, Context context) {
        this.map = map;
        this.startLatLng = startLatLng;
        this.destinationLatLng = destinationLatLng;
        this.context = context;
    }

    public void calculateRoute() {
        AsyncTask<Void, Integer, Boolean> task = new AsyncTask<Void, Integer, Boolean>() {
            private static final String TOAST_ERR_MSG = "Unable to calculate route";

            private final ArrayList<ArrayList<LatLng>> routes = new ArrayList<>();
            private final ArrayList<Float> routeDistances = new ArrayList<>();
            private final ArrayList<Long> routeTimes = new ArrayList<>();

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    String apiKey = "AIzaSyBCyVwhUeBZrcFLX8-PqsjYzvYMaVQvS_4";
                    String url = "https://maps.googleapis.com/maps/api/directions/xml?origin=" +
                            startLatLng.latitude + "," + startLatLng.longitude +
                            "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                            "&sensor=false&language=pt" +
                            "&mode=driving" +
                            "&alternatives=true" +
                            "&key=" + apiKey;

                    Log.d(TAG, "Request URL: " + url);

                    InputStream stream = new URL(url).openStream();
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    documentBuilderFactory.setIgnoringComments(true);
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(stream);
                    document.getDocumentElement().normalize();

                    String status = document.getElementsByTagName("status").item(0).getTextContent();
                    Log.d(TAG, "API response status: " + status);
                    if (!"OK".equals(status)) {
                        return false;
                    }

                    NodeList routeNodes = document.getElementsByTagName("route");
                    Log.d(TAG, "Number of routes: " + routeNodes.getLength());
                    for (int r = 0; r < routeNodes.getLength(); r++) {
                        Element routeElement = (Element) routeNodes.item(r);
                        NodeList legNodes = routeElement.getElementsByTagName("leg");
                        Element legElement = (Element) legNodes.item(0);

                        ArrayList<LatLng> lstLatLng = new ArrayList<>();
                        NodeList stepNodes = legElement.getElementsByTagName("step");

                        for (int i = 0; i < stepNodes.getLength(); i++) {
                            Node nodeStep = stepNodes.item(i);
                            if (nodeStep.getNodeType() == Node.ELEMENT_NODE) {
                                Element elementStep = (Element) nodeStep;
                                decodePolylines(elementStep.getElementsByTagName("points").item(0).getTextContent(), lstLatLng);
                            }
                        }

                        routes.add(lstLatLng);

                        String distanceText = legElement.getElementsByTagName("distance").item(0).getTextContent();
                        String durationText = legElement.getElementsByTagName("duration").item(0).getTextContent();

                        routeDistances.add(Float.parseFloat(distanceText.replaceAll("\\D+", "")) / 1000);
                        routeTimes.add(Long.parseLong(durationText.replaceAll("\\D+", "")));
                    }

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating route", e);
                    return false;
                }
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
                    map.clear();  // Limpa o mapa antes de desenhar as novas rotas

                    // Adiciona o marcador para o ponto de partida
                    MarkerOptions startMarker = new MarkerOptions()
                            .position(startLatLng)
                            .title("Ponto de Partida")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    map.addMarker(startMarker);

                    // Adiciona o marcador para o ponto de chegada
                    MarkerOptions endMarker = new MarkerOptions()
                            .position(destinationLatLng)
                            .title("Ponto de Chegada")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    map.addMarker(endMarker);

                    int[] colors = {Color.BLUE, Color.GREEN, Color.RED};
                    for (int i = 0; i < routes.size(); i++) {
                        PolylineOptions polylineOptions = new PolylineOptions();
                        polylineOptions.color(colors[i % colors.length]);
                        for (LatLng latLng : routes.get(i)) {
                            polylineOptions.add(latLng);
                        }
                        map.addPolyline(polylineOptions);

                        String distanceMessage = String.format(Locale.getDefault(), "Distância total: %.2f km", routeDistances.get(i));
                        String timeMessage = String.format(Locale.getDefault(), "Tempo total: %d segundos", routeTimes.get(i));
                        String combinedMessage = distanceMessage + "\n" + timeMessage;

                        Toast.makeText(context, combinedMessage, Toast.LENGTH_SHORT).show();

                        addGeofences(routes.get(i));  // Adiciona geocercas para cada rota
                    }
                }
            }
        };

        task.execute();
    }

    private void addGeofences(ArrayList<LatLng> route) {
        int numGeofences = 6;
        int routeSize = route.size();

        if (routeSize < 2) {
            // Se há menos de 2 pontos na rota, não podemos adicionar geocercas
            return;
        }

        // Adiciona geocercas igualmente espaçadas
        for (int i = 0; i < numGeofences; i++) {
            // Calcula o índice do ponto da rota para a geocerca
            int index = (routeSize - 1) * i / (numGeofences - 1);
            // Garante que o índice não está fora dos limites
            if (index >= routeSize) {
                index = routeSize - 1;
            }
            LatLng point = route.get(index);
            CircleOptions circleOptions = new CircleOptions()
                    .center(point)
                    .radius(30) // raio de 30 metros
                    .strokeColor(Color.rgb(128, 0, 128)) // cor roxa para a borda
                    .fillColor(Color.argb(100, 128, 0, 128));
            map.addCircle(circleOptions);
        }
    }

}
