package com.fit.realestate.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fit.realestate.R;
import com.fit.realestate.databinding.ActivityPropertyDetailBinding;
import com.google.firebase.auth.FirebaseAuth;

public class PropertyDetailActivity extends AppCompatActivity {
    private ActivityPropertyDetailBinding binding;
    private static final String TAG = "PROPERTY_DETAIL";
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private Context mContext;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPropertyDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init/setup ProgressDialog to show while account verification
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        loadMyProperty();
    }

    private void loadMyProperty() {

    }
}