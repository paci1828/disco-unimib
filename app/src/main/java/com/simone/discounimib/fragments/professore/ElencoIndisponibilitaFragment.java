package com.simone.discounimib.fragments.professore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.simone.discounimib.R;
import com.simone.discounimib.adapters.IndisponibilitaAdapter;
import com.simone.discounimib.databinding.FragmentElencoIndisponibilitaBinding;
import com.simone.discounimib.repositories.IndisponibilitaRepository;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.IndisponibilitaViewModel;

public class ElencoIndisponibilitaFragment extends Fragment {

    private FragmentElencoIndisponibilitaBinding binding;
    private IndisponibilitaViewModel viewModel;
    private IndisponibilitaAdapter adapter;
    private SharedPreferencesManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentElencoIndisponibilitaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefManager = new SharedPreferencesManager(requireContext());
        viewModel = new ViewModelProvider(this).get(IndisponibilitaViewModel.class);

        setupRecyclerView();
        observeIndisponibilita();

        binding.addFab.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new AggiungiIndisponibilitaFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupRecyclerView() {
        adapter = new IndisponibilitaAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemLongClickListener(indisponibilita -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Elimina Indisponibilità")
                    .setMessage("Sei sicuro di voler eliminare questa indisponibilità?")
                    .setPositiveButton("Elimina", (dialog, which) -> {
                        viewModel.eliminaIndisponibilita(indisponibilita.getId(), new IndisponibilitaRepository.OnTaskCompleted() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Eliminata", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(getContext(), "Errore: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });
    }

    private void observeIndisponibilita() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getIndisponibilitaProfessore(prefManager.getUserUid()).observe(getViewLifecycleOwner(), list -> {
            binding.progressBar.setVisibility(View.GONE);
            if (list != null && !list.isEmpty()) {
                adapter.setIndisponibilitaList(list);
                binding.emptyTextView.setVisibility(View.GONE);
            } else {
                adapter.setIndisponibilitaList(new java.util.ArrayList<>());
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