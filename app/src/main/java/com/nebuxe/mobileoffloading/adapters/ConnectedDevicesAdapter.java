package com.nebuxe.mobileoffloading.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nebuxe.mobileoffloading.R;
import com.nebuxe.mobileoffloading.pojos.ConnectedDevice;
import com.nebuxe.mobileoffloading.utilities.Constants;

import java.util.List;

public class ConnectedDevicesAdapter extends RecyclerView.Adapter<ConnectedDevicesAdapter.ViewHolder> {

    private Context context;
    private List<ConnectedDevice> connectedDevices;

    public ConnectedDevicesAdapter(@NonNull Context context, List<ConnectedDevice> connectedDevices) {
        this.context = context;
        this.connectedDevices = connectedDevices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.item_connected_device, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setClientId(connectedDevices.get(position).getEndpointId(), connectedDevices.get(position).getEndpointName());
        holder.setBatteryLevel(connectedDevices.get(position).getDeviceStats().getBatteryLevel());
        holder.setRequestStatus(connectedDevices.get(position).getRequestStatus());
    }

    @Override
    public int getItemCount() {
        return connectedDevices.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvClientId;
        private TextView tvBatteryLevel;
        private TextView tvRequestStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientId = itemView.findViewById(R.id.tv_client_id);
            tvBatteryLevel = itemView.findViewById(R.id.tv_battery_level);
            tvRequestStatus = itemView.findViewById(R.id.tv_request_status);
        }

        public void setClientId(String endpointId, String endpointName) {
            this.tvClientId.setText(endpointId + " (" + endpointName + ")");
        }

        public void setBatteryLevel(int batteryLevel) {
            if (batteryLevel > 0 && batteryLevel <= 100) {
                this.tvBatteryLevel.setText(batteryLevel + "%");
            } else {
                this.tvBatteryLevel.setText("- %");
            }
        }

        public void setRequestStatus(String requestStatus) {
            if (requestStatus.equals(Constants.RequestStatus.ACCEPTED)) {
                this.tvRequestStatus.setText("(request accepted)");
            } else if (requestStatus.equals(Constants.RequestStatus.REJECTED)) {
                this.tvRequestStatus.setText("(request rejected)");
            } else {
                this.tvRequestStatus.setText("(request pending)");
            }
        }
    }
}
