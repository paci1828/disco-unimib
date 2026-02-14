package com.simone.discounimib.fragments.professore;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.simone.discounimib.databinding.FragmentAggiungiIndisponibilitaBinding;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.repositories.IndisponibilitaRepository;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.IndisponibilitaViewModel;

import java.util.Calendar;
import java.util.Locale;

public class AggiungiIndisponibilitaFragment extends Fragment {

    private FragmentAggiungiIndisponibilitaBinding binding;
    private IndisponibilitaViewModel viewModel;
    private Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAggiungiIndisponibilitaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(IndisponibilitaViewModel.class);

        binding.startDateEditText.setOnClickListener(v -> showDatePicker(true));
        binding.endDateEditText.setOnClickListener(v -> showDatePicker(false));
        binding.startTimeEditText.setOnClickListener(v -> showTimePicker(true));
        binding.endTimeEditText.setOnClickListener(v -> showTimePicker(false));

        binding.saveButton.setOnClickListener(v -> saveIndisponibilita());
    }

    private void showDatePicker(boolean isStartDate) {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.ITALY, "%02d/%02d/%d", dayOfMonth, month + 1, year);
            if (isStartDate) binding.startDateEditText.setText(date);
            else binding.endDateEditText.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStartTime) {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String time = String.format(Locale.ITALY, "%02d:%02d", hourOfDay, minute);
            if (isStartTime) binding.startTimeEditText.setText(time);
            else binding.endTimeEditText.setText(time);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void saveIndisponibilita() {
        String startData = binding.startDateEditText.getText().toString();
        String endData = binding.endDateEditText.getText().toString();
        String startOra = binding.startTimeEditText.getText().toString();
        String endOra = binding.endTimeEditText.getText().toString();
        String motivo = binding.reasonEditText.getText().toString();

        if (startData.isEmpty() || endData.isEmpty() || motivo.isEmpty()) {
            Toast.makeText(getContext(), "Compila i campi obbligatori", Toast.LENGTH_SHORT).show();
            return;
        }

        Indisponibilita ind = new Indisponibilita();
        ind.setIdProfessore(new SharedPreferencesManager(requireContext()).getUserUid());
        ind.setDataInizio(startData);
        ind.setDataFine(endData);
        ind.setOraInizio(startOra);
        ind.setOraFine(endOra);
        ind.setMotivo(motivo);

        viewModel.aggiungiIndisponibilita(ind, new IndisponibilitaRepository.OnTaskCompleted() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Indisponibilità salvata", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "Errore: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}