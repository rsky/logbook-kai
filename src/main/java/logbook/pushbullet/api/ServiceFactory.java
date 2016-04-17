package logbook.pushbullet.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {
    public static final String BASE_URL = "https://api.pushbullet.com/v2/";

    public static PushbulletService getService(String accessToken) {
        return getClient(accessToken).create(PushbulletService.class);
    }

    public static Retrofit getClient(String accessToken) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(chain.request()
                        .newBuilder()
                        .addHeader("Access-Token", accessToken)
                        .build()))
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }
}
