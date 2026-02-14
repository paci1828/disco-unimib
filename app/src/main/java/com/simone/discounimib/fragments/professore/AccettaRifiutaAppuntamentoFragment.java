package com.simone.discounimib.fragments.professore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;
import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.databinding.FragmentAccettaRifiutaAppuntamentoBinding;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.repositories.AppuntamentiRepository;
import com.simone.discounimib.utils.CalendarServiceHelper;
import com.simone.discounimib.utils.GmailServiceHelper;
import com.simone.discounimib.viewmodels.AppuntamentiViewModel;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccettaRifiutaAppuntamentoFragment extends Fragment {

    private static final String ARG_APPUNTAMENTO_ID = "appuntamento_id";
    private FragmentAccettaRifiutaAppuntamentoBinding binding;
    private AppuntamentiViewModel viewModel;
    private String appuntamentoId;
    private Appuntamento currentAppuntamento;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static AccettaRifiutaAppuntamentoFragment newInstance(String appuntamentoId) {
        AccettaRifiutaAppuntamentoFragment fragment = new AccettaRifiutaAppuntamentoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPUNTAMENTO_ID, appuntamentoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appuntamentoId = getArguments().getString(ARG_APPUNTAMENTO_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccettaRifiutaAppuntamentoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AppuntamentiViewModel.class);

        loadAppuntamentoDetails();

        binding.accettaButton.setOnClickListener(v -> gestisciEsito("accettato"));
        binding.rifiutaButton.setOnClickListener(v -> gestisciEsito("rifiutato"));
    }

    private void loadAppuntamentoDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("appuntamenti").document(appuntamentoId)
                .get().addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    currentAppuntamento = documentSnapshot.toObject(Appuntamento.class);
                    if (currentAppuntamento != null) {
                        displayDetails();
                    }
                });
    }

    private void displayDetails() {
        binding.studenteNameTextView.setText(currentAppuntamento.getNomeStudente());
        binding.dateTimeTextView.setText(currentAppuntamento.getData() + " ore " + currentAppuntamento.getOraInizio());
        binding.motivazioneTextView.setText(currentAppuntamento.getMotivazione());
    }

    private void gestisciEsito(String nuovoStato) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.accettaButton.setEnabled(false);
        binding.rifiutaButton.setEnabled(false);

        viewModel.updateStato(appuntamentoId, nuovoStato, new AppuntamentiRepository.OnTaskCompleted() {
            @Override
            public void onSuccess() {
                if ("accettato".equals(nuovoStato)) {
                    eseguiAzioniGoogle();
                } else {
                    concludiOperazione(nuovoStato);
                }
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.accettaButton.setEnabled(true);
                binding.rifiutaButton.setEnabled(true);
                Toast.makeText(getContext(), "Errore: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eseguiAzioniGoogle() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) {
            concludiOperazione("accettato (senza Google Sync)");
            return;
        }

        executorService.execute(() -> {
            try {
                // Sincronizzazione Calendario
                GoogleAccountCredential credentialCal = GoogleAccountCredential.usingOAuth2(
                        requireContext(), Collections.singleton(CalendarScopes.CALENDAR));
                credentialCal.setSelectedAccount(account.getAccount());
                CalendarServiceHelper calHelper = new CalendarServiceHelper(credentialCal);
                
                // Formattazione data per Google (ISO 8601) - esempio semplificato
                String isoDate = currentAppuntamento.getData().replace("/", "-") + "T" + currentAppuntamento.getOraInizio() + ":00Z";
                calHelper.createEvent("Ricevimento DISCo", "Con studente: " + currentAppuntamento.getNomeStudente(), isoDate, isoDate);

                // Invio Email
                GoogleAccountCredential credentialMail = GoogleAccountCredential.usingOAuth2(
                        requireContext(), Collections.singleton(GmailScopes.GMAIL_SEND));
                credentialMail.setSelectedAccount(account.getAccount());
                GmailServiceHelper mailHelper = new GmailServiceHelper(credentialMail);
                
                mailHelper.sendConfirmationEmail(
                        currentAppuntamento.getEmailStudente(),
                        "Appuntamento DISCo Confermato",
                        "Gentile studente, il professore ha accettato il tuo appuntamento per il " + currentAppuntamento.getData() + " alle " + currentAppuntamento.getOraInizio()
                );

                requireActivity().runOnUiThread(() -> concludiOperazione("accettato e sincronizzato"));

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Sync Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    concludiOperazione("accettato (errore sync)");
                });
            }
        });
    }

    private void concludiOperazione(String stato) {
        binding.progressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Appuntamento " + stato, Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        binding = null;
    }
}