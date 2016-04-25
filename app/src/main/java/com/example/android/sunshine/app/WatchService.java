package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.widget.TodayWidgetIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.zzc;
import com.google.android.gms.common.api.zzl;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * Created by teisentraeger on 4/24/2016.
 */
public class WatchService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String ACTION_UPDATE_WATCH = "ACTION_UPDATE_WATCH";
    public static final String LOG_TAG = "SunWatchFace";
    private GoogleApiClient mGoogleApiClient;

    private static final String KEY_PATH = "/weather";
    private static final String KEY_WEATHER_ID = "KEY_WEATHER_ID";
    private static final String KEY_DESC = "KEY_DESC";
    private static final String KEY_MAX = "KEY_MAX";
    private static final String KEY_MIN = "KEY_MIN";

    public WatchService() {
        super(LOG_TAG);
        Log.d(LOG_TAG, "constructor called");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent");
        if(intent != null && intent.getAction() !=null && intent.getAction().equals(ACTION_UPDATE_WATCH)) {
            mGoogleApiClient = new GoogleApiClient.Builder(WatchService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");

        // Get today's data from the ContentProvider
        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = getContentResolver().query(weatherForLocationUri, TodayWidgetIntentService.FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // Extract the weather data from the Cursor
        int weatherId = data.getInt(TodayWidgetIntentService.INDEX_WEATHER_ID);
        int weatherArtResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
        String description = data.getString(TodayWidgetIntentService.INDEX_SHORT_DESC);
        double maxTemp = data.getDouble(TodayWidgetIntentService.INDEX_MAX_TEMP);
        double minTemp = data.getDouble(TodayWidgetIntentService.INDEX_MIN_TEMP);
        String formattedMaxTemperature = Utility.formatTemperature(this, maxTemp);
        String formattedMinTemperature = Utility.formatTemperature(this, minTemp);
        data.close();

        Log.d(LOG_TAG, "weatherId="+weatherId + " weatherArtResourceId="+weatherArtResourceId +  " description="+description +  " formattedMaxTemperature="+formattedMaxTemperature +  " formattedMinTemperature="+formattedMinTemperature );

        // Send the data to the watch with the DataApi
        final PutDataMapRequest mapRequest = PutDataMapRequest.create(KEY_PATH);
        mapRequest.getDataMap().putInt(KEY_WEATHER_ID, weatherId);
        mapRequest.getDataMap().putString(KEY_MAX, formattedMaxTemperature);
        mapRequest.getDataMap().putString(KEY_MIN, formattedMinTemperature);
        mapRequest.getDataMap().putString(KEY_DESC, description);

        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, mapRequest.asPutDataRequest());

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>()
        {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult)
            {
                if (dataItemResult.getStatus().isSuccess())
                {
                    Log.v(LOG_TAG,"DataMap sent.");
                } else
                {
                    // Log an error
                    Log.v(LOG_TAG,"ERROR: failed to send DataMap");
                }
            }
        });

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended");
    }
}
