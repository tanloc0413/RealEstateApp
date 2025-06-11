package com.fit.realestate.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.fit.realestate.R;
import com.fit.realestate.adapters.AdapterProperty;
import com.fit.realestate.databinding.ActivityMyPropertyListBinding;
import com.fit.realestate.models.ModelProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MyPropertyListActivity extends AppCompatActivity {
    // view binding
    private ActivityMyPropertyListBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "MY_PROFILE_LIST_TAG";
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelProperty> propertyArrayList;
    private AdapterProperty adapterProperty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init view binding... activity_my_property_list.xml = ActivityMyPropertyListBinding
        binding = ActivityMyPropertyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        loadMyProperties();

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

        // handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void loadMyProperties() {
        propertyArrayList = new ArrayList<>();
        String myUid = "" + firebaseAuth.getUid();
        Log.d(TAG, "loadMyProperties: " + myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        propertyArrayList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            try {
                                ModelProperty modelProperty = ds.getValue(ModelProperty.class);

                                propertyArrayList.add(modelProperty);
                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        adapterProperty = new AdapterProperty(MyPropertyListActivity.this, propertyArrayList);
                        binding.propertiesRv.setAdapter(adapterProperty);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}