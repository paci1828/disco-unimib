package com.simone.discounimib;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.simone.discounimib.activities.HomeActivity;
import com.simone.discounimib.activities.LoginActivity;
import com.simone.discounimib.utils.GestoreSessioneUtente;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GestoreSessioneUtente sessione = new GestoreSessioneUtente(this);
        boolean ricorda = sessione.ricordaAccesso();
        if (!ricorda) sessione.termina();
        startActivity(new Intent(this, ricorda ? HomeActivity.class : LoginActivity.class));
        finish();
    }
}
