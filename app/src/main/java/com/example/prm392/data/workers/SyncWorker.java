package com.example.prm392.data.workers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.prm392.data.repository.SyncRepository;

public class SyncWorker extends Worker {

    private final SyncRepository syncRepo;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        syncRepo = new SyncRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!isOnline()) {
            Log.d("SyncWorker", "⚠️ Device offline — skipping sync");
            return Result.retry();
        }

        Log.d("SyncWorker", "🌐 Online — performing background sync...");
        syncRepo.syncAll();
        return Result.success();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null &&
                (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }
}
