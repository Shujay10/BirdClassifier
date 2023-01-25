package com.example.birdclassifier;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Api {

    @GET("getbirdname/{name}")
    Call<Data> getData(@Path("name")String name);

}
