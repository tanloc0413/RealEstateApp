package com.example.realestate.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.realestate.R;
import com.example.realestate.adapters.AdapterProperty;
import com.example.realestate.databinding.FragmentFavoriteListBinding;
import com.example.realestate.models.ModelProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FavoriteListFragment extends Fragment {
    private FragmentFavoriteListBinding binding;
    private static final String TAG = "FAVORITE_LIST_TAG";
    private Context mContext;
    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelProperty> propertyArrayList;
    private AdapterProperty adapterProperty;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public FavoriteListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentFavoriteListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();

        loadFavoriteProperties();
    }

    private void loadFavoriteProperties() {
        Log.d(TAG, "loadFavoriteProperties: ");

        propertyArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        propertyArrayList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String propertyId = "" + ds.child("propertyId").getValue();

                            DatabaseReference propertyRef = FirebaseDatabase.getInstance().getReference("Properties");
                            propertyRef.child(propertyId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            try {
                                                ModelProperty modelProperty = snapshot.getValue(ModelProperty.class);
                                                propertyArrayList.add(modelProperty);
                                            } catch (Exception e) {
                                                Log.e(TAG, "onDataChange: ", e);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                adapterProperty = new AdapterProperty(mContext, propertyArrayList);
                                binding.favoriteRv.setAdapter(adapterProperty);
                            }
                        }, 1000);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }


}