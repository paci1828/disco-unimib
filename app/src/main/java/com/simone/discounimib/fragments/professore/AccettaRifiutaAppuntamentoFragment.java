package com.simone.discounimib.fragments.professore;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simone.discounimib.R;
import com.simone.discounimib.databinding.FragmentAccettaRifiutaAppuntamentoBinding;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.domain.RegoleAppuntamento;
import com.simone.discounimib.fragments.RiprogrammaAppuntamentoFragment;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.sync.GoogleSyncAuth;
import com.simone.discounimib.utils.StatoRichiesta;
import com.simone.discounimib.viewmodels.GestioneAppuntamentoViewModel;
import java.time.LocalDateTime;

/** Dettaglio condiviso: le azioni visibili dipendono dal partecipante e dallo stato. */
public class AccettaRifiutaAppuntamentoFragment extends Fragment {
    private FragmentAccettaRifiutaAppuntamentoBinding binding;
    private GestioneAppuntamentoViewModel viewModel;
    private AlertDialog dialog;
    private String authUid;
    private final ActivityResultLauncher<IntentSenderRequest> autorizza = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (binding == null) return;
                try {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        erroreGoogle();
                        return;
                    }
                    completaAutorizzazione(Identity.getAuthorizationClient(requireContext())
                            .getAuthorizationResultFromIntent(result.getData()));
                } catch (Exception e) { erroreGoogle(); }
            });

    public static AccettaRifiutaAppuntamentoFragment newInstance(String id) {
        AccettaRifiutaAppuntamentoFragment fragment = new AccettaRifiutaAppuntamentoFragment();
        Bundle args = new Bundle();
        args.putString("appuntamento_id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) authUid = savedInstanceState.getString("google_auth_uid");
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccettaRifiutaAppuntamentoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GestioneAppuntamentoViewModel.class);
        binding.accettaButton.setOnClickListener(v -> viewModel.cambiaStato("accettato"));
        binding.rifiutaButton.setOnClickListener(v -> conferma(R.string.ricevimento_conferma_rifiuto,
                () -> viewModel.cambiaStato("rifiutato")));
        binding.annullaButton.setOnClickListener(v -> conferma(R.string.ricevimento_conferma_annullamento,
                () -> viewModel.cambiaStato("annullato")));
        binding.riprogrammaButton.setOnClickListener(v -> {
            Appuntamento app = appuntamento();
            if (app != null) apri(RiprogrammaAppuntamentoFragment.newInstance(app.getId()));
        });
        binding.collegatoButton.setOnClickListener(v -> {
            Appuntamento app = appuntamento();
            if (app != null) apri(newInstance(app.getSostituitoDaId() != null
                    ? app.getSostituitoDaId() : app.getAppuntamentoPrecedenteId()));
        });
        binding.googleSyncButton.setOnClickListener(v -> collegaGoogle());
        binding.emailVerificataButton.setOnClickListener(v -> conferma(R.string.google_verifica_email,
                () -> viewModel.ritentaSync(true)));
        viewModel.getDettagli().observe(getViewLifecycleOwner(), stato -> mostra());
        viewModel.getOperazione().observe(getViewLifecycleOwner(), esito -> {
            mostra();
            if (esito == null || esito.caricamento) return;
            Toast.makeText(requireContext(), esito.errore == null
                    ? getString(R.string.ricevimento_aggiornato) : esito.errore, Toast.LENGTH_LONG).show();
            viewModel.consumaEsito();
        });
    }
    private Appuntamento appuntamento() {
        StatoRichiesta<Appuntamento> stato = viewModel.getDettagli().getValue();
        return stato == null ? null : stato.dati;
    }
    private void mostra() {
        StatoRichiesta<Appuntamento> stato = viewModel.getDettagli().getValue();
        Appuntamento app = appuntamento();
        boolean busy = viewModel.occupato() || stato == null || stato.caricamento;
        binding.progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        for (View pulsante : new View[]{binding.accettaButton, binding.rifiutaButton,
                binding.annullaButton, binding.riprogrammaButton, binding.googleSyncButton,
                binding.emailVerificataButton, binding.collegatoButton}) {
            pulsante.setEnabled(!busy);
            pulsante.setVisibility(View.GONE);
        }
        if (app == null) {
            if (stato != null && stato.errore != null) binding.statoTextView.setText(stato.errore);
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean docente = user != null && user.getUid().equals(app.getIdProfessore());
        boolean modificabile = RegoleAppuntamento.attivo(app.getStato()) && RegoleAppuntamento.futuro(
                app.getData(), app.getOraInizio(), LocalDateTime.now(CalcolatoreDisponibilita.FUSO));
        binding.studenteNameTextView.setText(docente ? app.getNomeStudente() : app.getNomeProfessore());
        binding.dateTimeTextView.setText(app.getData() + "  " + app.getOraInizio() + " – " + app.getOraFine());
        binding.motivazioneTextView.setText(app.getMotivazione());
        binding.statoTextView.setText(getString(R.string.ricevimento_stato, app.getStato()));
        if (docente && modificabile && "sospeso".equals(app.getStato())) {
            binding.accettaButton.setVisibility(View.VISIBLE);
            binding.rifiutaButton.setVisibility(View.VISIBLE);
        }
        if (modificabile) {
            binding.annullaButton.setVisibility(View.VISIBLE);
            binding.riprogrammaButton.setVisibility(View.VISIBLE);
        }
        if (app.getSostituitoDaId() != null || app.getAppuntamentoPrecedenteId() != null) {
            binding.collegatoButton.setVisibility(View.VISIBLE);
            binding.collegatoButton.setText(app.getSostituitoDaId() != null
                    ? R.string.ricevimento_nuovo : R.string.ricevimento_precedente);
        }
        binding.syncDescrizioneTextView.setText(docente ? R.string.google_descrizione : R.string.google_studente);
        int messaggio = R.string.google_non_richiesta;
        String sync = app.getStatoSincronizzazione();
        if ("in_attesa".equals(sync)) messaggio = R.string.google_attesa;
        else if ("in_corso".equals(sync)) messaggio = R.string.google_in_corso;
        else if ("completata".equals(sync)) messaggio = R.string.google_completata;
        else if ("autorizzazione_richiesta".equals(sync)) messaggio = R.string.google_autorizza;
        else if ("email_da_verificare".equals(sync)) messaggio = R.string.google_email_incerta;
        else if ("errore_permanente".equals(sync)) messaggio = R.string.google_errore;
        binding.syncStatoTextView.setText(getString(messaggio)
                + (app.getErroreSincronizzazione() == null ? "" : "\n" + app.getErroreSincronizzazione()));
        if (docente) {
            binding.googleSyncButton.setVisibility(View.VISIBLE);
            if ("email_da_verificare".equals(sync)) binding.emailVerificataButton.setVisibility(View.VISIBLE);
        }
    }
    private void apri(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null).commit();
    }
    private void conferma(int messaggio, Runnable azione) {
        dialog = new MaterialAlertDialogBuilder(requireContext()).setMessage(messaggio)
                .setNegativeButton(R.string.ricevimento_indietro, null)
                .setPositiveButton(R.string.ricevimento_conferma, (d, which) -> azione.run()).show();
    }
    private void collegaGoogle() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Appuntamento app = appuntamento();
        if (user == null || app == null || !user.getUid().equals(app.getIdProfessore())) return;
        authUid = user.getUid();
        binding.googleSyncButton.setEnabled(false);
        try {
            Identity.getAuthorizationClient(requireContext()).authorize(GoogleSyncAuth.richiesta(user))
                    .addOnSuccessListener(result -> {
                        if (binding == null) return;
                        if (result.hasResolution() && result.getPendingIntent() != null) {
                            autorizza.launch(new IntentSenderRequest.Builder(result.getPendingIntent()).build());
                        } else completaAutorizzazione(result);
                    }).addOnFailureListener(e -> erroreGoogle());
        } catch (RuntimeException e) { erroreGoogle(); }
    }
    private void completaAutorizzazione(AuthorizationResult result) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (binding == null || user == null || !user.getUid().equals(authUid)
                || result.hasResolution() || result.getAccessToken() == null) {
            erroreGoogle();
            return;
        }
        GoogleSyncAuth.abilita(requireContext(), user.getUid());
        viewModel.ritentaSync(false);
    }
    private void erroreGoogle() {
        if (binding == null) return;
        binding.googleSyncButton.setEnabled(true);
        Toast.makeText(requireContext(), R.string.google_non_autorizzato, Toast.LENGTH_LONG).show();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("google_auth_uid", authUid);
    }
    @Override public void onDestroyView() {
        if (dialog != null) dialog.dismiss();
        super.onDestroyView();
        binding = null;
    }
}
