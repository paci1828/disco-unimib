package com.simone.discounimib.utils;

import android.content.Context;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;

public class GoogleAuthHelper {
    public static GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR), new Scope(GmailScopes.GMAIL_SEND))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    public static GoogleSignInAccount getSignedInAccount(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context);
    }
}