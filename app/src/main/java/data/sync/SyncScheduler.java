package data.sync;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;

import data.repository.SyncRepository;

/**
 * Periodically syncs local SQLite with Firebase only when online.
 * Shows Material Snackbars when going offline or back online.
 */
public class SyncScheduler {

    private final Handler handler = new Handler();
    private final SyncRepository syncRepository;
    private final Activity activity;

    private static final long SYNC_INTERVAL = 5000L; // 5 seconds
    private boolean wasOnline = true;

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            boolean online = isOnline();

            // Notify network status change
            if (online && !wasOnline) {
                showSnackbar("🟢 Back online. Syncing now...", true);
                Log.d("SyncScheduler", "Device reconnected → Syncing started");
            } else if (!online && wasOnline) {
                showSnackbar("🔴 Offline mode. Changes will sync later.", false);
                Log.d("SyncScheduler", "Device went offline → Sync paused");
            }

            wasOnline = online;

            // Perform sync if online
            if (online) {
                syncRepository.syncProjectsToFirebase();
                syncRepository.syncProjectsFromFirebase();
            }

            // Schedule next run
            handler.postDelayed(this, SYNC_INTERVAL);
        }
    };

    public SyncScheduler(Activity activity) {
        this.activity = activity;
        this.syncRepository = new SyncRepository(activity.getApplicationContext());
    }

    /**
     * Start periodic sync
     */
    public void start() {
        Log.d("SyncScheduler", "Background sync started");
        handler.post(syncRunnable);
    }

    /**
     * Stop periodic sync
     */
    public void stop() {
        Log.d("SyncScheduler", "Background sync stopped");
        handler.removeCallbacks(syncRunnable);
    }

    /**
     * Check for internet connection (modern, not deprecated)
     */
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Display status message with Material Snackbar
     */
    private void showSnackbar(String message, boolean success) {
        activity.runOnUiThread(() -> {
            Snackbar snackbar = Snackbar.make(
                    activity.findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_SHORT
            );
            snackbar.setBackgroundTint(success ? 0xFF2E7D32 : 0xFFC62828); // green/red
            snackbar.setTextColor(0xFFFFFFFF);
            snackbar.show();
        });
    }
}
