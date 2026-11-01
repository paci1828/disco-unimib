package com.simone.discounimib.utils;

import android.content.Context;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Le preferenze sono una cache del profilo, non una sessione di autenticazione. */
public final class GestoreSessioneUtente {
    private final Context context;
    private final SharedPreferencesManager preferenze;

    public GestoreSessioneUtente(Context context) {
        this.context = context.getApplicationContext();
        preferenze = new SharedPreferencesManager(this.context);
    }

    public boolean valida() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String ruolo = preferenze.getUserRole();
        return user != null && user.getUid().equals(preferenze.getUserUid())
                && ("studente".equals(ruolo) || "professore".equals(ruolo));
    }

    public boolean ricordaAccesso() { return valida() && preferenze.isRememberMe(); }

    public void termina() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        com.simone.discounimib.sync.SincronizzazioneScheduler.interrompi(context,
                user == null ? preferenze.getUserUid() : user.getUid());
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        preferenze.clear();
    }
}
