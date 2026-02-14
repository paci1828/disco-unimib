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

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.databinding.FragmentModificaOrarioBinding;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.ProfessoriViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ModificaOrarioFragment extends Fragment {

    private FragmentModificaOrarioBinding binding;
    private ProfessoriViewModel viewModel;
    private String profId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ITALY);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentModificaOrarioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfessoriViewModel.class);
        SharedPreferencesManager prefManager = new SharedPreferencesManager(requireContext());
        profId = prefManager.getUserUid();

        loadCurrentOrario();

        binding.oraInizioEditText.setOnClickListener(v -> showTimePicker(true));
        binding.oraFineEditText.setOnClickListener(v -> showTimePicker(false));
        binding.saveButton.setOnClickListener(v -> saveOrario());
    }

    private void showTimePicker(boolean isStartTime) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Seleziona orario")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            String time = String.format(Locale.ITALY, "%02d:%02d", picker.getHour(), picker.getMinute());
            if (isStartTime) {
                binding.oraInizioEditText.setText(time);
            } else {
                binding.oraFineEditText.setText(time);
            }
        });

        picker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    private void loadCurrentOrario() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getDatiProfessore(profId).observe(getViewLifecycleOwner(), professore -> {
            binding.progressBar.setVisibility(View.GONE);
            if (professore != null) {
                binding.giornoEditText.setText(professore.getGiornoRicevimento());
                binding.oraInizioEditText.setText(professore.getOraInizio());
                binding.oraFineEditText.setText(professore.getOraFine());
                binding.durataEditText.setText(String.valueOf(professore.getDurataAppuntamento()));
            }
        });
    }

    private void saveOrario() {
        String giorno = Objects.requireNonNull(binding.giornoEditText.getText()).toString().trim();
        String inizio = Objects.requireNonNull(binding.oraInizioEditText.getText()).toString().trim();
        String fine = Objects.requireNonNull(binding.oraFineEditText.getText()).toString().trim();
        String durataStr = Objects.requireNonNull(binding.durataEditText.getText()).toString().trim();

        if (TextUtils.isEmpty(giorno) || TextUtils.isEmpty(inizio) || TextUtils.isEmpty(fine) || TextUtils.isEmpty(durataStr)) {
            Toast.makeText(getContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateTimes(inizio, fine)) {
            Toast.makeText(getContext(), "L'ora di inizio deve essere precedente all'ora di fine", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        Map<String, Object> updates = new HashMap<>();
        updates.put("giornoRicevimento", giorno);
        updates.put("oraInizio", inizio);
        updates.put("oraFine", fine);
        updates.put("durataAppuntamento", Integer.parseInt(durataStr));

        FirebaseFirestore.getInstance().collection("users").document(profId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Orario aggiornato correttamente", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Errore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateTimes(String start, String end) {
        try {
            Date startDate = timeFormat.parse(start);
            Date endDate = timeFormat.parse(end);
            return startDate != null && endDate != null && startDate.before(endDate);
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}