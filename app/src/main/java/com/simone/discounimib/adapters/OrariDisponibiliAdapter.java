package com.simone.discounimib.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.simone.discounimib.databinding.ItemSlotOrarioBinding;
import java.util.ArrayList;
import java.util.List;

public class OrariDisponibiliAdapter extends RecyclerView.Adapter<OrariDisponibiliAdapter.SlotViewHolder> {

    private List<String> slots = new ArrayList<>();
    private int selectedPosition = -1;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String time);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSlots(List<String> slots) {
        this.slots = slots;
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSlotOrarioBinding binding = ItemSlotOrarioBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SlotViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        holder.bind(slots.get(position), position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    class SlotViewHolder extends RecyclerView.ViewHolder {
        private final ItemSlotOrarioBinding binding;

        public SlotViewHolder(ItemSlotOrarioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.slotChip.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int previousSelected = selectedPosition;
                    selectedPosition = position;
                    notifyItemChanged(previousSelected);
                    notifyItemChanged(selectedPosition);
                    
                    if (listener != null) {
                        listener.onItemClick(slots.get(position));
                    }
                }
            });
        }

        public void bind(String time, boolean isSelected) {
            binding.slotChip.setText(time);
            binding.slotChip.setChecked(isSelected);
        }
    }
}