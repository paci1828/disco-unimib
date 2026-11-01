package com.simone.discounimib.fragments.studente;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.simone.discounimib.adapters.OrariDisponibiliAdapter;
import com.simone.discounimib.databinding.FragmentPrenotaAppuntamentoBinding;
import com.simone.discounimib.repositories.AppuntamentiRepository.Disponibilita;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.utils.StatoRichiesta;
import com.simone.discounimib.viewmodels.PrenotazioneViewModel;

public class PrenotaAppuntamentoFragment extends Fragment {
    private static final String ARG_PROF_ID = "prof_id";
    private static final String ARG_DATE = "date";
    private FragmentPrenotaAppuntamentoBinding binding;
    private PrenotazioneViewModel viewModel;
    private OrariDisponibiliAdapter adapter;

    public static PrenotaAppuntamentoFragment newInstance(String profId, String date) {
        PrenotaAppuntamentoFragment fragment = new PrenotaAppuntamentoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROF_ID, profId);
        args.putString(ARG_DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPrenotaAppuntamentoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PrenotazioneViewModel.class);
        adapter = new OrariDisponibiliAdapter();
        binding.slotsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        binding.slotsRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(viewModel::setOrario);
        binding.reasonEditText.setText(viewModel.getMotivazione());
        binding.reasonEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setMotivazione(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        String profId = getArguments() == null ? null : getArguments().getString(ARG_PROF_ID);
        String data = getArguments() == null ? null : getArguments().getString(ARG_DATE);
        binding.dateTextView.setText("Data: " + (data == null ? "" : data));
        binding.bookButton.setOnClickListener(v -> {
            StatoRichiesta<Disponibilita> stato = viewModel.getDisponibilita().getValue();
            if (stato != null && stato.errore != null) {
                viewModel.ricarica();
            } else {
                SharedPreferencesManager pref = new SharedPreferencesManager(requireContext());
                viewModel.prenota(pref.getString("user_full_name", "Studente"));
            }
        });
        viewModel.getDisponibilita().observe(getViewLifecycleOwner(), stato -> aggiornaVista());
        viewModel.getPrenotazione().observe(getViewLifecycleOwner(), esito -> {
            aggiornaVista();
            if (esito == null || esito.caricamento) return;
            if (esito.errore != null) {
                Toast.makeText(requireContext(), esito.errore, Toast.LENGTH_LONG).show();
                viewModel.consumaEsito();
            } else if (Boolean.TRUE.equals(esito.dati)) {
                viewModel.consumaEsito();
                Toast.makeText(requireContext(), "Prenotazione inviata!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });
        viewModel.inizializza(profId, data);
    }

    private void aggiornaVista() {
        StatoRichiesta<Disponibilita> stato = viewModel.getDisponibilita().getValue();
        StatoRichiesta<Boolean> operazione = viewModel.getPrenotazione().getValue();
        boolean occupato = (stato != null && stato.caricamento)
                || (operazione != null && operazione.caricamento);
        binding.progressBar.setVisibility(occupato ? View.VISIBLE : View.GONE);
        binding.reasonEditText.setEnabled(!occupato);
        binding.slotsRecyclerView.setVisibility(occupato ? View.INVISIBLE : View.VISIBLE);
        binding.bookButton.setEnabled(!occupato && stato != null
                && (stato.errore != null || (stato.dati != null && !stato.dati.slots.isEmpty())));
        binding.bookButton.setText(stato != null && stato.errore != null ? "Riprova" : "Prenota");
        if (stato == null || stato.caricamento) return;
        if (stato.errore != null) {
            adapter.setSlots(java.util.Collections.emptyList());
            binding.slotTitleTextView.setText(stato.errore);
        } else if (stato.dati != null) {
            binding.profNameTextView.setText(stato.dati.professore.getNome() + " " + stato.dati.professore.getCognome());
            binding.slotTitleTextView.setText(stato.dati.slots.isEmpty()
                    ? "Nessun orario disponibile per questa data" : "Seleziona orario");
            adapter.setSlots(stato.dati.slots);
            adapter.setSelectedTime(viewModel.getOrario());
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding.slotsRecyclerView.setAdapter(null);
        binding = null;
    }
}
