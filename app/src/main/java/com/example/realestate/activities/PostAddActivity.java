package com.example.realestate.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.util.Map;

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
    /* Array Adapter to set to AutoCompleteTextView, so user can
       select subcategory base on category */
    private ArrayAdapter<String> adapterPropertySubcategory;
    /* Image Uri to hold uri of the image (picked/captured using
       Gallery/Camera) to add in Ad Images List */
    private Uri imageUri = null;


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

        // handle pickImagesTv click, show images add options (Gallery/Camera)
        binding.pickImagesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickOptions();
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

    private void showImagePickOptions() {
        Log.d(TAG, "showImagePickOptions: ");

        /* Init the PopupMenu, param 1 is context and param 2 is Anchor view for
           this popup. The popup will appear below the anchor if there is room or
           above it if there is not */
        PopupMenu popupMenu = new PopupMenu(this, binding.pickImagesTv);

        /* Add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID,
           Param#3 is OrderID, Param#4 is Menu Item Title */
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");

        // Show Popup Menu
        popupMenu.show();

        // handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // get the id of the item clicked in popup menu
                int itemId = item.getItemId();

                /* check which item id is clicked from popup menu. 1 => Camera,
                   2 => Gallery as we defined */
                if (itemId == 1) {
                    /* camera is clicked we need to check if we have permission of
                       Camera/Storage before launching Camera to Capture image */
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Device version is TIRAMISU (SDK 33) or above. We only need Camera permission
                        String[] permissions =  new String[]{android.Manifest.permission.CAMERA};
                        requestCameraPermission.launch(permissions);
                    } else {
                        // Device version is below TIRAMISU. We need Camera & Storage permissions
                        String[] permissions =  new String[]{
                                android.Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        };
                        requestCameraPermission.launch(permissions);
                    }
                } else if (itemId == 2) {
                    /* gallery is clicked we need to check if we have permission of
                       Storage before launching Gallery to Pick image */
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        /* Device version is TIRAMISU or above. We don't need Storage
                           permission to launch Gallery */
                        pickImageGallery();
                    } else {
                        /* Device version is TIRAMISU. We need Storage permission
                           to launch Gallery */
                        String storagePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        requestStoragePermission.launch(storagePermission);
                    }

                }

                return false;
            }
        });
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");

                    // Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // get data from result param
                        Intent data = result.getData();
                        // get uri of image picked
                        imageUri = data.getData();

                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                    } else {
                        // cancelled
                        MyUtils.toast(PostAddActivity.this, "Cancelled!");
                    }
                }
            }
    );

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: " + isGranted);

                    // Let's check if permission is granted or not
                    if (isGranted) {
                        // Storage Permission granted, we can now launch gallery to pick image

                    } else {
                        // Storage Permission granted, we can't launch gallery to pick image
                        MyUtils.toast(PostAddActivity.this, "Storage permission denied!");
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: " + result.toString());

                    // Check if all permissions are granted
                    boolean areAllGranted = true; // Iterate through the result values to check each permission
                    for (Boolean isGranted : result.values()) {
                        // Check if any permission is not granted
                        areAllGranted = areAllGranted && isGranted;
                    }

                    if (areAllGranted) {
                        // All Permission Camera, Storage are granted, we can now launch camera to capture image
                        pickImageCamera();
                    } else {
                        /* Camera or Storage or Both permission are denied, can't launch camera capture image
                           Log.d(TAG, "onActivityResult: All or either one permission denied..."); */
                        MyUtils.toast(PostAddActivity.this, "Camera or Storage or Both permissions denied!");
                    }
                }
            }
    );
    
    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_DESCRIPTION");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");

                    // Check if image is captured or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // no need to get image uri here we already got it in pickImageCamera() function
                        Log.d(TAG, "onActivityResult: ");
                    } else {
                        // Cancelled
                        MyUtils.toast(PostAddActivity.this, "Cancelled!");
                    }
                }
            }
    );
}