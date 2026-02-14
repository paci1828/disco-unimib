package com.simone.discounimib.fragments.studente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.simone.discounimib.adapters.OrariDisponibiliAdapter;
import com.simone.discounimib.databinding.FragmentPrenotaAppuntamentoBinding;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.repositories.AppuntamentiRepository;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.AppuntamentiViewModel;
import com.simone.discounimib.viewmodels.ProfessoriViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PrenotaAppuntamentoFragment extends Fragment {

    private static final String ARG_PROF_ID = "prof_id";
    private static final String ARG_DATE = "date";

    private FragmentPrenotaAppuntamentoBinding binding;
    private ProfessoriViewModel profViewModel;
    private AppuntamentiViewModel appViewModel;
    private OrariDisponibiliAdapter adapter;
    
    private String profId;
    private String selectedDate;
    private String selectedTime;
    private Professore currentProf;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ITALY);

    public static PrenotaAppuntamentoFragment newInstance(String profId, String date) {
        PrenotaAppuntamentoFragment fragment = new PrenotaAppuntamentoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROF_ID, profId);
        args.putString(ARG_DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            profId = getArguments().getString(ARG_PROF_ID);
            selectedDate = getArguments().getString(ARG_DATE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPrenotaAppuntamentoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profViewModel = new ViewModelProvider(this).get(ProfessoriViewModel.class);
        appViewModel = new ViewModelProvider(this).get(AppuntamentiViewModel.class);

        setupSlotsRecyclerView();
        loadProfData();

        binding.bookButton.setOnClickListener(v -> prenota());
    }

    private void setupSlotsRecyclerView() {
        adapter = new OrariDisponibiliAdapter();
        binding.slotsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        binding.slotsRecyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickListener(time -> selectedTime = time);
    }

    private void loadProfData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        profViewModel.getDatiProfessore(profId).observe(getViewLifecycleOwner(), professore -> {
            if (professore != null) {
                currentProf = professore;
                binding.profNameTextView.setText(professore.getNome() + " " + professore.getCognome());
                binding.dateTextView.setText("Data: " + selectedDate);
                caricaIndisponibilitaEGeneraSlots(professore);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void caricaIndisponibilitaEGeneraSlots(Professore prof) {
        FirebaseFirestore.getInstance().collection("indisponibilita")
                .whereEqualTo("idProfessore", profId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Indisponibilita> indisponibilitaList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        indisponibilitaList.add(doc.toObject(Indisponibilita.class));
                    }
                    generaSlots(prof, indisponibilitaList);
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    generaSlots(prof, new ArrayList<>());
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    private void generaSlots(Professore prof, List<Indisponibilita> indisponibilitaList) {
        List<String> slots = new ArrayList<>();
        String oraInizioStr = prof.getOraInizio();
        String oraFineStr = prof.getOraFine();
        int durata = prof.getDurataAppuntamento();

        if (oraInizioStr == null || oraFineStr == null || durata <= 0) {
            Toast.makeText(getContext(), "Orari del professore non configurati", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(Objects.requireNonNull(timeFormat.parse(oraInizioStr)));
            Date fine = timeFormat.parse(oraFineStr);

            while (cal.getTime().before(fine)) {
                String currentSlot = timeFormat.format(cal.getTime());
                
                if (isSlotDisponibile(currentSlot, indisponibilitaList)) {
                    slots.add(currentSlot);
                }
                
                cal.add(Calendar.MINUTE, durata);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        adapter.setSlots(slots);
    }

    private boolean isSlotDisponibile(String time, List<Indisponibilita> indispList) {
        try {
            Date slotTime = timeFormat.parse(time);
            for (Indisponibilita indisp : indispList) {
                // Controllo se l'indisponibilità copre l'intera giornata o un orario specifico
                if (selectedDate.equals(indisp.getDataInizio())) {
                    if (indisp.getOraInizio() == null || indisp.getOraFine() == null) {
                        return false; // Indisponibile tutto il giorno
                    }
                    Date indispInizio = timeFormat.parse(indisp.getOraInizio());
                    Date indispFine = timeFormat.parse(indisp.getOraFine());
                    
                    if (!slotTime.before(indispInizio) && slotTime.before(indispFine)) {
                        return false; // Slot all'interno dell'intervallo di indisponibilità
                    }
                }
            }
        } catch (ParseException e) {
            return true;
        }
        return true;
    }

    private void prenota() {
        String motivazione = Objects.requireNonNull(binding.reasonEditText.getText()).toString();
        if (selectedTime == null || motivazione.isEmpty()) {
            Toast.makeText(getContext(), "Seleziona un orario e inserisci la motivazione", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.bookButton.setEnabled(false);

        SharedPreferencesManager pref = new SharedPreferencesManager(requireContext());
        
        Appuntamento app = new Appuntamento();
        app.setIdProfessore(profId);
        app.setIdStudente(pref.getUserUid());
        app.setNomeProfessore(currentProf.getNome() + " " + currentProf.getCognome());
        
        // Recupero nome completo dello studente dalle SharedPreferences
        String nomeStudente = pref.getString("user_full_name", "Studente");
        app.setNomeStudente(nomeStudente);
        
        app.setData(selectedDate);
        app.setOraInizio(selectedTime);
        
        // Calcolo ora fine
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(Objects.requireNonNull(timeFormat.parse(selectedTime)));
            cal.add(Calendar.MINUTE, currentProf.getDurataAppuntamento());
            app.setOraFine(timeFormat.format(cal.getTime()));
        } catch (Exception e) {
            app.setOraFine(selectedTime);
        }

        app.setMotivazione(motivazione);
        app.setStato("sospeso");
        app.setDataCreazione(Timestamp.now());
        app.setEmailStudente(FirebaseAuth.getInstance().getCurrentUser().getEmail());

        new AppuntamentiRepository().creaAppuntamento(app, new AppuntamentiRepository.OnTaskCompleted() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Prenotazione inviata!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.bookButton.setEnabled(true);
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