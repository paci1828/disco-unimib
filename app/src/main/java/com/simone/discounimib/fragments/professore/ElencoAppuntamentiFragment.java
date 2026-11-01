package com.simone.discounimib.fragments.professore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.simone.discounimib.adapters.AppuntamentiAdapter;
import com.simone.discounimib.R;
import com.simone.discounimib.fragments.professore.AccettaRifiutaAppuntamentoFragment;
import com.simone.discounimib.databinding.FragmentElencoAppuntamentiBinding;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.AppuntamentiViewModel;

public class ElencoAppuntamentiFragment extends Fragment {

    private FragmentElencoAppuntamentiBinding binding;
    private AppuntamentiViewModel viewModel;
    private AppuntamentiAdapter adapter;
    private SharedPreferencesManager prefManager;
    private androidx.lifecycle.LiveData<java.util.List<com.simone.discounimib.models.Appuntamento>> sorgente;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentElencoAppuntamentiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefManager = new SharedPreferencesManager(requireContext());
        viewModel = new ViewModelProvider(this).get(AppuntamentiViewModel.class);
        
        setupRecyclerView();
        setupTabs();
        observeAppuntamenti();
    }

    private void setupRecyclerView() {
        adapter = new AppuntamentiAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(appuntamento -> getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, AccettaRifiutaAppuntamentoFragment.newInstance(appuntamento.getId()))
                .addToBackStack(null).commit());
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                observeAppuntamenti();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeAppuntamenti() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = prefManager.getUserUid();
        
        if (sorgente != null) sorgente.removeObservers(getViewLifecycleOwner());
        sorgente = viewModel.getAppuntamentiProfessore(uid, null);
        sorgente.observe(getViewLifecycleOwner(), appuntamenti -> {
            binding.progressBar.setVisibility(View.GONE);
            if (appuntamenti != null) {
                boolean passati = binding.tabLayout.getSelectedTabPosition() == 1;
                java.time.LocalDateTime adesso = java.time.LocalDateTime.now(
                        com.simone.discounimib.domain.CalcolatoreDisponibilita.FUSO);
                appuntamenti = appuntamenti.stream().filter(a ->
                        passati != com.simone.discounimib.domain.RegoleAppuntamento.futuro(
                                a.getData(), a.getOraInizio(), adesso)).collect(java.util.stream.Collectors.toList());
            }
            if (appuntamenti != null && !appuntamenti.isEmpty()) {
                adapter.setAppuntamenti(appuntamenti);
                binding.emptyTextView.setVisibility(View.GONE);
            } else {
                adapter.setAppuntamenti(new java.util.ArrayList<>());
                binding.emptyTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        sorgente = null;
        binding = null;
    }
}