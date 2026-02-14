package com.simone.discounimib.fragments.studente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.simone.discounimib.R;
import com.simone.discounimib.adapters.ProfessoriAdapter;
import com.simone.discounimib.databinding.FragmentPrenotaAppuntamentoCalendarioBinding;
import com.simone.discounimib.viewmodels.ProfessoriViewModel;
import java.util.Calendar;
import java.util.Locale;

public class PrenotaAppuntamentoCalendarioFragment extends Fragment {

    private FragmentPrenotaAppuntamentoCalendarioBinding binding;
    private ProfessoriViewModel viewModel;
    private ProfessoriAdapter adapter;
    private String selectedDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPrenotaAppuntamentoCalendarioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfessoriViewModel.class);

        setupRecyclerView();
        setupCalendar();
        
        Calendar c = Calendar.getInstance();
        updateDateString(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        observeProfessori();
    }

    private void setupRecyclerView() {
        adapter = new ProfessoriAdapter();
        binding.professorsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.professorsRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(professore -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, PrenotaAppuntamentoFragment.newInstance(professore.getUid(), selectedDate))
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateDateString(year, month, dayOfMonth);
            observeProfessori();
        });
    }

    private void updateDateString(int year, int month, int dayOfMonth) {
        selectedDate = String.format(Locale.ITALY, "%02d/%02d/%d", dayOfMonth, month + 1, year);
    }

    private void observeProfessori() {
        binding.progressBar.setVisibility(View.VISIBLE);
        // ⭐ Corretto: Ora usa il filtro indisponibilità basato sulla data
        viewModel.getProfessoriDisponibili(selectedDate).observe(getViewLifecycleOwner(), professori -> {
            binding.progressBar.setVisibility(View.GONE);
            if (professori != null && !professori.isEmpty()) {
                adapter.setProfessoriList(professori);
                binding.emptyTextView.setVisibility(View.GONE);
            } else {
                adapter.setProfessoriList(new java.util.ArrayList<>());
                binding.emptyTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}