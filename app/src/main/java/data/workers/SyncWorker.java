package data.workers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import data.repository.SyncRepository;

public class SyncWorker extends Worker {

    private final SyncRepository syncRepository;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.syncRepository = new SyncRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!isOnline()) {
            Log.d("SyncWorker", "Device offline — skipping sync.");
            return Result.retry(); // retry later automatically
        }

        Log.d("SyncWorker", "Device online — performing background sync...");
        syncRepository.syncProjectsToFirebase();
        syncRepository.syncProjectsFromFirebase();

        return Result.success();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}
