package com.simone.discounimib.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.databinding.ActivityRegisterBinding;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.models.User;
import com.simone.discounimib.utils.SharedPreferencesManager;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferencesManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefManager = new SharedPreferencesManager(this);

        binding.registerButton.setOnClickListener(v -> registerUser());
        binding.loginLinkTextView.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = Objects.requireNonNull(binding.nameEditText.getText()).toString().trim();
        String surname = Objects.requireNonNull(binding.surnameEditText.getText()).toString().trim();
        String email = Objects.requireNonNull(binding.emailEditText.getText()).toString().trim();
        String password = Objects.requireNonNull(binding.passwordEditText.getText()).toString().trim();
        String matricola = Objects.requireNonNull(binding.matricolaEditText.getText()).toString().trim();
        String role = binding.radioProfessore.isChecked() ? "professore" : "studente";

        if (TextUtils.isEmpty(name)) {
            binding.nameInputLayout.setError("Inserisci il nome");
            return;
        }
        if (TextUtils.isEmpty(surname)) {
            binding.surnameInputLayout.setError("Inserisci il cognome");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError("Inserisci un'email valida");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.passwordInputLayout.setError("La password deve essere di almeno 6 caratteri");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid, name, surname, email, role, matricola);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.registerButton.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Errore: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String surname, String email, String role, String matricola) {
        User user;
        Timestamp now = Timestamp.now();
        
        if ("professore".equals(role)) {
            user = new Professore(uid, email, name, surname, role, null,  null, null,null, 30);
        } else {
            user = new User(uid, email, name, surname, role, matricola);
        }
        
        user.setDataCreazione(now);
        user.setUltimoAccesso(now);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    prefManager.saveUser(uid, role);
                    Toast.makeText(RegisterActivity.this, "Registrazione completata!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Errore salvataggio dati: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}