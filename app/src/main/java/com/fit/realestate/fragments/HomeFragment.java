package com.fit.realestate.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.activities.LocationPickerActivity;
import com.fit.realestate.adapters.AdapterProperty;
import com.fit.realestate.databinding.FragmentHomeBinding;
import com.fit.realestate.models.ModelProperty;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private static final String TAG = "HOME_TAG";
    private Context mContext;
    private ArrayList<ModelProperty> propertyArrayList;
    private AdapterProperty adapterProperty;
    private SharedPreferences locationSp;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentAddress = "";
    private String currentCity = "";

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationSp = mContext.getSharedPreferences("LOCATION_SP", MODE_PRIVATE);
        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0f);
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0f);
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "");
        currentCity = locationSp.getString("CURRENT_CITY", "");

//        if (!currentCity.isEmpty() && currentCity != null) {
//            binding.cityTv.setText(currentCity);
//        }

        if (currentCity != null && !currentCity.isEmpty()) {
            binding.cityTv.setText(currentCity);
        } else {
            currentLatitude = 0.0;
            currentLongitude = 0.0;
            binding.cityTv.setText("Tất cả");
        }

        loadProperties();

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                Log.d(TAG, "onTextChanged: " + s);

                try {
                    String query = s.toString();
                    adapterProperty.getFilter().filter(query);
                } catch (Exception e) {
                    Log.e(TAG, "onTextChanged: ", e);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.cityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, LocationPickerActivity.class);
                locationActivityResultLauncher.launch(intent);
            }
        });
    }

    private ActivityResultLauncher<Intent> locationActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();

                if (data != null) {
                    currentLatitude = data.getDoubleExtra("latitude", 0.0);
                    currentLongitude = data.getDoubleExtra("longitude", 0.0);
                    currentAddress = data.getStringExtra("address");
                    currentCity = data.getStringExtra("city");

                    locationSp.edit()
                            .putFloat("CURRENT_LATITUDE", Float.parseFloat("" + currentLatitude))
                            .putFloat("CURRENT_LONGITUDE", Float.parseFloat("" + currentLongitude))
                            .putString("CURRENT_ADDRESS", currentAddress)
                            .putString("CURRENT_CITY", currentCity)
                            .apply();

                    binding.cityTv.setText(currentCity);
                    loadProperties();
                }
            } else {
                Log.d(TAG, "onActivityResult: Đã hủy");
                MyUtils.toast(mContext, "Đã thoát!");
            }
        }
    });

    private void loadProperties() {
        Log.d(TAG, "loadProperties: ");

        propertyArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                propertyArrayList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelProperty modelProperty = ds.getValue(ModelProperty.class);

                    try {
                        double propertyLatitude = modelProperty.getLatitude();
                        double propertyLongitude = modelProperty.getLongitude();

                        double distance = MyUtils.caculateDistanceKm(
                                currentLatitude, currentLongitude,
                                propertyLatitude, propertyLongitude
                        );
                        Log.d(TAG, "onDataChange: Khoảng cách: " + distance + "Km");

//                        if (distance <= MyUtils.MAX_DISTANCE_TO_LOAD_PROPERTIES) {
//                            propertyArrayList.add(modelProperty);
//                        }

                        if (binding.cityTv.getText().toString().equals("Tất cả")) {
                            propertyArrayList.add(modelProperty); // Hiển thị tất cả
                        } else {
                            if (distance <= MyUtils.MAX_DISTANCE_TO_LOAD_PROPERTIES) {
                                propertyArrayList.add(modelProperty); // Lọc theo vị trí
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

                adapterProperty = new AdapterProperty(mContext, propertyArrayList);
                binding.propertiesRv.setAdapter(adapterProperty);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}