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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.realestate.AdapterImagePicked;
import com.example.realestate.models.ModelImagePicked;
import com.example.realestate.MyUtils;
import com.example.realestate.databinding.ActivityPostAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
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
    /* Image Uri to hold uri of the image (picked/captured using
       Gallery/Camera) to add in Ad Images List */
    private Uri imageUri = null;
    /* Array Adapter to set to AutoCompleteTextView, so user can
       select subcategory base on category */
    private ArrayAdapter<String> adapterPropertySubcategory;
    // List of images (picked/captured using Gallery/Camera or from Internet)
    private ArrayList<ModelImagePicked> imagePickedArrayList;
    // Adapter to show images picked/taken from Gallery/Camera or from Internet
    private AdapterImagePicked adapterImagePicked;
    private String category = MyUtils.propertyTypes[0];
    private String purpose = MyUtils.PROPERTY_PURPOSE_SELL;
    private String subcategory = "", floors = "", bedRooms = "", bathRooms = "";
    private String areaSize = "", areaSizeUnit = "", price = "", title = "";
    private String description = "", email = "", phoneCode = "", phoneNumber = "";
    private String country = "", city = "", address = "", state = "";
    private double latitude = 0, longitude = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init view binding... activity_post_add.xml = ActivityPostAddBinding
        binding = ActivityPostAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        // init/setup ProgressDialog to show while adding/updating the Ad
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...!");
        progressDialog.setCanceledOnTouchOutside(false);

        /* setup and set the property area size unit adapter to the
           Property Area Unit Filed i.e areaSizeUnitAct */
        ArrayAdapter<String> adapterAreaSize = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, MyUtils.propertyAreaSizeUnit
        );
        binding.areaSizeUnitAct.setAdapter(adapterAreaSize);

        // init imagePickedArrayList
        imagePickedArrayList = new ArrayList<>();
        loadImages();
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

        // handle locationAct click, launch LocationPickerActivity to pick location from MAP
        binding.locationAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PostAddActivity.this, LocationPickerActivity.class);
                locationPickerActivityResultLauncher.launch(intent);
            }
        });

        // handle submitBtn click, validate data and upload
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private ActivityResultLauncher<Intent> locationPickerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: result: " + result);

                    // get result of location picked from LocationPickerActivity
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();

                        if (data != null) {
                            latitude = data.getDoubleExtra("latitude", 0);
                            longitude = data.getDoubleExtra("longitude", 0);
                            address = data.getStringExtra("address");
                            city = data.getStringExtra("city");
                            country = data.getStringExtra("country");
                            state = data.getStringExtra("state");

                            Log.d(TAG, "onActivityResult: latitude" + latitude);
                            Log.d(TAG, "onActivityResult: longitude" + longitude);
                            Log.d(TAG, "onActivityResult: address" + address);
                            Log.d(TAG, "onActivityResult: city" + city);
                            Log.d(TAG, "onActivityResult: country" + country);
                            Log.d(TAG, "onActivityResult: state" + state);

                            binding.locationAct.setText(address);
                        }
                    }
                }
            }
    );

    private void validateData() {
        Log.d(TAG, "validateData: ");

        // input data
        subcategory = binding.propertySubcategoryAct.getText().toString().trim();
        floors = binding.floorsEt.getText().toString().trim();
        bedRooms = binding.bedRoomEt.getText().toString().trim();
        bathRooms = binding.bathRoomEt.getText().toString().trim();
        areaSize = binding.areaSizeEt.getText().toString().trim();
        areaSizeUnit = binding.areaSizeUnitAct.getText().toString().trim();
        address = binding.locationAct.getText().toString().trim();
        price = binding.priceEt.getText().toString().trim();
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();

        // validate data
        if (subcategory.isEmpty()) {
            /* no property subcategory selected in propertySubcategoryAct, show
               error in propertySubcategoryAct and focus */
            binding.propertySubcategoryAct.setError("Choose Subcategory");
            binding.propertySubcategoryAct.requestFocus();
        } else if (category.equals(MyUtils.propertyTypes[0]) && floors.isEmpty()) {
            /* Property Type is Home, No floors count entered in floorEt, show
               error in floorEt and focus */
            binding.floorsEt.setError("Enter Floors Count...!");
            binding.floorsEt.requestFocus();
        } else if (category.equals(MyUtils.propertyTypes[0]) && bedRooms.isEmpty()) {
            /* Property Type is Home, No floors count entered in bedRoomEt, show
               error in bedRoomEt and focus */
            binding.bedRoomEt.setError("Enter Bedrooms Count...!");
            binding.bedRoomEt.requestFocus();
        } else if (category.equals(MyUtils.propertyTypes[0]) && bathRooms.isEmpty()) {
            /* Property Type is Home, No floors count entered in bathRoomEt, show
               error in bathRoomEt and focus */
            binding.bathRoomEt.setError("Enter Bathrooms Count...!");
            binding.bathRoomEt.requestFocus();
        } else if (areaSize.isEmpty()) {
            // no area size entered in areaSizeEt, show error in areaSizeEt and focus
            binding.areaSizeEt.setError("Enter Area Size...!");
            binding.areaSizeEt.requestFocus();
        } else if (areaSizeUnit.isEmpty()) {
            // no area size entered in areaSizeUnitAct, show error in areaSizeUnitAct and focus
            binding.areaSizeUnitAct.setError("Choose Area Size Unit...!");
            binding.areaSizeUnitAct.requestFocus();
        } else if (address.isEmpty()) {
            /* no address selected in locationAct (need to pick from map), show error
               in locationAct and focus */
            binding.locationAct.setError("Pick Location...!");
            binding.locationAct.requestFocus();
        } else if (price.isEmpty()) {
            // no price entered in priceEt, show error in priceEt and focus
            binding.priceEt.setError("Enter Price...!");
            binding.priceEt.requestFocus();
        } else if (title.isEmpty()) {
            // no title entered in titleEt, show error in titleEt and focus
            binding.titleEt.setError("Enter Title...!");
            binding.titleEt.requestFocus();
        } else if (description.isEmpty()) {
            // no description entered in descriptionEt, show error in descriptionEt and focus
            binding.descriptionEt.setError("Enter Description...!");
            binding.descriptionEt.requestFocus();
        } else if (phoneNumber.isEmpty()) {
            // no phone number entered in phoneNumberEt, show error in phoneNumberEt and focus
            binding.phoneNumberEt.setError("Enter Phone Number...!");
            binding.phoneNumberEt.requestFocus();
        } else if (imagePickedArrayList.isEmpty()) {
            // no image selected/picked
            MyUtils.toast(this, "Pick at-least one image...!");
        } else {
            // all data is validated, we can proceed further now
            postAd();
        }

    }

    private void postAd() {
        Log.d(TAG, "postAd: ");

        // show progress
        progressDialog.setMessage("Publishing Ad");
        progressDialog.show();

        // if floors is empty init with "0"
        if (floors.isEmpty()) {
            floors = "0";
        }

        // if bedRooms is empty init with "0"
        if (bedRooms.isEmpty()) {
            bedRooms = "0";
        }

        // if bathRooms is empty init with "0"
        if (bathRooms.isEmpty()) {
            bathRooms = "0";
        }

        // get current timestamp
        long timestamp = MyUtils.timestamp();
        // firebase database Properties reference to store new Properties
        DatabaseReference refProperties = FirebaseDatabase.getInstance().getReference("Properties");
        // key id from the reference to use as Ad id
        String keyId = refProperties.push().getKey();

        // setup data to add in firebase database
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", "" + keyId);
        hashMap.put("uid", "" + firebaseAuth.getUid());
        hashMap.put("purpose", "" + purpose);
        hashMap.put("category", "" + category);
        hashMap.put("subcategory", "" + subcategory);
        hashMap.put("areaSize", Double.parseDouble(areaSize));
        hashMap.put("areaSizeUnit", "" + areaSizeUnit);
        hashMap.put("title", "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("email", "" + email);
        hashMap.put("phoneCode", "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("country", "" + country);
        hashMap.put("city", "" + city);
        hashMap.put("state", "" + state);
        hashMap.put("address", "" + address);
        hashMap.put("status", "" + MyUtils.AD_STATUS_AVAILABLE);
        hashMap.put("floors", Long.parseLong(floors));
        hashMap.put("bedRooms", Long.parseLong(bedRooms));
        hashMap.put("bathRooms", Long.parseLong(bathRooms));
        hashMap.put("price", Double.parseDouble(price));
        hashMap.put("timestamp", timestamp);
        hashMap.put("latitude", latitude);
        hashMap.put("longitude", longitude);

        // set data to firebase database, Properties -> PropertyId -> PropertyDataJSON
        refProperties.child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Ad Published");

                        uploadImagesStorage(keyId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(
                                PostAddActivity.this,
                                "Failed to publish due to " + e.getMessage()
                        );
                    }
                });
    }

    private void uploadImagesStorage(String propertyId) {
        Log.d(TAG, "uploadImagesStorage: propertyId: " + propertyId);

        for (int i = 0; i < imagePickedArrayList.size(); i++) {
            ModelImagePicked modelImagePicked = imagePickedArrayList.get(i);

            if (!modelImagePicked.isFromInternet()) {
                String imageName = modelImagePicked.getId();
                String filePathAndName = "Properties/" + imageName;
                int imageIndexForProgress = i + 1;
                Uri pickedImageUri = modelImagePicked.getImageUri();

                StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
                storageReference.putFile(pickedImageUri)
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                                String message = "Uploading" + imageIndexForProgress
                                        + " of " + imagePickedArrayList.size()
                                        + " images... \nProgress " + (int)progress + "%";
                                Log.d(TAG, "onProgress: message: " + message);

                                progressDialog.setMessage(message);
                                progressDialog.show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Log.d(TAG, "onSuccess: ");

                                // image uploaded get url of uploaded image
                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful());
                                Uri uploadedImageUrl = uriTask.getResult();

                                if (uriTask.isSuccessful()) {
                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    hashMap.put("id", "" + modelImagePicked.getId());
                                    hashMap.put("uid", "" + uploadedImageUrl);

                                    DatabaseReference refProperties = FirebaseDatabase.getInstance().getReference("Properties");
                                    refProperties.child(propertyId).child("Images")
                                            .child(imageName).updateChildren(hashMap);
                                }

                                progressDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: ", e);

                                MyUtils.toast(
                                        PostAddActivity.this,
                                        "Failed to uploaded due to " + e.getMessage()
                                );

                                progressDialog.dismiss();
                            }
                        });

            }
        }
    }

    private void loadImages() {
        Log.d(TAG, "loadImages: ");

        adapterImagePicked = new AdapterImagePicked(this, imagePickedArrayList);
        // set adapter to imageRv
        binding.imagesRv.setAdapter(adapterImagePicked);
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

                        // timestamp will be used as id of the image picked
                        String timestamp = "" + MyUtils.timestamp();

                        /* setup model for image. Param 1 is id, Param 2 is imageUri,
                           Param 3 is imageUrl, from Internet */
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false);
                        // add model to the imagePickedArrayList
                        imagePickedArrayList.add(modelImagePicked);
                        // reload the images
                        loadImages();

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

                        // timestamp will be used as id of the image picked
                        String timestamp = "" + MyUtils.timestamp();

                        /* setup model for image. Param 1 is id, Param 2 is imageUri,
                           Param 3 is imageUrl, from Internet */
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false);
                        // add model to the imagePickedArrayList
                        imagePickedArrayList.add(modelImagePicked);
                        // reload the images
                        loadImages();
                    } else {
                        // Cancelled
                        MyUtils.toast(PostAddActivity.this, "Cancelled!");
                    }
                }
            }
    );
}