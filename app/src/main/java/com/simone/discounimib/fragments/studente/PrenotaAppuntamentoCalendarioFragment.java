package com.simone.discounimib.fragments.studente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.simone.discounimib.R;
import com.simone.discounimib.adapters.ProfessoriAdapter;
import com.simone.discounimib.databinding.FragmentPrenotaAppuntamentoCalendarioBinding;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.utils.StatoRichiesta;
import com.simone.discounimib.viewmodels.ProfessoriViewModel;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class PrenotaAppuntamentoCalendarioFragment extends Fragment {
    private FragmentPrenotaAppuntamentoCalendarioBinding binding;
    private ProfessoriViewModel viewModel;
    private ProfessoriAdapter adapter;
    private String selectedDate;
    private LiveData<StatoRichiesta<List<Professore>>> richiesta;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPrenotaAppuntamentoCalendarioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfessoriViewModel.class);
        adapter = new ProfessoriAdapter();
        binding.professorsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.professorsRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(prof -> getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, PrenotaAppuntamentoFragment.newInstance(prof.getUid(), selectedDate))
                .addToBackStack(null).commit());
        LocalDate oggi = LocalDate.now(CalcolatoreDisponibilita.FUSO);
        if (savedInstanceState != null) selectedDate = savedInstanceState.getString("data");
        if (selectedDate == null || LocalDate.parse(selectedDate, CalcolatoreDisponibilita.DATA).isBefore(oggi)) {
            selectedDate = oggi.format(CalcolatoreDisponibilita.DATA);
        }
        binding.calendarView.setMinDate(oggi.atStartOfDay(CalcolatoreDisponibilita.FUSO).toInstant().toEpochMilli());
        binding.calendarView.setDate(LocalDate.parse(selectedDate, CalcolatoreDisponibilita.DATA)
                .atStartOfDay(CalcolatoreDisponibilita.FUSO).toInstant().toEpochMilli());
        binding.calendarView.setOnDateChangeListener((calendar, year, month, day) -> {
            selectedDate = LocalDate.of(year, month + 1, day).format(CalcolatoreDisponibilita.DATA);
            observeProfessori();
        });
        binding.emptyTextView.setOnClickListener(v -> observeProfessori());
        observeProfessori();
    }

    private void observeProfessori() {
        if (richiesta != null) richiesta.removeObservers(getViewLifecycleOwner());
        adapter.setProfessoriList(Collections.emptyList());
        binding.emptyTextView.setVisibility(View.GONE);
        richiesta = viewModel.getProfessoriDisponibili(selectedDate);
        richiesta.observe(getViewLifecycleOwner(), stato -> {
            binding.progressBar.setVisibility(stato.caricamento ? View.VISIBLE : View.GONE);
            if (stato.caricamento) return;
            List<Professore> professori = stato.dati == null ? Collections.emptyList() : stato.dati;
            adapter.setProfessoriList(professori);
            binding.emptyTextView.setText(stato.errore != null ? stato.errore
                    : "Nessun docente disponibile. Scegli un'altra data.");
            binding.emptyTextView.setVisibility(professori.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("data", selectedDate);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        richiesta = null;
        binding.professorsRecyclerView.setAdapter(null);
        binding = null;
    }
}
