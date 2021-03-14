package com.geektech.weatherappretrofitopenweather.retrofit;

import com.geektech.weatherappretrofitopenweather.models.GetMainWeather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface API {

    @GET("weather?&appid=900c3a80721771d783a4550a7563f743")
    Call<GetMainWeather> getWeather(
            @Query("q")
            String name
    );
}
