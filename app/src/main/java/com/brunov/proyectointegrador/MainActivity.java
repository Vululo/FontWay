package com.brunov.proyectointegrador;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.SearchView;

import com.brunov.proyectointegrador.api.ApiClient;
import com.brunov.proyectointegrador.api.ApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
//import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap Map;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int RADIUS_METERS = 500; // Radio en metros (500 m)
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean petClick,peopleClick,maintenaceClick,disabledClick,operativeClick = false;

    private HashMap<String, Marker> currentMarkers = new HashMap<>();
    private HashMap<String,Marker> searchMarkers = new HashMap<>();


    private ListView listview;
    private ArrayAdapter<String> adapter;
    private List<String> lista;
    private Set<String> barriosUnicos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        BarraDeBusqueda();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Button pet = findViewById(R.id.pet);
        Button people = findViewById(R.id.people);
        Button available = findViewById(R.id.available);
        Button maintenance = findViewById(R.id.maintenance);
        Button disabled = findViewById(R.id.disabled);



        // Button click listeners
        pet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                petClick=!petClick;
                togglePetPeopleFilter("MASCOTAS");

                if(petClick){
                    Toast.makeText(MainActivity.this,"Mascotas",Toast.LENGTH_SHORT).show();
                    pet.setBackground(getDrawable(R.drawable.paw2));
                }else{
                    pet.setBackground(getDrawable(R.drawable.paw1));

                }
            }
        });

        people.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                peopleClick=!peopleClick;
                togglePetPeopleFilter("PERSONAS");

                if(peopleClick){
                Toast.makeText(MainActivity.this,"Personas",Toast.LENGTH_SHORT).show();
                    people.setBackground(getDrawable(R.drawable.people2));
                }else{
                    people.setBackground(getDrawable(R.drawable.people1));
                }
            }
        });

        available.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                operativeClick=!operativeClick;
                toggleStatusFilter("OPERATIVO");

                if(operativeClick){
                    Toast.makeText(MainActivity.this,"Operativo",Toast.LENGTH_SHORT).show();
                    available.setBackground(getDrawable(R.drawable.enabled2));
                }else{
                    available.setBackground(getDrawable(R.drawable.enabled1));
                }
            }
        });
        maintenance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maintenaceClick=!maintenaceClick;
                toggleStatusFilter("CERRADA_TEMPORALMENT");

                if(maintenaceClick){
                    Toast.makeText(MainActivity.this,"En mantenimiento",Toast.LENGTH_SHORT).show();
                    maintenance.setBackground(getDrawable(R.drawable.maintenance2));
                }else{
                    maintenance.setBackground(getDrawable(R.drawable.maintenance1));
                }
            }
        });
        disabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disabledClick = !disabledClick;
                toggleStatusFilter("FUERA_DE_SERVICIO");

                if(disabledClick) {
                    Toast.makeText(MainActivity.this, "Fuera de Servicio", Toast.LENGTH_SHORT).show();
                    disabled.setBackground(getDrawable(R.drawable.disabled2));
                }else{
                    disabled.setBackground(getDrawable(R.drawable.disabled1));
                }
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //todo
    // Apply the current filters
    private void applyFilters() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call API after getting location
            ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
            apiService.getFuentes().enqueue(new Callback<List<Fuentes>>() {
                @Override
                public void onResponse(Call<List<Fuentes>> call, Response<List<Fuentes>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Filter sources using the selected filters
                        List<Fuentes> filteredFuentes = filterFuentes(response.body());
                        actualizarMarcadores(filteredFuentes);
                    }
                }

                @Override
                public void onFailure(Call<List<Fuentes>> call, Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error al obtener fuentes", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    //todo
    // Modify the filterFuentes method to handle multiple filters (exclusive)
    private List<Fuentes> filterFuentes(List<Fuentes> fuentes) {
        List<Fuentes> fuentesCercanas = new ArrayList<>();

        for (Fuentes fuente : fuentes) {
            boolean matchesPetPeopleFilter = selectedPetPeopleFilters.isEmpty() || selectedPetPeopleFilters.stream().anyMatch(filter ->
                    fuente.getUso().contains(filter)
            );

            boolean matchesStatusFilter = selectedStatusFilters.isEmpty() || selectedStatusFilters.stream().anyMatch(filter ->
                    fuente.getEstado().equalsIgnoreCase(filter)
            );

            // Get the location of each source
            Location fuenteLocation = new Location("");
            fuenteLocation.setLatitude(fuente.getLatitud());
            fuenteLocation.setLongitude(fuente.getLongitud());

            // Apply the filters: distance and matching filters (exclusive)
            if (matchesPetPeopleFilter && matchesStatusFilter) {
                fuentesCercanas.add(fuente);
            }
        }
        return fuentesCercanas;
    }



    private void BarraDeBusqueda() {
        ListView listView=findViewById(R.id.lista);
        lista=new ArrayList<>();
        barriosUnicos=new HashSet<>();

        SearchView searchView = findViewById(R.id.busqueda);
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setHint("Busqueda por Barrio");
        searchEditText.setTextColor(Color.BLACK); // Color del texto
        searchEditText.setHintTextColor(Color.GRAY); // Color del hint

        searchView.setOnQueryTextFocusChangeListener((v,hasfocus)->{
            if(!hasfocus){
                searchView.setQuery("",false);
                listView.setVisibility(View.GONE);
            }
        });
        searchView.setOnClickListener(v -> {
            barriosUnicos.clear(); // Limpiar el Set de barrios únicos
            lista.clear();

            searchView.setIconified(false);
            listView.setVisibility(View.VISIBLE);

            ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
            apiService.getFuentes().enqueue(new Callback<List<Fuentes>>() {
                @Override
                public void onResponse(Call<List<Fuentes>> call, Response<List<Fuentes>> response) {
                    List<Fuentes>fuente=response.body();
                    for(Fuentes fuentes : fuente){
                        barriosUnicos.add(fuentes.getBarrio());
                    }
                    for(String barrios:barriosUnicos){
                        lista.add(barrios);
                    }
                    adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, lista) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView textView = (TextView) view.findViewById(android.R.id.text1);
                            textView.setTextColor(Color.BLACK); // Cambia el color del texto
                            return view;
                        }
                    };
                    listView.setAdapter(adapter);
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            Map.clear();

                            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder(); // Para ajustar la cámara

                            boolean found = false; // Para saber si se encontraron fuentes en el barrio

                            // Agregar todos los marcadores del barrio seleccionado
                            for (Fuentes fuentes : fuente) {
                                if (query != null && query.equalsIgnoreCase(fuentes.getBarrio())) {
                                    LatLng latLng = new LatLng(fuentes.getLatitud(), fuentes.getLongitud());

                                    String estado = fuentes.getEstado();
                                    searchMarkers.put(fuentes.getNomVia(), addMarker(fuentes,estado));
                                    boundsBuilder.include(latLng);
                                    found = true;
                                }
                            }

                            if (found) {
                                // Ajustar la cámara para que muestre todos los marcadores
                                Map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                            } else {
                                Toast.makeText(MainActivity.this, "No se encontraron fuentes en esta zona", Toast.LENGTH_SHORT).show();
                            }

                            hideKeyboard(searchView);
                            // Ocultar el ListView después de la selección
                            listView.setVisibility(View.GONE);
                            return found;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            adapter.getFilter().filter(newText);

                            if (newText.isEmpty()) {
                                listView.setVisibility(View.GONE);
                                Map.clear(); // Borrar los marcadores de la búsqueda
                                currentMarkers.clear();
                                configureLocationUpdates();
                                getDeviceLocation();
                            } else {
                                listView.setVisibility(View.VISIBLE);
                            }
                            return false;
                        }
                    });

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedItem = adapter.getItem(position); // Barrio seleccionado
                        searchView.setQuery(selectedItem, false); // Mostrar en SearchView

                        // Limpiar los marcadores actuales
                        Map.clear();

                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder(); // Para ajustar la cámara

                        boolean found = false; // Para saber si se encontraron fuentes en el barrio

                        // Agregar todos los marcadores del barrio seleccionado
                        for (Fuentes fuentes : fuente) {
                            if (selectedItem.equalsIgnoreCase(fuentes.getBarrio())) {
                                LatLng latLng = new LatLng(fuentes.getLatitud(), fuentes.getLongitud());
                                String estado = fuentes.getEstado();
                                searchMarkers.put(fuentes.getNomVia(), addMarker(fuentes,estado));
                                boundsBuilder.include(latLng);
                                found = true;
                            }
                        }

                        if (found) {
                            // Ajustar la cámara para que muestre todos los marcadores
                            Map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                        } else {
                            Toast.makeText(MainActivity.this, "No se encontraron fuentes en esta zona", Toast.LENGTH_SHORT).show();
                        }

                        hideKeyboard(searchView);
                        // Ocultar el ListView después de la selección
                        listView.setVisibility(View.GONE);
                    });
                    /*
                    // Agregar solo los marcadores del barrio buscado
                            for (Fuentes fuentes : fuente) {
                                if (query != null && query.equalsIgnoreCase(fuentes.getBarrio())) {
                                    LatLng latLng = new LatLng(fuentes.getLatitud(), fuentes.getLongitud());
                                    Map.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title(fuentes.getNomVia())
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                                    Map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                                }
                            }
                            listView.setVisibility(View.GONE);
                            // Ocultar el teclado
                            hideKeyboard(searchView);
                            return true;
                    */



                }
                @Override
                public void onFailure(Call<List<Fuentes>> call, Throwable t) {
                    t.printStackTrace(); // Manejo de errores
                }
            });
        });
    }


    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Map = googleMap;
        LatLngBounds mapBounds = new LatLngBounds(
                new LatLng(40.3121,-3.8466),
                new LatLng(40.6437,-3.5702)
        );
        Map.setLatLngBoundsForCameraTarget(mapBounds);
        Map.setMinZoomPreference(10);
        Map.setMaxZoomPreference(17);
        // Solicitar permisos
        requestLocationPermission();

        configureLocationUpdates();
    }


    private void configureLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000) // Cada 10 segundos
                .setFastestInterval(5000) // Intervalo más rápido posible
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location userLocation = locationResult.getLastLocation();
                if (userLocation != null) {
                    updateMapWithUserLocation(userLocation);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    private void updateMapWithUserLocation(Location userLocation) {
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        apiService.getFuentes().enqueue(new Callback<List<Fuentes>>() {
            @Override
            public void onResponse(Call<List<Fuentes>> call, Response<List<Fuentes>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Fuentes> fuentesCercanas = filtrarFuentesCercanas(response.body(), userLocation);
                    actualizarMarcadores(fuentesCercanas);
                }
            }

            @Override
            public void onFailure(Call<List<Fuentes>> call, Throwable t) {
                t.printStackTrace(); // Manejo de errores
            }
        });
    }


    private void actualizarMarcadores(List<Fuentes> fuentesCercanas) {
        HashMap<String, Marker> updatedMarkers = new HashMap<>();

        for (Fuentes fuente : fuentesCercanas) {
            String key = fuente.getLatitud() + "," + fuente.getLongitud();

            if (currentMarkers.containsKey(key)) {
                // Mantener marcador existente
                updatedMarkers.put(key, currentMarkers.get(key));
            } else {
                // Crear nuevo marcador

                String estado = fuente.getEstado();
                updatedMarkers.put(key, addMarker(fuente,estado));
            }
        }

        // Eliminar marcadores que ya no están cerca
        for (String key : currentMarkers.keySet()) {
            if (!updatedMarkers.containsKey(key)) {
                currentMarkers.get(key).remove();
            }
        }

        currentMarkers = updatedMarkers; // Actualizar lista de marcadores
    }

    private Marker addMarker(Fuentes fuente,String estado){
        LatLng latLng = new LatLng(fuente.getLatitud(), fuente.getLongitud());
        Marker marker = null;
        switch(estado){
            case "OPERATIVO":
                marker = Map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(fuente.getNomVia())
                        .icon(getCustomMarker(R.drawable.markeroperative)));
                break;
            case "CERRADA_TEMPORALMENT":
                marker = Map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(fuente.getNomVia())
                        .icon(getCustomMarker(R.drawable.markermaintenance)));
                break;
            case "FUERA_DE_SERVICIO":
                marker = Map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(fuente.getNomVia())
                        .icon(getCustomMarker(R.drawable.markerclosed)));
                break;
        }
        return marker;
    }

    // Método para filtrar fuentes cercanas
    private List<Fuentes> filtrarFuentesCercanas(List<Fuentes> fuentes, Location userLocation) {
        List<Fuentes> fuentesCercanas = new ArrayList<>();
        for (Fuentes fuente : fuentes) {
            Location fuenteLocation = new Location("");
            fuenteLocation.setLatitude(fuente.getLatitud());
            fuenteLocation.setLongitude(fuente.getLongitud());

            // Calcula la distancia entre el usuario y la fuente
            float distancia = userLocation.distanceTo(fuenteLocation);
            if (distancia <= RADIUS_METERS) { // Radio de 2 km

                fuentesCercanas.add(fuente);

            }
        }
        return fuentesCercanas;
    }


    private List<String> selectedPetPeopleFilters = new ArrayList<>();  // Store selected pet/people filters
    private List<String> selectedStatusFilters = new ArrayList<>();  // Store selected available/maintenance/disabled filters

    // Toggle pet/people filter (add or remove from the list)
    private void togglePetPeopleFilter(String filter) {
        if (selectedPetPeopleFilters.contains(filter)) {
            selectedPetPeopleFilters.remove(filter);  // Remove if already added
        } else {
            selectedPetPeopleFilters.add(filter);  // Add if not already present
        }
        applyFilters();  // Apply the updated list of filters
    }

    // Toggle available/maintenance/disabled filter (add or remove from the list)
    private void toggleStatusFilter(String filter) {
        if (selectedStatusFilters.contains(filter)) {
            selectedStatusFilters.remove(filter);  // Remove if already added
        } else {
            selectedStatusFilters.add(filter);  // Add if not already present
        }
        applyFilters();  // Apply the updated list of filters
    }



    private void getDeviceLocation(){
        try{
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this,task ->{
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Obtén la última ubicación conocida
                        Location location = task.getResult();
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Mueve la cámara al usuario
                        Map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }catch(SecurityException e){
            e.printStackTrace();
        }
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Map.setMyLocationEnabled(true);
            getDeviceLocation();
        }

    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            }
        }
    }

    private BitmapDescriptor getCustomMarker(int drawableRes) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableRes);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


}