package edu.asu.mobile_offloading_master;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ItemList extends RecyclerView.Adapter<ItemList.SingleItemHolder> {

    private ArrayList<SecondaryDataModel> secondaryDataModelArrayList;
    public ItemList(ArrayList<SecondaryDataModel> secondaryDataModelArrayList) {
        this.secondaryDataModelArrayList = secondaryDataModelArrayList;
    }

    @NonNull
    @Override
    public SingleItemHolder onCreateViewHolder(@NonNull ViewGroup p, int type) {
        LayoutInflater inflater = LayoutInflater.from(p.getContext());
        View view = inflater.inflate(R.layout.item_list_layout,p,false);
        return new SingleItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SingleItemHolder holder, int position) {
        SecondaryDataModel secondaryDataModel =  secondaryDataModelArrayList.get(position);
        holder.secondaryName.setText(secondaryDataModel.getSecondaryId());
        holder.remainingPower.setText(secondaryDataModel.getRemainingPower()+" %");
        holder.coordinates.setText("( "+ secondaryDataModel.getLatitudeCoord()+" , "+secondaryDataModel.getLongitudeCoord()+" )");
    }

    @Override
    public int getItemCount() {
        return secondaryDataModelArrayList.size();
    }

    public class SingleItemHolder extends RecyclerView.ViewHolder{
        TextView secondaryName;
        TextView remainingPower;
        TextView coordinates;
        public SingleItemHolder(@NonNull View it) {
            super(it);
            secondaryName = it.findViewById(R.id.secTV);
            remainingPower = it.findViewById(R.id.pwLevel);
            coordinates = it.findViewById(R.id.lcTv);
        }
    }
}
