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
import com.google.android.material.tabs.TabLayout;
import com.simone.discounimib.adapters.AppuntamentiAdapter;
import com.simone.discounimib.databinding.FragmentElencoAppuntamentiStudenteBinding;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.AppuntamentiViewModel;
import java.util.stream.Collectors;

public class ElencoAppuntamentiStudenteFragment extends Fragment {

    private FragmentElencoAppuntamentiStudenteBinding binding;
    private AppuntamentiViewModel viewModel;
    private AppuntamentiAdapter adapter;
    private SharedPreferencesManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentElencoAppuntamentiStudenteBinding.inflate(inflater, container, false);
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
        int selectedTab = binding.tabLayout.getSelectedTabPosition();

        viewModel.getAppuntamentiStudente(uid).observe(getViewLifecycleOwner(), appuntamenti -> {
            binding.progressBar.setVisibility(View.GONE);
            if (appuntamenti != null) {
                java.util.List<com.simone.discounimib.models.Appuntamento> filteredList;
                if (selectedTab == 1) { // Sospesi
                    filteredList = appuntamenti.stream()
                            .filter(a -> "sospeso".equalsIgnoreCase(a.getStato()))
                            .collect(Collectors.toList());
                } else if (selectedTab == 2) { // Accettati
                    filteredList = appuntamenti.stream()
                            .filter(a -> "accettato".equalsIgnoreCase(a.getStato()))
                            .collect(Collectors.toList());
                } else { // Tutti
                    filteredList = appuntamenti;
                }

                if (filteredList.isEmpty()) {
                    binding.emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyTextView.setVisibility(View.GONE);
                }
                adapter.setAppuntamenti(filteredList);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}