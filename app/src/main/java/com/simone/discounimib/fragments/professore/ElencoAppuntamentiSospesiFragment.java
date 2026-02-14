package com.simone.discounimib.fragments.professore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.simone.discounimib.R;
import com.simone.discounimib.adapters.AppuntamentiAdapter;
import com.simone.discounimib.databinding.FragmentAppuntamentiSospesiBinding;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.AppuntamentiViewModel;

public class ElencoAppuntamentiSospesiFragment extends Fragment {

    private FragmentAppuntamentiSospesiBinding binding;
    private AppuntamentiViewModel viewModel;
    private AppuntamentiAdapter adapter;
    private SharedPreferencesManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppuntamentiSospesiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefManager = new SharedPreferencesManager(requireContext());
        viewModel = new ViewModelProvider(this).get(AppuntamentiViewModel.class);
        
        setupRecyclerView();
        observeAppuntamenti();
    }

    private void setupRecyclerView() {
        adapter = new AppuntamentiAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(appuntamento -> {
            replaceFragment(AccettaRifiutaAppuntamentoFragment.newInstance(appuntamento.getId()));
        });
    }

    private void observeAppuntamenti() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = prefManager.getUserUid();
        
        viewModel.getAppuntamentiProfessore(uid, "sospeso").observe(getViewLifecycleOwner(), appuntamenti -> {
            binding.progressBar.setVisibility(View.GONE);
            if (appuntamenti != null && !appuntamenti.isEmpty()) {
                adapter.setAppuntamenti(appuntamenti);
                binding.emptyTextView.setVisibility(View.GONE);
            } else {
                adapter.setAppuntamenti(new java.util.ArrayList<>());
                binding.emptyTextView.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        // Sostituito con l'ID corretto trovato nel layout activity_home.xml
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}