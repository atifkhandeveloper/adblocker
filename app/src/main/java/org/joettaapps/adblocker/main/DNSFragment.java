package org.joettaapps.adblocker.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joettaapps.adblocker.Configuration;
import org.joettaapps.adblocker.FileHelper;
import org.joettaapps.adblocker.ItemChangedListener;
import org.joettaapps.adblocker.MainActivity;
import org.joettaapps.adblocker.R;

public class DNSFragment extends Fragment implements FloatingActionButtonFragment {

    private ItemRecyclerViewAdapter mAdapter;

    public DNSFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getActivity() != null) {
            getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        View rootView = inflater.inflate(R.layout.fragment_dns, container, false);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.dns_entries);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new ItemRecyclerViewAdapter(MainActivity.config.dnsServers.items, 2);
        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(mAdapter));
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        Switch dnsEnabled = rootView.findViewById(R.id.dns_enabled);
        dnsEnabled.setChecked(MainActivity.config.dnsServers.enabled);
        dnsEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.config.dnsServers.enabled = isChecked;
                FileHelper.writeSettings(getContext(), MainActivity.config);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity)getActivity()).showInterstitialAd();
                }
            }
        });

        ExtraBar.setup(rootView.findViewById(R.id.extra_bar), "dns");

        return rootView;
    }

    @Override
    public void setupFloatingActionButton(FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity main = (MainActivity) getActivity();
                if (main == null) return;

                main.editItem(2, null, new ItemChangedListener() {
                    @Override
                    public void onItemChanged(Configuration.Item item) {
                        MainActivity.config.dnsServers.items.add(item);
                        if (mAdapter != null) {
                            mAdapter.notifyItemInserted(mAdapter.getItemCount() - 1);
                        }
                        FileHelper.writeSettings(getContext(), MainActivity.config);
                        main.showInterstitialAd();
                    }
                });
            }
        });
    }

    // ------------------ PUBLIC METHODS ------------------

    /** Update the DNS switch from outside (MainActivity) */
    public void updateDnsEnabled(boolean enabled) {
        if (getView() != null) {
            Switch dnsEnabled = getView().findViewById(R.id.dns_enabled);
            if (dnsEnabled != null) {
                dnsEnabled.setChecked(enabled);
            }
        }
    }

    /** Refresh the RecyclerView from outside (MainActivity) */
    public void refreshRecyclerView() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
