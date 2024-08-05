// CustomLocationManager.java
package com.automacao.avanacada40;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

public class CustomLocationManager {

    private static final String TAG = "CustomLocationManager";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long UPDATE_INTERVAL = 5000; // 5 segundos
    private static final int FASTEST_UPDATE_INTERVAL = 2000; // 2 segundos

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationCallbackListener callbackListener;
    private Handler handler;
    private Context context;
    private long startTime;
    private long endTime;
    private boolean isTimerRunning;

    public CustomLocationManager(Context context) {
        this.context = context;
        fusedLocationProviderClient = new FusedLocationProviderClient(context);
        createLocationCallback();
        handler = new Handler(Looper.getMainLooper());
        requestLocationPermission();
    }


    public void setLocationCallbackListener(LocationCallbackListener listener) {
        this.callbackListener = listener; // Define o callbackListener como o ouvinte de callback de localização fornecido
    }



    public void startLocationUpdatesInBackground() {
        // Inicializa o temporizador

        new Thread(() -> {
            Looper.prepare(); // Prepara o Looper para processar mensagens de localização
            if (checkLocationPermission()) { // Verifica se a permissão de localização foi concedida
                Log.d(TAG, "Location permission granted. Starting location updates..."); // Registra uma mensagem de log informando que a permissão de localização foi concedida
                startLocationUpdates(); // Inicia as atualizações de localização
                // Log.d(TAG, "fim da autalalização.");

            } else {
                Log.d(TAG, "Location permission not granted."); // Registra uma mensagem de log informando que a permissão de localização não foi concedida
            }
            Looper.loop(); // Inicia o loop do Looper para processar mensagens de localização
            // Encerra o temporizador quando o loop do Looper terminar

        }).start(); // Inicia a nova thread
    }



    public boolean checkLocationPermission() {
        // Verifica se a permissão ACCESS_FINE_LOCATION foi concedida
        boolean fineLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        // Verifica se a permissão ACCESS_COARSE_LOCATION foi concedida
        boolean coarseLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Verifica se pelo menos uma das permissões foi concedida
        boolean isPermissionGranted = fineLocationPermissionGranted || coarseLocationPermissionGranted;
        // Registra uma mensagem de log indicando se a permissão de localização foi concedida ou não
        Log.d(TAG, "Location permission granted: " + isPermissionGranted);

        return isPermissionGranted; // Retorna verdadeiro se pelo menos uma das permissões foi concedida, falso caso contrário
    }



    public void requestLocationPermission() {
        if (context instanceof Activity) { // Verifica se o contexto é uma instância de Activity
            ActivityCompat.requestPermissions((Activity) context, // Se for uma instância de Activity, solicita permissão de localização
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, // Permissões a serem solicitadas
                    LOCATION_PERMISSION_REQUEST_CODE); // Código de solicitação de permissão
        } else {
            Log.e(TAG, "Context is not an instance of Activity. Unable to request permissions."); // Se não for uma instância de Activity, registra uma mensagem de log informando que não é possível solicitar permissões
        }
    }



    private void startLocationUpdates() {
        if (checkLocationPermission()) { // Verifica se a permissão de localização foi concedida
            LocationRequest locationRequest = LocationRequest.create(); // Cria uma solicitação de localização
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Define a prioridade da solicitação como alta precisão
            locationRequest.setInterval(UPDATE_INTERVAL); // Define o intervalo de atualização
            locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL); // Define o intervalo mais rápido de atualização

            // Solicita atualizações de localização ao provedor de localização fundida usando a solicitação de localização criada
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Log.d(TAG, "Location updates started."); // Registra uma mensagem de log informando que as atualizações de localização foram iniciadas
        } else {
            Log.d(TAG, "Location updates cannot be started due to lack of permissions."); // Se a permissão de localização não foi concedida, registra uma mensagem de log informando que as atualizações de localização não podem ser iniciadas devido à falta de permissões
        }
    }



    public void stopLocationUpdates() {
        if (checkLocationPermission()) { // Verifica se a permissão de localização foi concedida
            fusedLocationProviderClient.removeLocationUpdates(locationCallback); // Remove as atualizações de localização registradas com o provedor de localização fundida
            Log.d(TAG, "Location updates stopped."); // Registra uma mensagem de log informando que as atualizações de localização foram interrompidas
        } else {
            Log.d(TAG, "Location updates cannot be stopped due to lack of permissions."); // Se a permissão de localização não foi concedida, registra uma mensagem de log informando que as atualizações de localização não podem ser interrompidas devido à falta de permissões
        }
    }



    private void createLocationCallback() {
        // Cria um novo LocationCallback e substitui seu método onLocationResult()
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) { // Verifica se o objeto LocationResult não é nulo
                    // Registra o tempo de início ao receber a atualização de localização
                    long startTime = System.nanoTime();

                    final Location location = locationResult.getLastLocation(); // Obtém a última localização do objeto LocationResult
                    if (location != null && callbackListener != null) { // Verifica se a localização não é nula e se o callbackListener não é nulo
                        // Envie a nova localização recebida para o callbackListener usando um Handler
                        handler.post(() -> callbackListener.onNewLocationReceived(location));
                    }

                    // Registra o tempo de término após receber a atualização de localização
                    long endTime = System.nanoTime();
                    // Calcula o tempo decorrido em milissegundos
                    long elapsedTime = endTime - startTime;
                    // Use o tempo decorrido conforme necessário, como registrá-lo em logs ou realizar outras ações
                    Log.d(TAG, "Tempo decorrido da atualização da localização: " + elapsedTime + " nanosegundos");
                }
            }
        };
    }

}