package com.kongtech.plutocon.template.alarm;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kongtech.plutocon.sdk.Plutocon;
import com.kongtech.plutocon.sdk.PlutoconManager;
import com.kongtech.plutocon.template.PlutoconListActivity;
import com.kongtech.plutocon.template.view.AttrItemView;

import java.util.List;

public class TemplateFragment extends Fragment implements View.OnClickListener {

    private final int OFFSET_RSSI = -100;

    private AttrItemView aivTargetName;
    private AttrItemView aivTargetAddress;
    private TextView tvRssi;
    private SeekBar sbRssi;


    private Snackbar snackbar;

    private PlutoconManager plutoconManager;
    private Plutocon targetPlutocon;
    private int targetRssi = OFFSET_RSSI;
    private boolean isDiscovered;

    public static Fragment newInstance(Context context) {
        TemplateFragment f = new TemplateFragment();
        return f;
    }

    public void startMonitoring(){

        View view = getView().findViewById(R.id.background_scale);

        AnimationSet animationSet = createAnimation();

        view.setAnimation(null);
        view.startAnimation(animationSet);

        plutoconManager.startMonitoring(PlutoconManager.MONITORING_FOREGROUND, new PlutoconManager.OnMonitoringPlutoconListener() {
            @Override
            public void onPlutoconDiscovered(Plutocon plutocon, List<Plutocon> plutocons) {
                if(plutocon.getMacAddress().equals(targetPlutocon.getMacAddress())
                        && plutocon.getRssi() > targetRssi
                        && !isDiscovered){

                    isDiscovered = true;
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(300);

                    snackbar = Snackbar.make(getView().findViewById(R.id.root), "Plutocon recognized", Snackbar.LENGTH_INDEFINITE);
                    snackbar.getView().setBackgroundColor(getResources().getColor(R.color.appleGreen));
                    snackbar.setActionTextColor(Color.WHITE);
                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isDiscovered = false;
                        }
                    });
                    snackbar.show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(targetPlutocon != null){
            this.startMonitoring();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        plutoconManager.stopMonitoring();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        plutoconManager.close();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_template, null);

        aivTargetName = (AttrItemView)view.findViewById(R.id.aivTargetName);
        aivTargetAddress = (AttrItemView)view.findViewById(R.id.aivTargetAddress);

        aivTargetName.setOnClickListener(this);

        tvRssi = (TextView) view.findViewById(R.id.tvRSSI);
        sbRssi = (SeekBar) view.findViewById(R.id.sbRSSI);
        sbRssi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                targetRssi = OFFSET_RSSI + progress;
                tvRssi.setText(targetRssi + "dBm");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return view;
    }

    @Override
    public void onStart() {
        plutoconManager = new PlutoconManager(this.getContext());
        plutoconManager.connectService(null);
        super.onStart();
    }

    @Override
    public void onClick(View v) {
        if(checkPermission())
            startActivityForResult(new Intent(getActivity(), PlutoconListActivity.class), 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 0:
                if(resultCode == 1) {
                    targetPlutocon = (Plutocon)data.getParcelableExtra("PLUTOCON");
                    aivTargetName.setValue(targetPlutocon.getName());
                    aivTargetAddress.setValue(targetPlutocon.getMacAddress());

                    this.startMonitoring();
                }
            break;
        }
    }

    private AnimationSet createAnimation() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(0.2f, 1, 0.2f, 1, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setStartOffset(300);
        scaleAnimation.setDuration(900);
        scaleAnimation.setRepeatCount(Animation.INFINITE);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
        alphaAnimation.setStartOffset(300);
        alphaAnimation.setDuration(900);
        alphaAnimation.setRepeatCount(Animation.INFINITE);


        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animationSet.setFillAfter(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(alphaAnimation);

        return animationSet;
    }

    private boolean checkPermission(){
        BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if((mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())){
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return false;
        }


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            }

            LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ex) {}

            if(!gps_enabled){
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return false;
            }
        }
        return true;
    }
}
