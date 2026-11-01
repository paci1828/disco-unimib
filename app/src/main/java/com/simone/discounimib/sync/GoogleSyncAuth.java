package com.simone.discounimib.sync;

import android.accounts.Account;
import android.content.Context;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;
import com.google.firebase.auth.FirebaseUser;
import java.util.Arrays;

public final class GoogleSyncAuth {
    private GoogleSyncAuth() {}
    public static AuthorizationRequest richiesta(FirebaseUser user) {
        if (user == null || user.getEmail() == null) throw new IllegalStateException("Accedi con l'account docente");
        return AuthorizationRequest.builder().setAccount(new Account(user.getEmail(), "com.google"))
                .setRequestedScopes(Arrays.asList(new Scope(CalendarScopes.CALENDAR_EVENTS),
                        new Scope(GmailScopes.GMAIL_SEND))).build();
    }
    public static boolean abilitata(Context context, String uid) {
        return context.getSharedPreferences("google_sync", Context.MODE_PRIVATE).getBoolean(uid, false);
    }
    public static void abilita(Context context, String uid) {
        context.getSharedPreferences("google_sync", Context.MODE_PRIVATE).edit().putBoolean(uid, true).apply();
    }
}
