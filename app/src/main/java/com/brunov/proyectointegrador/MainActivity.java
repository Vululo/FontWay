package com.brunov.proyectointegrador;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import androidx.fragment.app.Fragment;

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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap Map;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int RADIUS_METERS = 500; // Radio en metros (500 m)
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String, Marker> currentMarkers = new HashMap<>();
    private boolean petClick,peopleClick,maintenaceClick,disabledClick,operativeClick = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
                if(petClick){
                    togglePetPeopleFilter("MASCOTAS");
                    Toast.makeText(MainActivity.this,"Mascotas",Toast.LENGTH_SHORT).show();
                    pet.setBackground(getDrawable(R.drawable.paw2));
                }else{
                    pet.setBackground(getDrawable(R.drawable.paw1));

                }
                petClick=!petClick;
            }
        });

        people.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(peopleClick){
                togglePetPeopleFilter("PERSONAS");
                Toast.makeText(MainActivity.this,"Personas",Toast.LENGTH_SHORT).show();
                    people.setBackground(getDrawable(R.drawable.people2));
                }else{
                    people.setBackground(getDrawable(R.drawable.people1));
                }
                peopleClick=!peopleClick;
            }
        });

        available.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(operativeClick){
                    toggleStatusFilter("OPERATIVO");
                    Toast.makeText(MainActivity.this,"Operativo",Toast.LENGTH_SHORT).show();
                    available.setBackground(getDrawable(R.drawable.enabled2));
                }else{
                    available.setBackground(getDrawable(R.drawable.enabled1));
                }
                operativeClick=!operativeClick;
            }
        });
        maintenance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(maintenaceClick){
                    toggleStatusFilter("CERRADA_TEMPORALMENT");
                    Toast.makeText(MainActivity.this,"En mantenimiento",Toast.LENGTH_SHORT).show();
                    maintenance.setBackground(getDrawable(R.drawable.maintenance2));
                }else{
                    maintenance.setBackground(getDrawable(R.drawable.maintenance1));
                }
                maintenaceClick=!maintenaceClick;
            }
        });
        disabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(disabledClick) {
                    toggleStatusFilter("FUERA_DE_SERVICIO");
                    Toast.makeText(MainActivity.this, "Fuera de Servicio", Toast.LENGTH_SHORT).show();
                    disabled.setBackground(getDrawable(R.drawable.disabled2));
                }else{
                    disabled.setBackground(getDrawable(R.drawable.disabled1));
                }
                disabledClick = !disabledClick;
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                // Crear nuevo marcador personalizado segun el estado
                LatLng latLng = new LatLng(fuente.getLatitud(), fuente.getLongitud());
                String estado = fuente.getEstado();
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

                updatedMarkers.put(key, marker);
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
                        List<Fuentes> filteredFuentes = filterFuentes(response.body(), location);
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

    // Modify the filterFuentes method to handle multiple filters (exclusive)
    private List<Fuentes> filterFuentes(List<Fuentes> fuentes, Location userLocation) {
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

            // Calculate the distance between the user and the source
            float distance = userLocation.distanceTo(fuenteLocation);

            // Apply the filters: distance and matching filters (exclusive)
            if (distance <= RADIUS_METERS && matchesPetPeopleFilter && matchesStatusFilter) {
                fuentesCercanas.add(fuente);
            }
        }

        return fuentesCercanas;
    }









    //Filtro de las Fuentes
    private List<Fuentes> filterFuentes(List<Fuentes> fuentes, String filter, Location userLocation) {
        List<Fuentes> fuentesCercanas = new ArrayList<>();

        for (Fuentes fuente : fuentes) {
            // Entre todas las fuentes fitra segun lo que se pide
            boolean matchesEstado = fuente.getEstado().equalsIgnoreCase(filter);
            boolean matchesUso = fuente.getUso().contains(filter);

            // Busca la localizacion de cada fuente
            Location fuenteLocation = new Location("");
            fuenteLocation.setLatitude(fuente.getLatitud());
            fuenteLocation.setLongitude(fuente.getLongitud());

            // Calcula la distancia entre el usuario y la fuente
            float distancia = userLocation.distanceTo(fuenteLocation);

            // Filtro de las fuentes
            // Por ESTADO o por USO
            if (distancia <= RADIUS_METERS && (matchesEstado || matchesUso) || (matchesUso && matchesEstado)) { // Radio de 2 km
                fuentesCercanas.add(fuente);
            }
        }

        return fuentesCercanas;
    }

    private void applyFilter(String filter) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                return;
            }

            // Llamar a la API después de obtener la ubicación
            ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
            apiService.getFuentes().enqueue(new Callback<List<Fuentes>>() {
                @Override
                public void onResponse(Call<List<Fuentes>> call, Response<List<Fuentes>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Filtrar fuentes usando la ubicación obtenida
                        List<Fuentes> filteredFuentes = filterFuentes(response.body(), filter, location);
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
                        Map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
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