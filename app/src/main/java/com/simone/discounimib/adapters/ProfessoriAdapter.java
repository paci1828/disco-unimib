package com.simone.discounimib.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.simone.discounimib.databinding.ItemProfessoreBinding;
import com.simone.discounimib.models.Professore;
import java.util.ArrayList;
import java.util.List;

public class ProfessoriAdapter extends RecyclerView.Adapter<ProfessoriAdapter.ProfessoreViewHolder> {

    private List<Professore> professoriList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Professore professore);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setProfessoriList(List<Professore> list) {
        this.professoriList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfessoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfessoreBinding binding = ItemProfessoreBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProfessoreViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfessoreViewHolder holder, int position) {
        holder.bind(professoriList.get(position));
    }

    @Override
    public int getItemCount() {
        return professoriList.size();
    }

    class ProfessoreViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfessoreBinding binding;

        public ProfessoreViewHolder(ItemProfessoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(professoriList.get(position));
                }
            });
        }

        public void bind(Professore professore) {
            binding.profNameTextView.setText(professore.getNome() + " " + professore.getCognome());
            binding.profEmailTextView.setText(professore.getEmail());
            
            String ricevimento = "Ricevimento: " + professore.getGiornoRicevimento() + " " + 
                               professore.getOraInizio() + " - " + professore.getOraFine();
            binding.ricevimentoTextView.setText(ricevimento);
        }
    }
}