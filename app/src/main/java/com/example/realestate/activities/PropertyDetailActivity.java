package com.example.realestate.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.R;
import com.example.realestate.databinding.ActivityPropertyDetailBinding;

public class PropertyDetailActivity extends AppCompatActivity {
    // view binding
    private ActivityPropertyDetailBinding binding;
    //
    private String title, description, category, subcategory, address;
    //
    private static final String TAG = "PROPERTY_DETAIL_TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}