package com.simone.discounimib.fragments.professore;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.simone.discounimib.databinding.FragmentModificaOrarioBinding;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.repositories.ProfessoriRepository;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.ProfessoriViewModel;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;

public class ModificaOrarioFragment extends Fragment {
    private FragmentModificaOrarioBinding binding;
    private ProfessoriViewModel viewModel;
    private String profId;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModificaOrarioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfessoriViewModel.class);
        profId = new SharedPreferencesManager(requireContext()).getUserUid();
        binding.giornoEditText.setKeyListener(null);
        binding.giornoEditText.setOnClickListener(v -> {
            String[] giorni = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica"};
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Giorno di ricevimento")
                    .setItems(giorni, (dialog, which) -> {
                        if (binding != null) binding.giornoEditText.setText(giorni[which]);
                    }).show();
        });
        binding.oraInizioEditText.setOnClickListener(v -> showTimePicker(true));
        binding.oraFineEditText.setOnClickListener(v -> showTimePicker(false));
        binding.saveButton.setOnClickListener(v -> saveOrario());
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);
        viewModel.getDatiProfessore(profId).observe(getViewLifecycleOwner(), prof -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.saveButton.setEnabled(prof != null);
            if (prof == null) {
                Toast.makeText(requireContext(), "Impossibile caricare l'orario", Toast.LENGTH_LONG).show();
                return;
            }
            // Android ripristina i campi dopo una ricreazione della vista.
            if (savedInstanceState == null) {
                binding.giornoEditText.setText(prof.getGiornoRicevimento());
                binding.oraInizioEditText.setText(prof.getOraInizio());
                binding.oraFineEditText.setText(prof.getOraFine());
                binding.durataEditText.setText(String.valueOf(prof.getDurataAppuntamento()));
            }
        });
    }

    private void showTimePicker(boolean inizio) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H).setHour(12).setMinute(0)
                .setTitleText("Seleziona orario").build();
        picker.addOnPositiveButtonClickListener(v -> {
            if (binding == null) return;
            String ora = String.format(Locale.ITALY, "%02d:%02d", picker.getHour(), picker.getMinute());
            if (inizio) binding.oraInizioEditText.setText(ora);
            else binding.oraFineEditText.setText(ora);
        });
        picker.show(getChildFragmentManager(), "TIME_PICKER");
    }

    private void saveOrario() {
        String giorno = Objects.requireNonNull(binding.giornoEditText.getText()).toString().trim();
        String inizio = Objects.requireNonNull(binding.oraInizioEditText.getText()).toString().trim();
        String fine = Objects.requireNonNull(binding.oraFineEditText.getText()).toString().trim();
        String durataStr = Objects.requireNonNull(binding.durataEditText.getText()).toString().trim();
        if (TextUtils.isEmpty(giorno) || TextUtils.isEmpty(inizio) || TextUtils.isEmpty(fine) || TextUtils.isEmpty(durataStr)) {
            Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show();
            return;
        }
        final int durata;
        try {
            durata = Integer.parseInt(durataStr);
            CalcolatoreDisponibilita.giorno(giorno);
            CalcolatoreDisponibilita.validaOrario(LocalTime.parse(inizio, CalcolatoreDisponibilita.ORA),
                    LocalTime.parse(fine, CalcolatoreDisponibilita.ORA), durata);
        } catch (RuntimeException e) {
            Toast.makeText(requireContext(), "Controlla giorno, orari e durata: deve essere positiva e rientrare nella fascia",
                    Toast.LENGTH_LONG).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);
        viewModel.salvaOrario(profId, giorno, inizio, fine, durata, new ProfessoriRepository.OnTaskCompleted() {
            @Override public void onSuccess() {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Orario aggiornato correttamente", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            @Override public void onError(String message) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.saveButton.setEnabled(true);
                Toast.makeText(requireContext(), "Errore: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
