package com.geektech.weatherappretrofitopenweather.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.geektech.weatherappretrofitopenweather.R;
import com.geektech.weatherappretrofitopenweather.databinding.ActivityMainBinding;
import com.geektech.weatherappretrofitopenweather.models.GetMainWeather;
import com.geektech.weatherappretrofitopenweather.retrofit.API;
import com.geektech.weatherappretrofitopenweather.retrofit.APIClient;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int REQUEST_CODE_GPS_PERMISSION = 1;
    private ActivityMainBinding mainBinding;
    private Geocoder geocoder;
    private LocationManager locationManager;
    
    private ProgressDialog dialog;

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Getting current location...");
        dialog.setCancelable(false);
        dialog.setIcon(R.drawable.gps);
        dialog.setButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                endGPS();
            }
        });

        mainBinding.imageSearchCity.setOnClickListener(v -> {
            if (!mainBinding.editSearch.getText().toString().equals("")) {
                getWeather(mainBinding.editSearch.getText().toString().trim());
                mainBinding.currentLocation.setText(mainBinding.editSearch.getText().toString());
                hideKeyboard();
            } else {
                mainBinding.editSearch.setError("Search for places on Search window");
            }
        });

        mainBinding.currentLocation.setOnClickListener(v -> {
            if (hasPermissions()) {
                // our app has permissions.
                getLocation();
                geocoder = new Geocoder(this, Locale.getDefault());
            } else {
                //our app doesn't have permissions, So i m requesting permissions.
                requestPermissionWithRationale();
            }
        });

        mainBinding.imageGetLocation.setOnClickListener(v -> {
            if (hasPermissions()) {
                // our app has permissions.
                getLocation();
                geocoder = new Geocoder(this, Locale.getDefault());
            } else {
                //our app doesn't have permissions, So i m requesting permissions.
                requestPermissionWithRationale();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onStart() {
        super.onStart();
        if (hasPermissions()) {
            // our app has permissions.
            getLocation();
            geocoder = new Geocoder(this, Locale.getDefault());
        } else {
            //our app doesn't have permissions, So i m requesting permissions.
            requestPermissionWithRationale();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        endGPS();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void getLocation() {
        try {
            dialog.show();
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
            if (!locationManager.isLocationEnabled()) {
                Toast.makeText(MainActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Something went wrong " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    public void endGPS() {
        try {
            locationManager.removeUpdates(this);
            locationManager = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            mainBinding.editSearch.setText(address);
            mainBinding.currentLocation.setText(address);
            dialog.dismiss();

            getWeather(address);

        } catch (IOException e) {
            Toast.makeText(this, "Something went wrong " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String convertDate(long time) {
        String t;
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time * 1000);
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        int seconds = cal.get(Calendar.SECOND);
        t = hours + ":" + minutes + ":" + seconds;
        return t;
    }

    private void getWeather(String city) {
        API api = APIClient.getClient().create(API.class);
        Call<GetMainWeather> call = api.getWeather(city);

        // 0 Celsius = 273.15 Kelvin
        final float K = (float) 273.15;

        call.enqueue(new Callback<GetMainWeather>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<GetMainWeather> call, @NonNull Response<GetMainWeather> response) {
                if (response.body() != null) {
                    int temp = (int) (Float.parseFloat(response.body().getMain().getTemp()) - K);
                    int feelsLike = (int) (Float.parseFloat(response.body().getMain().getFeels_like()) - K);
                    int minTemp = (int) (Float.parseFloat(response.body().getMain().getTemp_min()) - K);
                    int maxTemp = (int) (Float.parseFloat(response.body().getMain().getTemp_max()) - K);

                    //converting pressure info to show in millibar
                    double pressure = Double.parseDouble(response.body().getMain().getPressure()) / 1000;

                    //converting Visibility info to show (from meter) in kilometer
                    int visibility = Integer.parseInt(response.body().getVisibility()) / 1000;

                    mainBinding.textTemperature.setText(temp + "°C");
                    mainBinding.textHumidity.setText("Humidity: " + response.body().getMain().getHumidity() + "%");
                    mainBinding.textFeelsLike.setText("Feels like: " + feelsLike + "°C");
                    mainBinding.textVisibility.setText("Visibility: " + visibility + " km");
                    mainBinding.textPressure.setText("Pressure: " + pressure + "mBar");
                    mainBinding.textTempMax.setText("Max temp: " + maxTemp + " °C");
                    mainBinding.textTempMin.setText("Min temp: " + minTemp + " °C");
                    mainBinding.textWindSpeed.setText("Wind speed: " + response.body().getWind().getSpeed() + " km/h");
                    mainBinding.textSunrise.setText("Sunrise: " + convertDate(Long.parseLong(response.body().getSys().getSunrise())));
                    mainBinding.textSunset.setText("Sunset: " + convertDate(Long.parseLong(response.body().getSys().getSunset())));
                    mainBinding.textDate.setText("Data for: " + convertDate(Long.parseLong(response.body().getDt())));

                    //Getting Icons
                    getWeatherIcons(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GetMainWeather> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Something went wrong " + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // хардкодил, пока не нашел других вариантов:)
    private void getWeatherIcons(@NonNull Response<GetMainWeather> response) {
        assert response.body() != null;
        if (response.body().getWeather().get(0).getIcon().equals("01d")) {
            mainBinding.weatherIcons.setAnimation(R.raw.sunny);
            mainBinding.weatherIcons.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);

        } else if (response.body().getWeather().get(0).getIcon().equals("01n")) {
            mainBinding.weatherIcons9.setAnimation(R.raw.clear_night);
            mainBinding.weatherIcons9.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("02d")) {
            mainBinding.weatherIcons1.setAnimation(R.raw.few_cloud);
            mainBinding.weatherIcons1.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);

        } else if (response.body().getWeather().get(0).getIcon().equals("02n")) {
            mainBinding.weatherIcons10.setAnimation(R.raw.cloudynight);
            mainBinding.weatherIcons10.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("03d") || response.body().getWeather().get(0).getIcon().equals("03n")) {
            mainBinding.weatherIcons2.setAnimation(R.raw.scattered_clouds_origin);
            mainBinding.weatherIcons2.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("04d") || response.body().getWeather().get(0).getIcon().equals("04n")) {
            mainBinding.weatherIcons3.setAnimation(R.raw.cloudy_orirgin);
            mainBinding.weatherIcons3.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("09d") || response.body().getWeather().get(0).getIcon().equals("09n")) {
            mainBinding.weatherIcons4.setAnimation(R.raw.shower_rain);
            mainBinding.weatherIcons4.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("10d")) {
            mainBinding.weatherIcons5.setAnimation(R.raw.rain);
            mainBinding.weatherIcons5.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);

        } else if (response.body().getWeather().get(0).getIcon().equals("10n")) {
            mainBinding.weatherIcons11.setAnimation(R.raw.rainynight);
            mainBinding.weatherIcons11.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("11d") || response.body().getWeather().get(0).getIcon().equals("11n")) {
            mainBinding.weatherIcons6.setAnimation(R.raw.thunderstorm);
            mainBinding.weatherIcons6.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("13d")) {
            mainBinding.weatherIcons7.setAnimation(R.raw.snow);
            mainBinding.weatherIcons7.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);

        } else if (response.body().getWeather().get(0).getIcon().equals("13n")) {
            mainBinding.weatherIcons12.setAnimation(R.raw.rainynight);
            mainBinding.weatherIcons12.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons8.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
        }

        if (response.body().getWeather().get(0).getIcon().equals("50d") || response.body().getWeather().get(0).getIcon().equals("50n")) {
            mainBinding.weatherIcons8.setAnimation(R.raw.mist);
            mainBinding.weatherIcons8.setVisibility(View.VISIBLE);
            mainBinding.weatherIcons1.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons2.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons3.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons4.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons5.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons6.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons7.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons9.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons10.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons11.setVisibility(View.INVISIBLE);
            mainBinding.weatherIcons12.setVisibility(View.INVISIBLE);
        }
        mainBinding.weatherMain.setText(response.body().getWeather().get(0).getMain());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private boolean hasPermissions() {
        int res;
        //string array of permissions
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        for (String perms : permissions) {
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, REQUEST_CODE_GPS_PERMISSION);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;

        if (requestCode == REQUEST_CODE_GPS_PERMISSION) {
            for (int res : grantResults) {
                // if user granted all permissions.
                allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
            }
        } else {
            // if user not granted permissions.
            allowed = false;
        }

        if (allowed) {
            //user granted all permissions we can perform our task.
            getLocation();
            geocoder = new Geocoder(this, Locale.getDefault());
        } else {
            // giving warning to user that they haven't granted permissions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "GPS(Location) Permissions denied.", Toast.LENGTH_SHORT).show();
                } else {
                    showGPSPermissionSnackBar();
                }
            }
        }
    }

    public void showGPSPermissionSnackBar() {
        Snackbar.make(MainActivity.this.findViewById(R.id.activity_view), "GPS(Location) permission isn't granted", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS", v -> {
                    openApplicationSettings();
                    Toast.makeText(this, "Open Permissions and grant the GPS(Location) permission", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public void openApplicationSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, REQUEST_CODE_GPS_PERMISSION);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GPS_PERMISSION) {
            getLocation();
            geocoder = new Geocoder(this, Locale.getDefault());
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            final String message = "GPS(Location) permission is needed to show files count";
            Snackbar.make(MainActivity.this.findViewById(R.id.activity_view), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", v -> requestPerms())
                    .show();
        } else {
            requestPerms();
        }
    }
}