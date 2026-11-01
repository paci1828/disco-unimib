package com.simone.discounimib.fragments.professore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simone.discounimib.R;
import com.simone.discounimib.databinding.FragmentModificaOrarioBinding;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.utils.StatoRichiesta;
import com.simone.discounimib.viewmodels.ModificaOrarioViewModel;
import java.time.LocalTime;
import java.util.Locale;

public class ModificaOrarioFragment extends Fragment {
    private FragmentModificaOrarioBinding binding;
    private ModificaOrarioViewModel viewModel;
    private AlertDialog giornoDialog;
    private boolean aggiornaCampi;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentModificaOrarioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ModificaOrarioViewModel.class);
        binding.giornoEditText.setKeyListener(null);
        binding.giornoEditText.setOnClickListener(v -> {
            String[] giorni = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica"};
            giornoDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle("Giorno di ricevimento")
                    .setItems(giorni, (dialog, which) -> viewModel.setGiorno(giorni[which])).show();
        });
        binding.oraInizioEditText.setOnClickListener(v -> showTimePicker(true));
        binding.oraFineEditText.setOnClickListener(v -> showTimePicker(false));
        for (boolean inizio : new boolean[]{true, false}) {
            Fragment ripristinato = getChildFragmentManager().findFragmentByTag(tag(inizio));
            if (ripristinato instanceof MaterialTimePicker) collegaPicker((MaterialTimePicker) ripristinato, inizio);
        }
        binding.durataEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!aggiornaCampi) viewModel.setDurata(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        binding.saveButton.setOnClickListener(v -> {
            if (viewModel.daRicaricare()) viewModel.ricarica();
            else viewModel.salva();
        });
        viewModel.getCaricamento().observe(getViewLifecycleOwner(), stato -> mostra());
        viewModel.getSalvataggio().observe(getViewLifecycleOwner(), stato -> mostra());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        viewModel.inizializza(user == null ? null : user.getUid());
        mostra();
    }
    private static String tag(boolean inizio) { return inizio ? "ORARIO_INIZIO" : "ORARIO_FINE"; }
    private void showTimePicker(boolean inizio) {
        LocalTime corrente;
        try {
            corrente = LocalTime.parse(inizio ? viewModel.getInizio() : viewModel.getFine(), CalcolatoreDisponibilita.ORA);
        } catch (RuntimeException e) { corrente = LocalTime.NOON; }
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(corrente.getHour()).setMinute(corrente.getMinute()).setTitleText("Seleziona orario").build();
        collegaPicker(picker, inizio);
        picker.show(getChildFragmentManager(), tag(inizio));
    }
    private void collegaPicker(MaterialTimePicker picker, boolean inizio) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(v -> {
            String ora = String.format(Locale.ITALY, "%02d:%02d", picker.getHour(), picker.getMinute());
            if (inizio) viewModel.setInizio(ora); else viewModel.setFine(ora);
        });
    }
    private void mostra() {
        if (binding == null) return;
        aggiornaCampi = true;
        if (!android.text.TextUtils.equals(viewModel.getGiorno(), binding.giornoEditText.getText())) binding.giornoEditText.setText(viewModel.getGiorno());
        if (!android.text.TextUtils.equals(viewModel.getInizio(), binding.oraInizioEditText.getText())) binding.oraInizioEditText.setText(viewModel.getInizio());
        if (!android.text.TextUtils.equals(viewModel.getFine(), binding.oraFineEditText.getText())) binding.oraFineEditText.setText(viewModel.getFine());
        if (!android.text.TextUtils.equals(viewModel.getDurata(), binding.durataEditText.getText())) binding.durataEditText.setText(viewModel.getDurata());
        aggiornaCampi = false;
        StatoRichiesta<Professore> carico = viewModel.getCaricamento().getValue();
        StatoRichiesta<Boolean> salva = viewModel.getSalvataggio().getValue();
        boolean busy = viewModel.occupato();
        boolean erroreCaricamento = carico != null && carico.errore != null;
        binding.progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!busy && (erroreCaricamento || (carico != null && carico.dati != null)));
        binding.saveButton.setText(salva != null && salva.caricamento ? R.string.orario_salvataggio
                : carico != null && carico.caricamento ? R.string.orario_caricamento
                : erroreCaricamento ? R.string.orario_riprova : R.string.orario_salva);
        binding.giornoEditText.setEnabled(!busy);
        binding.oraInizioEditText.setEnabled(!busy);
        binding.oraFineEditText.setEnabled(!busy);
        binding.durataEditText.setEnabled(!busy);
        String messaggio = erroreCaricamento ? carico.errore
                : salva != null && salva.errore != null ? salva.errore
                : salva != null && Boolean.TRUE.equals(salva.dati) ? getString(R.string.orario_salvato) : "";
        binding.esitoTextView.setText(messaggio);
        binding.esitoTextView.setVisibility(messaggio.isEmpty() ? View.GONE : View.VISIBLE);
    }
    @Override public void onDestroyView() {
        if (giornoDialog != null) giornoDialog.dismiss();
        super.onDestroyView();
        binding = null;
    }
}
