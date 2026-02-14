package com.simone.discounimib.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.simone.discounimib.adapters.FaqAdapter;
import com.simone.discounimib.databinding.FragmentFaqBinding;
import com.simone.discounimib.models.Faq;
import java.util.ArrayList;
import java.util.List;

public class FaqFragment extends Fragment {

    private FragmentFaqBinding binding;
    private FaqAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFaqBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        adapter = new FaqAdapter();
        binding.faqRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.faqRecyclerView.setAdapter(adapter);

        loadFaqs();
    }

    private void loadFaqs() {
        List<Faq> faqs = new ArrayList<>();
        faqs.add(new Faq("1", "Come prenotare?", "Vai nella sezione 'Prenota' e scegli un professore.", 1));
        faqs.add(new Faq("2", "Come annullare?", "Vai nei 'Miei appuntamenti' e tieni premuto sull'appuntamento.", 2));
        adapter.setFaqList(faqs);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}