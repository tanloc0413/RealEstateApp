package com.example.realestate.activities;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityPostAddBinding;
import com.example.realestate.databinding.ActivityProfileEditBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

public class PostAddActivity extends AppCompatActivity {
    // view binding
    private ActivityPostAddBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "POST_ADD_TAG";
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // ProgressDialog to show while verify account
    private ProgressDialog progressDialog;
    //
    private String category = MyUtils.propertyTypes[0];
    private String purpose = MyUtils.PROPERTY_PURPOSE_SELL;
    // Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
    private ArrayAdapter<String> adapterPropertySubcategory;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init view binding... activity_post_add.xml = ActivityPostAddBinding
        binding = ActivityPostAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* setup and set the property area size unit adapter to the
           Property Area Unit Filed i.e areaSizeUnitAct */
        ArrayAdapter<String> adapterAreaSize = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, MyUtils.propertyAreaSizeUnit
        );
        binding.areaSizeUnitAct.setAdapter(adapterAreaSize);



        propertyCategoryHomes();

        // handle propertyCategoryTabLayout change listener, Choose Category
        binding.propertyCategoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // get selected category
                int position = tab.getPosition();
                if (position == 0) {
                    // Home Tab clicked: Prepare adapter with categories related to Homes
                    category = MyUtils.propertyTypes[0];
                    propertyCategoryHomes();
                } else if (position == 1) {
                    // Plot Tab clicked: Prepare adapter with categories related to Plots
                    category = MyUtils.propertyTypes[1];
                    propertyCategoryPlots();
                } else if (position == 2) {
                    // Commercial Tab clicked: Prepare adapter with categories related to Commercial
                    category = MyUtils.propertyTypes[2];
                    propertyCategoryCommercial();
                }

                Log.d(TAG, "onTabSelected: category: " + category);

                binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);


            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // set a listener for RadioGroup
        binding.purposeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                RadioButton selectedRadioButton = findViewById(checkedId);
                purpose = selectedRadioButton.getText().toString();
                // show in logs
                Log.d(TAG, "onCheckedChanged: purpose: " + purpose);

            }
        });
    }

    private void propertyCategoryHomes() {
        // In case of category Homes we will show
        binding.floorsTil.setVisibility(View.VISIBLE);
        binding.bedRoomTil.setVisibility(View.VISIBLE);
        binding.bathRoomTil.setVisibility(View.VISIBLE);

        /* Array Adapter to set to AutoCompleteTextView, so user can select
           subcategory base on category */
        adapterPropertySubcategory = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesHomes
        );
        // set adapter to propertySubcategoryAct
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
        // category changed, reset subcategory
        binding.propertySubcategoryAct.setText("");
    }

    private void propertyCategoryPlots() {
        // In case of category Plots we will hide
        binding.floorsTil.setVisibility(View.GONE);
        binding.bedRoomTil.setVisibility(View.GONE);
        binding.bathRoomTil.setVisibility(View.GONE);

        /* Array Adapter to set to AutoCompleteTextView, so user can select
           subcategory base on category */
        adapterPropertySubcategory = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesPlots
        );
        // set adapter to propertySubcategoryAct
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
        // category changed, reset subcategory
        binding.propertySubcategoryAct.setText("");
    }

    private void propertyCategoryCommercial() {
        // In case of category Commercial we will show/hide
        binding.floorsTil.setVisibility(View.VISIBLE);
        binding.bedRoomTil.setVisibility(View.GONE);
        binding.bathRoomTil.setVisibility(View.GONE);

        /* Array Adapter to set to AutoCompleteTextView, so user can select
           subcategory base on category */
        adapterPropertySubcategory = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesCommercial
        );
        // set adapter to propertySubcategoryAct
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
        // category changed, reset subcategory
        binding.propertySubcategoryAct.setText("");
    }
}