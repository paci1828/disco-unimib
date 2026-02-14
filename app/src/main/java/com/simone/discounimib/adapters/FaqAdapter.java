package com.simone.discounimib.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.simone.discounimib.databinding.ItemFaqBinding;
import com.simone.discounimib.models.Faq;
import java.util.ArrayList;
import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    private List<Faq> faqList = new ArrayList<>();

    public void setFaqList(List<Faq> faqList) {
        this.faqList = faqList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFaqBinding binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FaqViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        holder.bind(faqList.get(position));
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

    class FaqViewHolder extends RecyclerView.ViewHolder {
        private final ItemFaqBinding binding;

        public FaqViewHolder(ItemFaqBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Faq faq) {
            binding.questionTextView.setText(faq.getDomanda());
            binding.answerTextView.setText(faq.getRisposta());
        }
    }
}