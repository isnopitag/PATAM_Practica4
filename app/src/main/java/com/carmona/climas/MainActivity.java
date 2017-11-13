package com.carmona.climas;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements LocationListener, PostRVAdapter.PostRVAdapterListener, Callback<Climas>{

    boolean bandGPS = false;
    boolean bandRED = false;

    private static final String TAG = "MainActivity";

    private double latActual, lonActual;
    private double latMarca, lonMarca;
    private LocationManager locationManager;

    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/";
    private String URL="";
    private ArrayList<com.carmona.climas.List> climas;
    private String city;
    private PostRVAdapter mPostRVAdapter;

    private static final String appid="e838902b2803dd3fe762d8600ddb3d22";
    private static final String units="metric";
    private static final String lang="es";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        climas = new ArrayList<com.carmona.climas.List>();
        city = "";
        if(!runtime_permissions()) {

            getGeoLocation();
            Toast.makeText(this, "Ubicacion: " + latActual + " " + lonActual, Toast.LENGTH_SHORT).show();
            URL="?lat="+latActual+"&lon="+lonActual;
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            mPostRVAdapter = new PostRVAdapter(this, climas);
            recyclerView.setAdapter(mPostRVAdapter);
            //loadFakePosts();
            loadPosts();

        }else{
            Toast.makeText(this, "Habilita los Permisos Prro", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pri,menu);
        return super.onCreateOptionsMenu(menu);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.itmUbicacion:
                    aboutDialog();
                break;
            case R.id.itmOtro:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getGeoLocation()
    {
        Location ubicacion;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        bandGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        bandRED = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        try{
            if(bandGPS)
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,100000,10,this);
                ubicacion = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                latActual = ubicacion.getLatitude();
                lonActual = ubicacion.getLongitude();
                Toast.makeText(this,"Posicion GPS "+latActual+":"+lonActual,Toast.LENGTH_SHORT).show();
            }
            if(bandRED)
            {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,100000,10,this);
                ubicacion = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                latActual = ubicacion.getLatitude();
                lonActual = ubicacion.getLongitude();
                Toast.makeText(this,"Posicion Red "+latActual+":"+lonActual,Toast.LENGTH_SHORT).show();
            }

        }catch (SecurityException se){
            Log.e("GetLocation Error Rep:",se.toString());
        }

    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permisos Otorgados",Toast.LENGTH_SHORT).show();
            }else {
                runtime_permissions();
            }
        }
    }

    private void aboutDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Climita");
        final WebView webView = new WebView(this);
        String about = "<p>Early Access :u </p>" +
                "<p>Practica 4: Climas</p>" +
                "<p>Desarrollada por Carmona</a></p>" +
                "<p>Bandido Studios</a>";
        TypedArray ta = obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary, R.attr.colorAccent});
        String textColor = String.format("#%06X", (0xFFFFFF & ta.getColor(0, Color.BLACK)));
        String accentColor = String.format("#%06X", (0xFFFFFF & ta.getColor(1, Color.BLUE)));
        ta.recycle();
        about = "<style media=\"screen\" type=\"text/css\">" +
                "body {\n" +
                "    color:" + textColor + ";\n" +
                "}\n" +
                "a:link {color:" + accentColor + "}\n" +
                "</style>" +
                about;
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(about, "text/html", "UTF-8");
        //alert.setView(webView, 32, 0, 32, 0);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        alert.show();
    }

    public void loadPosts() {
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        JsonPlaceHolderAPI jsonPlaceHolderAPI = retrofit.create(JsonPlaceHolderAPI.class);
        Call<Climas> call = jsonPlaceHolderAPI.getClimas(latActual,lonActual,appid,units,lang);
        call.enqueue(this);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }



    @Override
    public void onResponse(Call<Climas> call, Response<Climas> response) {
        if(response.isSuccessful()) {
            Log.d(TAG, "onResponse: Server Response: " + response.toString());
            Log.d(TAG, "onResponse: received information: " + response.body().getList());
            Log.d(TAG, "onResponse: received information: " + response.body().getCity().getName());

            List<com.carmona.climas.List> climasList = (List<com.carmona.climas.List>) response.body().getList();

            city = response.body().getCity().getName();

            climas.clear();

            for (com.carmona.climas.List list : climasList) {
                climas.add(list);
            }

            mPostRVAdapter.notifyDataSetChanged();
        } else {
            System.out.println(response.errorBody());
        }
    }

    @Override
    public void onFailure(Call<Climas> call, Throwable t) {
        Log.e(TAG, "onFailure: Something went wrong: " + t.getMessage() );
        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
        t.printStackTrace();
    }


    @Override
    public void OnItemClicked(Climas aPost) {

    }
}
