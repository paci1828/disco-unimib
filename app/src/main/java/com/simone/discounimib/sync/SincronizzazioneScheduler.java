package com.simone.discounimib.sync;

import android.content.Context;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simone.discounimib.utils.SharedPreferencesManager;
import java.util.concurrent.TimeUnit;

public final class SincronizzazioneScheduler {
    private SincronizzazioneScheduler() {}
    private static String tag(String uid) { return "google_sync_" + uid; }
    private static Constraints rete() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }
    public static void richiedi(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !GoogleSyncAuth.abilitata(context, user.getUid())
                || !"professore".equals(new SharedPreferencesManager(context).getUserRole())) return;
        String uid = user.getUid();
        WorkManager manager = WorkManager.getInstance(context);
        Data dati = new Data.Builder().putString("uid", uid).build();
        manager.enqueueUniquePeriodicWork(tag(uid) + "_recupero", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(SincronizzaAppuntamentiWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(rete()).setInputData(dati).addTag(tag(uid))
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
        manager.enqueueUniqueWork(tag(uid), ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(SincronizzaAppuntamentiWorker.class)
                        .setConstraints(rete()).setInputData(dati).addTag(tag(uid))
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
    }
    public static void interrompi(Context context, String uid) {
        if (uid != null) WorkManager.getInstance(context).cancelAllWorkByTag(tag(uid));
    }
}
