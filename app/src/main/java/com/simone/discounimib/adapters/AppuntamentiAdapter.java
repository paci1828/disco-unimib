package com.simone.discounimib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.simone.discounimib.databinding.ItemAppuntamentoBinding;
import com.simone.discounimib.models.Appuntamento;
import java.util.ArrayList;
import java.util.List;

public class AppuntamentiAdapter extends RecyclerView.Adapter<AppuntamentiAdapter.AppuntamentoViewHolder> {

    private List<Appuntamento> appuntamenti = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Appuntamento appuntamento);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setAppuntamenti(List<Appuntamento> appuntamenti) {
        this.appuntamenti = appuntamenti;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppuntamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppuntamentoBinding binding = ItemAppuntamentoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AppuntamentoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppuntamentoViewHolder holder, int position) {
        holder.bind(appuntamenti.get(position));
    }

    @Override
    public int getItemCount() {
        return appuntamenti.size();
    }

    class AppuntamentoViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppuntamentoBinding binding;

        public AppuntamentoViewHolder(ItemAppuntamentoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(appuntamenti.get(position));
                }
            });
        }

        public void bind(Appuntamento appuntamento) {
            binding.dataTextView.setText(appuntamento.getData());
            binding.oraTextView.setText(appuntamento.getOraInizio() + " - " + appuntamento.getOraFine());
            
            // In base a chi visualizza, mostriamo il nome del professore o dello studente
            // Qui mettiamo una logica semplice, poi si può affinare
            if (appuntamento.getNomeStudente() != null) {
                binding.nomeUtenteTextView.setText(appuntamento.getNomeStudente());
            } else {
                binding.nomeUtenteTextView.setText(appuntamento.getNomeProfessore());
            }
            
            binding.motivazioneTextView.setText(appuntamento.getMotivazione());
            binding.statoTextView.setText(appuntamento.getStato());
            
            // Cambia colore in base allo stato
            updateStatoColor(appuntamento.getStato());
        }

        private void updateStatoColor(String stato) {
            if (stato == null) return;
            switch (stato.toLowerCase()) {
                case "accettato":
                    binding.statoTextView.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                    break;
                case "rifiutato":
                    binding.statoTextView.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                    break;
                case "sospeso":
                default:
                    binding.statoTextView.setBackgroundColor(itemView.getContext().getColor(android.R.color.darker_gray));
                    break;
            }
        }
    }
}