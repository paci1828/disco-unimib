package com.simone.discounimib.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.simone.discounimib.R;
import com.simone.discounimib.databinding.ActivityHomeBinding;
import com.simone.discounimib.fragments.FaqFragment;
import com.simone.discounimib.fragments.SettingsFragment;
import com.simone.discounimib.fragments.professore.ElencoAppuntamentiFragment;
import com.simone.discounimib.fragments.professore.ElencoAppuntamentiSospesiFragment;
import com.simone.discounimib.fragments.professore.ElencoIndisponibilitaFragment;
import com.simone.discounimib.fragments.professore.HomeProfessoreFragment;
import com.simone.discounimib.fragments.professore.ModificaOrarioFragment;
import com.simone.discounimib.fragments.studente.ElencoAppuntamentiStudenteFragment;
import com.simone.discounimib.fragments.studente.PrenotaAppuntamentoCalendarioFragment;
import com.simone.discounimib.utils.SharedPreferencesManager;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityHomeBinding binding;
    private SharedPreferencesManager prefManager;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefManager = new SharedPreferencesManager(this);
        userRole = prefManager.getUserRole();

        setSupportActionBar(binding.toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        updateMenuByRole();
        setupHeader();

        if (savedInstanceState == null) {
            loadDefaultFragment();
        }
    }

    private void loadDefaultFragment() {
        if ("professore".equalsIgnoreCase(userRole)) {
            replaceFragment(new HomeProfessoreFragment());
        } else {
            replaceFragment(new PrenotaAppuntamentoCalendarioFragment());
        }
        binding.navView.setCheckedItem(R.id.nav_home);
    }

    private void setupHeader() {
        View headerView = binding.navView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.nav_header_title);
        TextView emailTextView = headerView.findViewById(R.id.nav_header_subtitle);
        
        String fullName = prefManager.getString("user_full_name", "Utente");
        nameTextView.setText("Benvenuto " + fullName);
        emailTextView.setText(userRole != null ? userRole.toUpperCase() : "");
    }

    private void updateMenuByRole() {
        if (userRole == null) return;

        boolean isProf = userRole.equalsIgnoreCase("professore");
        boolean isStud = userRole.equalsIgnoreCase("studente");

        binding.navView.getMenu().findItem(R.id.nav_appointments_pending).setVisible(isProf);
        binding.navView.getMenu().findItem(R.id.nav_manage_availability).setVisible(isProf);
        binding.navView.getMenu().findItem(R.id.nav_office_hours).setVisible(isProf);
        
        binding.navView.getMenu().findItem(R.id.nav_book_appointment).setVisible(isStud);
        binding.navView.getMenu().findItem(R.id.nav_my_appointments).setVisible(isStud);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_home) {
            loadDefaultFragment();
        } else if (id == R.id.nav_appointments_pending) {
            fragment = new ElencoAppuntamentiSospesiFragment();
        } else if (id == R.id.nav_manage_availability) {
            fragment = new ElencoIndisponibilitaFragment();
        } else if (id == R.id.nav_office_hours) {
            fragment = new ModificaOrarioFragment();
        } else if (id == R.id.nav_all_appointments) {
            fragment = new ElencoAppuntamentiFragment();
        } else if (id == R.id.nav_book_appointment) {
            fragment = new PrenotaAppuntamentoCalendarioFragment();
        } else if (id == R.id.nav_my_appointments) {
            fragment = new ElencoAppuntamentiStudenteFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else if (id == R.id.nav_logout) {
            logout();
        }

        if (fragment != null) {
            replaceFragment(fragment);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.commit();
    }

    private void logout() {
        prefManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}