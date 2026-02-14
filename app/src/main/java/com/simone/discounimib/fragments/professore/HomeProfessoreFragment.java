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
import com.simone.discounimib.adapters.AppuntamentiAdapter;
import com.simone.discounimib.databinding.FragmentHomeProfessoreBinding;
import com.simone.discounimib.utils.SharedPreferencesManager;
import com.simone.discounimib.viewmodels.AppuntamentiViewModel;

public class HomeProfessoreFragment extends Fragment {

    private FragmentHomeProfessoreBinding binding;
    private AppuntamentiViewModel viewModel;
    private AppuntamentiAdapter adapter;
    private SharedPreferencesManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeProfessoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefManager = new SharedPreferencesManager(requireContext());
        viewModel = new ViewModelProvider(this).get(AppuntamentiViewModel.class);
        
        setupRecyclerView();
        setupRefresh();
        observeAppuntamenti();

        // Mostriamo il nome nel titolo se desiderato, o usiamo quello della HomeActivity
        String fullName = prefManager.getString("user_full_name", "Professore");
        binding.titleTextView.setText("Appuntamenti di " + fullName);
    }

    private void setupRecyclerView() {
        adapter = new AppuntamentiAdapter();
        binding.appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.appointmentsRecyclerView.setAdapter(adapter);
    }

    private void setupRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::observeAppuntamenti);
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    private void observeAppuntamenti() {
        if (!binding.swipeRefreshLayout.isRefreshing()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        
        String uid = prefManager.getUserUid();
        
        viewModel.getAppuntamentiProfessore(uid, "accettato").observe(getViewLifecycleOwner(), appuntamenti -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.swipeRefreshLayout.setRefreshing(false);

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
        binding = null;
    }
}