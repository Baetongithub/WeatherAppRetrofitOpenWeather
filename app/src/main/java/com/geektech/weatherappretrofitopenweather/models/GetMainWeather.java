package com.geektech.weatherappretrofitopenweather.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetMainWeather {

    @SerializedName("main")
    private Main main;

    @SerializedName("weather")
    private List<WeatherIcons> weather;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("wind")
    private Wind wind;

    @SerializedName("dt")
    private String dt;

    @SerializedName("sys")
    private Sys sys;

    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public String getDt() {
        return dt;
    }

    public void setDt(String dt) {
        this.dt = dt;
    }

    public Sys getSys() {
        return sys;
    }

    public void setSys(Sys sys) {
        this.sys = sys;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<WeatherIcons> getWeather() {
        return weather;
    }

    public void setWeather(List<WeatherIcons> weather) {
        this.weather = weather;
    }

    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }
}
