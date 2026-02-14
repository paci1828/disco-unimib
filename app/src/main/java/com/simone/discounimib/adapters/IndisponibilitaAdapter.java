package com.simone.discounimib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.simone.discounimib.databinding.ItemIndisponibilitaBinding;
import com.simone.discounimib.models.Indisponibilita;
import java.util.ArrayList;
import java.util.List;

public class IndisponibilitaAdapter extends RecyclerView.Adapter<IndisponibilitaAdapter.IndisponibilitaViewHolder> {

    private List<Indisponibilita> indisponibilitaList = new ArrayList<>();
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(Indisponibilita indisponibilita);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setIndisponibilitaList(List<Indisponibilita> list) {
        this.indisponibilitaList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IndisponibilitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIndisponibilitaBinding binding = ItemIndisponibilitaBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new IndisponibilitaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IndisponibilitaViewHolder holder, int position) {
        holder.bind(indisponibilitaList.get(position));
    }

    @Override
    public int getItemCount() {
        return indisponibilitaList.size();
    }

    class IndisponibilitaViewHolder extends RecyclerView.ViewHolder {
        private final ItemIndisponibilitaBinding binding;

        public IndisponibilitaViewHolder(ItemIndisponibilitaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(indisponibilitaList.get(position));
                    return true;
                }
                return false;
            });
        }

        public void bind(Indisponibilita indisponibilita) {
            binding.dateRangeTextView.setText(indisponibilita.getDataInizio() + " - " + indisponibilita.getDataFine());
            
            if (indisponibilita.getOraInizio() != null && !indisponibilita.getOraInizio().isEmpty()) {
                binding.timeRangeTextView.setText(indisponibilita.getOraInizio() + " - " + indisponibilita.getOraFine());
            } else {
                binding.timeRangeTextView.setText("Tutto il giorno");
            }
            
            binding.motivoTextView.setText(indisponibilita.getMotivo());
        }
    }
}