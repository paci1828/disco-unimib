package com.simone.discounimib.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simone.discounimib.R;
import com.simone.discounimib.adapters.OrariDisponibiliAdapter;
import com.simone.discounimib.databinding.FragmentPrenotaAppuntamentoBinding;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.repositories.AppuntamentiRepository;
import com.simone.discounimib.utils.StatoRichiesta;
import com.simone.discounimib.viewmodels.GestioneAppuntamentoViewModel;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

public class RiprogrammaAppuntamentoFragment extends Fragment {
    private FragmentPrenotaAppuntamentoBinding binding;
    private GestioneAppuntamentoViewModel viewModel;
    private OrariDisponibiliAdapter adapter;
    private DatePickerDialog calendario;
    private AlertDialog conferma;
    public static RiprogrammaAppuntamentoFragment newInstance(String id) {
        RiprogrammaAppuntamentoFragment fragment = new RiprogrammaAppuntamentoFragment();
        Bundle args = new Bundle();
        args.putString("appuntamento_id", id);
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPrenotaAppuntamentoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GestioneAppuntamentoViewModel.class);
        binding.titleTextView.setText(R.string.ricevimento_riprogramma);
        binding.reasonEditText.setEnabled(false);
        adapter = new OrariDisponibiliAdapter();
        binding.slotsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        binding.slotsRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(ora -> { viewModel.setOrario(ora); mostra(); });
        binding.dateTextView.setOnClickListener(v -> scegliData());
        binding.bookButton.setOnClickListener(v -> {
            StatoRichiesta<AppuntamentiRepository.Disponibilita> slots = viewModel.getSlots().getValue();
            if (slots != null && slots.errore != null) { viewModel.ricaricaSlots(); return; }
            StatoRichiesta<Appuntamento> dettagli = viewModel.getDettagli().getValue();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            boolean docente = user != null && dettagli != null && dettagli.dati != null
                    && user.getUid().equals(dettagli.dati.getIdProfessore());
            conferma = new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(docente ? R.string.ricevimento_conferma_riprogrammazione_docente
                            : R.string.ricevimento_conferma_riprogrammazione_studente)
                    .setNegativeButton(R.string.ricevimento_indietro, null)
                    .setPositiveButton(R.string.ricevimento_conferma, (d, which) -> viewModel.riprogramma()).show();
        });
        viewModel.getDettagli().observe(getViewLifecycleOwner(), stato -> {
            if (stato.dati != null) {
                if (stato.dati.getSostituitoDaId() != null && !viewModel.occupato()) {
                    Toast.makeText(requireContext(), R.string.ricevimento_riprogrammato, Toast.LENGTH_LONG).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }
                binding.profNameTextView.setText(stato.dati.getNomeProfessore());
                binding.reasonEditText.setText(stato.dati.getMotivazione());
                viewModel.preparaRiprogrammazione(stato.dati.getData());
            } else if (stato.errore != null) binding.slotTitleTextView.setText(stato.errore);
            mostra();
        });
        viewModel.getSlots().observe(getViewLifecycleOwner(), stato -> mostra());
        viewModel.getOperazione().observe(getViewLifecycleOwner(), esito -> {
            mostra();
            if (esito == null || esito.caricamento) return;
            if (esito.errore != null) {
                Toast.makeText(requireContext(), esito.errore, Toast.LENGTH_LONG).show();
                viewModel.consumaEsito();
            } else {
                viewModel.consumaEsito();
                Toast.makeText(requireContext(), R.string.ricevimento_riprogrammato, Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            }
        });
    }
    private void mostra() {
        StatoRichiesta<AppuntamentiRepository.Disponibilita> stato = viewModel.getSlots().getValue();
        boolean busy = viewModel.occupato() || (stato != null && stato.caricamento);
        binding.progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.dateTextView.setEnabled(!busy);
        binding.dateTextView.setText(getString(R.string.ricevimento_data_scegli,
                viewModel.getData() == null ? "" : viewModel.getData()));
        boolean errore = stato != null && stato.errore != null;
        binding.bookButton.setText(errore ? R.string.ricevimento_riprova : R.string.ricevimento_conferma);
        binding.bookButton.setEnabled(!busy && (errore || viewModel.getOrario() != null));
        binding.slotsRecyclerView.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        if (stato == null || stato.caricamento) return;
        adapter.setSlots(stato.dati == null ? Collections.emptyList() : stato.dati.slots);
        adapter.setSelectedTime(viewModel.getOrario());
        if (errore) binding.slotTitleTextView.setText(stato.errore);
        else binding.slotTitleTextView.setText(stato.dati == null || stato.dati.slots.isEmpty()
                ? R.string.ricevimento_slot_vuoti : R.string.ricevimento_scegli_slot);
    }
    private void scegliData() {
        LocalDate oggi = LocalDate.now(CalcolatoreDisponibilita.FUSO);
        LocalDate scelta = viewModel.getData() == null ? oggi
                : LocalDate.parse(viewModel.getData(), CalcolatoreDisponibilita.DATA);
        calendario = new DatePickerDialog(requireContext(), (picker, anno, mese, giorno) -> {
            if (binding != null) viewModel.scegliData(LocalDate.of(anno, mese + 1, giorno)
                    .format(CalcolatoreDisponibilita.DATA));
        }, scelta.getYear(), scelta.getMonthValue() - 1, scelta.getDayOfMonth());
        calendario.getDatePicker().setMinDate(oggi.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        calendario.show();
    }
    @Override public void onDestroyView() {
        if (calendario != null) calendario.dismiss();
        if (conferma != null) conferma.dismiss();
        binding.slotsRecyclerView.setAdapter(null);
        super.onDestroyView();
        binding = null;
    }
}
