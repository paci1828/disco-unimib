package com.simone.discounimib.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.databinding.ActivityLoginBinding;
import com.simone.discounimib.models.User;
import com.simone.discounimib.utils.SharedPreferencesManager;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferencesManager prefManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefManager = new SharedPreferencesManager(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("791889335400-ml354c416rqha25351cavn7acav3rjba.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        binding.registerTextView.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        
        binding.forgotPasswordTextView.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Inserisci la tua email per il reset", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Email di reset inviata", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        updateLastAccessAndGetData(uid);
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError("Inserisci un'email valida");
            return;
        } else {
            binding.emailInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordInputLayout.setError("Inserisci la password");
            return;
        } else {
            binding.passwordInputLayout.setError(null);
        }

        binding.loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        updateLastAccessAndGetData(uid);
                    } else {
                        binding.loginButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Autenticazione fallita: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateLastAccessAndGetData(String uid) {
        db.collection("users").document(uid)
                .update("ultimoAccesso", Timestamp.now())
                .addOnCompleteListener(task -> getUserData(uid));
    }

    private void getUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        prefManager.saveUser(uid, user.getRuolo());
                        // Salviamo anche nome e cognome per visualizzarli nella Home
                        prefManager.saveString("user_full_name", user.getNome() + " " + user.getCognome());
                        prefManager.setRememberMe(binding.rememberMeCheckBox.isChecked());
                        
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        binding.loginButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Utente non trovato su Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.loginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Errore recupero dati: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}