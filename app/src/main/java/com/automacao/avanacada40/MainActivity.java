package com.automacao.avanacada40;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.biblioteca.Region;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationCallbackListener {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private MapView mMapView;
    private GoogleMap mMap;
    private Marker currentLocationMarker;
    private CustomLocationManager customLocationManager;
    private AutoCompleteTextView locationSearchTextView;
    private PlacesAutoCompleteAdapter autoCompleteAdapter;
    private com.google.android.libraries.places.api.net.PlacesClient placesClient;
    private TextView currentLatTextView;
    private TextView currentLngTextView;
    private FirebaseDataSaver firebaseDataSaver;
    private AutoCompleteTextView arrivalPointTextView;
    private LatLng startLatLng;
    private LatLng destinationLatLng;
    private GoogleMap googleMap;

    private boolean isUserTypingStartPoint = false; // Flag para verificar se o usuário está digitando no ponto de partida
    private boolean isUserTypingArrivalPoint = false;
    private Semaphore semaphore = new Semaphore(1);
    private ExecutorService executorService = Executors.newFixedThreadPool(2); // Dois threads para as duas operações

    // Use sua própria chave de API aqui
    private static final String PLACES_API_KEY = "AIzaSyBCyVwhUeBZrcFLX8-PqsjYzvYMaVQvS_4";

    private List<Region> regions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar o Places API
        Places.initialize(getApplicationContext(), PLACES_API_KEY);

        // Inicializar o MapView
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        // Inicializar o CustomLocationManager e registrar este activity como ouvinte de retorno de chamada
        customLocationManager = new CustomLocationManager(this);
        customLocationManager.setLocationCallbackListener(this);

        // Verificar se a permissão de localização foi concedida
        if (!customLocationManager.checkLocationPermission()) {
            // Se a permissão de localização não foi concedida, solicitar permissão
            customLocationManager.requestLocationPermission();
        } else {
            // Se a permissão de localização foi concedida, iniciar atualizações de localização em segundo plano
            customLocationManager.startLocationUpdatesInBackground();

            // Inicializar AutoCompleteTextView para pesquisa de localização (ponto de partida)
            TextInputLayout locationSearchLayout = findViewById(R.id.editTextStartPoint);
            locationSearchTextView = findViewById(R.id.starting_point);
            locationSearchLayout.setStartIconOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAutocompleteActivity(); // Passar true para ponto de partida
                }
            });

            // Inicializar AutoCompleteTextView para pesquisa de localização (ponto de chegada)
            TextInputLayout arrivalPointLayout = findViewById(R.id.editTextArrivalPoint);
            arrivalPointTextView = findViewById(R.id.Arrival_point);
            arrivalPointLayout.setStartIconOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAutocompleteActivity(); // Passar false para ponto de chegada
                }
            });

            // Inicializar placesClient e autoCompleteAdapter
            placesClient = Places.createClient(MainActivity.this);
            autoCompleteAdapter = new PlacesAutoCompleteAdapter(MainActivity.this, placesClient);

            // Configurar o adaptador para AutoCompleteTextView do ponto de partida
            locationSearchTextView.setAdapter(autoCompleteAdapter);
            locationSearchTextView.setThreshold(1); // Definir o número mínimo de caracteres para acionar as sugestões

            // Configurar o adaptador para AutoCompleteTextView do ponto de chegada
            arrivalPointTextView.setAdapter(autoCompleteAdapter);
            arrivalPointTextView.setThreshold(1); // Definir o número mínimo de caracteres para acionar as sugestões

            // Configurar o ouvinte de clique de item para AutoCompleteTextView do ponto de partida
            locationSearchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedLocation = autoCompleteAdapter.getItem(position);
                    if (selectedLocation != null) {
                        locationSearchTextView.setText(selectedLocation);
                        startLatLng = getLatLngFromAddress(selectedLocation); // Salvar coordenadas do ponto de partida
                    }
                }
            });

            // Configurar o ouvinte de clique de item para AutoCompleteTextView do ponto de chegada
            arrivalPointTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedLocation = autoCompleteAdapter.getItem(position);
                    if (selectedLocation != null) {
                        arrivalPointTextView.setText(selectedLocation);
                        destinationLatLng = getLatLngFromAddress(selectedLocation); // Salvar coordenadas do ponto de chegada
                    }
                }
            });

            // Adicionar TextWatchers para detectar quando o usuário está digitando
            locationSearchTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    isUserTypingStartPoint = true;
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    isUserTypingStartPoint = true;
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    isUserTypingStartPoint = false;
                }
            });

            arrivalPointTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    isUserTypingArrivalPoint = true;
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    isUserTypingArrivalPoint = true;
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    isUserTypingArrivalPoint = false;
                }
            });

            // Inicializar TextViews para exibir latitude e longitude atuais
            currentLatTextView = findViewById(R.id.latitudeTextView);
            currentLngTextView = findViewById(R.id.longitudeTextView);

            findViewById(R.id.buttonCoordenadas).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Salvar a localização atual
                        saveCurrentLocationToRegionsList();

                        // Verifique se as coordenadas de início e destino estão disponíveis
                        if (startLatLng != null && destinationLatLng != null) {
                            // Crie uma instância do RouteCalculator e calcule a rotaRouteCalculator routeCalculator = new RouteCalculator(mMap, startLatLng, destinationLatLng, this);
                            RouteCalculator routeCalculator = new RouteCalculator(mMap, startLatLng, destinationLatLng, MainActivity.this);
                            routeCalculator.calculateRoute();
                        } else {
                            // Mostre uma mensagem se as coordenadas não estiverem disponíveis
                            Toast.makeText(MainActivity.this, "Por favor, selecione ambos os pontos de início e destino", Toast.LENGTH_SHORT).show();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // Botão para salvar dados no Firebase
            findViewById(R.id.buttonBancoDeDados).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveCurrentLocationToFirebase();
                }
            });
        }
    }



        // Este método é chamado quando o fragmento é retomado e resume o MapView.
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    // Este método é chamado quando o fragmento é pausado e pausa o MapView.
    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    // Este método é chamado quando o fragmento é destruído e destrói o MapView e interrompe as atualizações de localização.
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        customLocationManager.stopLocationUpdates();
        firebaseDataSaver.stopThread();
        executorService.shutdown();

    }

    // Este método é chamado quando a memória está baixa e notifica o MapView.
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    // Este método é chamado quando o mapa está pronto para uso.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    private void updateLocationAddress(final double latitude, final double longitude, final boolean isStartingPoint) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                final String address = getRegionNameFromCoordinates(latitude, longitude);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isStartingPoint) {
                            locationSearchTextView.setText(address);
                        } else {
                            arrivalPointTextView.setText(address);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onNewLocationReceived(Location location) {
        updateMap(location);
        updateCurrentLocationTextViews(location.getLatitude(), location.getLongitude());

        // Atualize o AutoCompleteTextView do ponto de partida se estiver vazio e se o usuário não estiver digitando
        if (!isUserTypingStartPoint && locationSearchTextView.getText().toString().isEmpty()) {
            updateLocationAddress(location.getLatitude(), location.getLongitude(), true); // true para ponto de partida
        }

        // Não atualizar o AutoCompleteTextView do ponto de chegada automaticamente
    }

    private LatLng getLatLngFromAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                return new LatLng(addr.getLatitude(), addr.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }




    private void updateMap(Location location) {
        // Verifica se o mapa está disponível
        if (mMap != null) {
            // Converte a localização em coordenadas de latitude e longitude
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Verifica se o marcador da localização atual ainda não foi criado
            if (currentLocationMarker == null) {
                // Se o marcador ainda não existe, cria um novo marcador na posição atual e adiciona ao mapa
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Minha Localização"));
            } else {
                // Se o marcador já existe, atualiza apenas sua posição
                currentLocationMarker.setPosition(latLng);
            }

            // Move a câmera do mapa para a nova posição com um nível de zoom de 15
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        }
    }




    private void startAutocompleteActivity() {
        // Defina os campos para especificar quais tipos de dados de local devem ser retornados após o usuário fazer uma seleção.
        List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME);

        // Inicie a intenção de autocompletar.
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Verifica se o resultado corresponde à solicitação de autocompletar.
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            // Verifica se a operação foi concluída com sucesso.
            if (resultCode == Activity.RESULT_OK) {
                // Obtém o local selecionado do intent.
                com.google.android.libraries.places.api.model.Place place = Autocomplete.getPlaceFromIntent(data);
                // Atualiza o TextView com o nome do local.
                locationSearchTextView.setText(place.getName());
                // Use o objeto place para obter detalhes como place.getName(), place.getLatLng(), etc.
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Lidar com erro de autocompletar
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // O usuário cancelou a operação.
            }
        }
    }





    private void updateCurrentLocationTextViews(double latitude, double longitude) {
        // Formata os valores de latitude e longitude e os define nos TextViews correspondentes.
        currentLatTextView.setText("Lat: " + String.format("%.7f", latitude));
        currentLngTextView.setText("Long: " + String.format("%.7f", longitude));
    }



    private void saveCurrentLocationToRegionsList() throws InterruptedException {
        // Verifica se o marcador da localização atual não é nulo
        if (currentLocationMarker != null) {
            // Extrai as coordenadas de latitude e longitude dos TextViews correspondentes
            double latitude = Double.parseDouble(currentLatTextView.getText().toString().replace("Lat: ", "").replace(",", "."));
            double longitude = Double.parseDouble(currentLngTextView.getText().toString().replace("Long: ", "").replace(",", "."));

            // Obter o nome da região usando o serviço de geocodificação
            String regionName = getRegionNameFromCoordinates(latitude, longitude);

            if (regionName != null) {
                // Acessando a lista de regiões diretamente da MainActivity


                // Iniciar uma nova thread para adicionar a região à lista
                ConsultDatabase thread = new ConsultDatabase(regions, regionName, latitude, longitude, semaphore);
                thread.start();
                try {
                    // Aguardar até que a thread termine sua execução
                    thread.join();
                } catch (InterruptedException e) {
                    Log.e("Home Fragment", "Erro execução da Thread de Consulta de Banco  " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Exibe um Toast informando sobre a indisponibilidade do nome da região
                Toast.makeText(this, "Nome da região não disponível", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Exibe um Toast informando sobre a indisponibilidade da localização atual
            Toast.makeText(this, "Localização atual não disponível", Toast.LENGTH_SHORT).show();
        }
    }




    private void saveCurrentLocationToFirebase() {
        // Verifica se o marcador da localização atual não é nulo
        if (currentLocationMarker != null) {
            // Verifica se a thread FirebaseDataSaver ainda não foi iniciada
            if (firebaseDataSaver == null || !firebaseDataSaver.isAlive()) {
                // Criar uma instância de FirebaseDataSaver
                firebaseDataSaver = new FirebaseDataSaver(this, regions, semaphore);

                firebaseDataSaver.start();
            } else {
                // Registra uma mensagem de log se a thread já estiver em execução
                Log.d("HomeFragment", "Thread já está em execução.");
            }

            // Notificar a thread quando a lista não estiver mais vazia
            synchronized (regions) {
                regions.notify();
            }

        } else {
            // Exibe um Toast informando ao usuário sobre a indisponibilidade da localização atual
            Toast.makeText(this, "Localização atual não disponível.", Toast.LENGTH_SHORT).show();
        }
    }





    private String getRegionNameFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String fullAddress = "";

        try {
            // Esperar 1 segundo antes de tentar novamente
            Thread.sleep(1000);
            // Tentar obter o endereço correspondente às coordenadas
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            // Se houver endereços retornados e não estiver vazio
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder stringBuilder = new StringBuilder();
                // Construir o endereço completo a partir das linhas do endereço
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    stringBuilder.append(address.getAddressLine(i));
                    // Adicionar vírgula entre as linhas do endereço, exceto para a última linha
                    if (i < address.getMaxAddressLineIndex()) {
                        stringBuilder.append(", ");
                    }
                }

                // Remover espaços em branco extras e definir como o nome da região
                fullAddress = stringBuilder.toString().trim();
            }
        } catch (IOException e) {
            // Lidar com exceções de E/S, como falha na conexão de rede
            Log.e("Home Fragment", "Erro na primeira tentativa de obter o endereço completo a partir das coordenadas: " + e.getMessage());
            try {
                // Tentar novamente após um curto intervalo de tempo
                Thread.sleep(1000);
                // Nova tentativa de obter o endereço
                List<Address> addressesRetry = geocoder.getFromLocation(latitude, longitude, 1);
                if (addressesRetry != null && !addressesRetry.isEmpty()) {
                    Address addressRetry = addressesRetry.get(0);

                    StringBuilder stringBuilderRetry = new StringBuilder();
                    // Construir o endereço completo a partir das linhas do endereço
                    for (int i = 0; i <= addressRetry.getMaxAddressLineIndex(); i++) {
                        stringBuilderRetry.append(addressRetry.getAddressLine(i));
                        // Adicionar vírgula entre as linhas do endereço, exceto para a última linha
                        if (i < addressRetry.getMaxAddressLineIndex()) {
                            stringBuilderRetry.append(", ");
                        }
                    }

                    // Remover espaços em branco extras e definir como o nome da região
                    fullAddress = stringBuilderRetry.toString().trim();
                }
            } catch (InterruptedException | IOException ex) {
                // Lidar com exceções ao tentar novamente
                Log.e("Home Fragment", "Erro na segunda tentativa de obter o endereço completo: " + ex.getMessage());
            }
        } catch (Exception e) {
            // Lidar com outras exceções imprevistas
            Log.e("Home Fragment", "Erro ao obter o endereço completo a partir das coordenadas: " + e.getMessage());
        }

        // Retorna o nome da região correspondente ou uma string vazia se não puder ser obtido
        return fullAddress;
    }

}