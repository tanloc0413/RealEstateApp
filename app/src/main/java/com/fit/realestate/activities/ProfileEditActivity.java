package com.fit.realestate.activities;

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
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.fit.realestate.BirthdateInputHandler;
import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.databinding.ActivityProfileEditBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {
    // view binding
    private ActivityProfileEditBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "PROFILE_EDIT_TAG";
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // ProgressDialog to show while verify account
    private ProgressDialog progressDialog;
    // Hold user type e.g Email/Google/Phone
    private String myUserType = "";
    // Image Uri to hold the uri of image picked from Camera/Gallery
    private Uri imageUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_profile_edit.xml = ActivityProfileEditBinding
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        // init/setup ProgressDialog to show while updating account
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Xin vui lòng đợi!");
        progressDialog.setCanceledOnTouchOutside(false);

        loadMyInfo();

        // handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // handle profileImagePickFab click, show image pick popup menu
        binding.profileImagePickFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePickDialog();
            }
        });

        // handle updateBtn click, validate data
        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

        // Áp dụng định dạng ngày tháng cho dobEt
        BirthdateInputHandler.setupDateInput(binding.dobEt);

        // Áp dụng tính năng xóa văn bản bằng cách nhấn giữ cho dobEt
        BirthdateInputHandler.setupLongDelete(binding.dobEt);
    }

    private String name = "", dob = "", email = "", phoneCode = "", phoneNumber = "";

    private void validateData() {
        // input data
        name = binding.nameEt.getText().toString().trim();
        dob = binding.dobEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.countryCodePicker.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();

        // validate data
        if (imageUri == null) {
            // no image to upload to storage, just update db
            updateProfileDb(null);
        } else {
            // image need to upload to storage, first upload image the update db
            uploadProfileImageStorage();
        }
    }

    private void uploadProfileImageStorage() {
        Log.d(TAG, "uploadProfileImageStorage: ");

        // show progress
        progressDialog.setMessage("Đang tải hình ảnh lên hồ sơ người dùng...");
        progressDialog.show();

        String filePathAndName = "UserImages/" + firebaseAuth.getUid();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putFile(imageUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        // progress from 0 to 100
                        double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        Log.d(TAG, "onSuccess: " + progress);

                        // show progress to progress dialog
                        progressDialog.setMessage("Đang tải ảnh lên hồ sơ. Tiến trình: " + (int)progress + "");
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Image uploaded successfully
                        Log.d(TAG, "onSuccess: Đã tải lên");
                        // To get url of upload image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        // Get url of uploaded image
                        while(!uriTask.isSuccessful());
                        String uploadedImageUrl = uriTask.getResult().toString();

                        // if we get successfully then upload to db
                        if(uriTask.isSuccessful()) {
                            updateProfileDb(uploadedImageUrl);

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to upload image
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ProfileEditActivity.this, "Không tải được ảnh hồ sơ do " + e.getMessage());
                    }
                });
    }

    private void updateProfileDb(String imageUrl) {
        // show progress
        progressDialog.setMessage("Cập nhật thông tin người dùng...");
        progressDialog.show();

        // setup data in hashmap to update to firebase db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", "" + name);
        hashMap.put("dob", "" + dob);

        if (imageUrl != null) {
            // update profileImageUrl in db only if uploaded image url is not null
            hashMap.put("profileImageUrl", "" + imageUrl);
        }

        /* if user type is Phone then allow to update email otherwise
           (in case of Google or Email) allow to update Phone */
        if (myUserType.equals(MyUtils.USER_TYPE_EMAIL) || myUserType.equals(MyUtils.USER_TYPE_GOOGLE)) {
            // user type is google/email, allow to update phone number not email
            hashMap.put("phoneCode", "" + phoneCode);
            hashMap.put("phoneNumber", "" + phoneNumber);
        } else if (myUserType.equals(MyUtils.USER_TYPE_PHONE)) {
            // user type is phone, allow to update email, not phone number
            hashMap.put("email", "" + email);
        }

        // Database reference of user to update info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // updated successfully
                        Log.d(TAG, "onSuccess: Thông tin đã cập nhật");
                        progressDialog.dismiss();
                        MyUtils.toast(ProfileEditActivity.this, "Đã cập nhật hồ sơ...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // failed to update
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ProfileEditActivity.this, "Không cập nhật được do " + e.getMessage());
                    }
                });
    }

    private void imagePickDialog() {
        /* Init the PopupMenu, param 1 is context and param 2 is the UI View (profileImagePickFab)
           to above/below we need to show popup menu */
        PopupMenu popupMenu = new PopupMenu(this, binding.profileImagePickFab);
        /* Add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID,
           Param#3 is OrderID, Param#4 is Menu Item Title */
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Thư viện");
        // Show Popup Menu
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Get the id of the menu item clicked
                int itemId = item.getItemId();

                // Check which menu item is clicked based on itemId we got
                if (itemId == 1) {
                    /* Camera is clicked we need to check if we have permission of
                       Camera, Storage before launching Camera to Capture image */
                    Log.d(TAG, "onMenuItemClick: ");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Device version is TIRAMISU (SDK 33) or above. We only need Camera permission
                        requestCameraPermission.launch(new String[] {
                                android.Manifest.permission.CAMERA
                        });
                    } else {
                        // Device version is below TIRAMISU. We need Camera & Storage permissions
                        requestCameraPermission.launch(new String[] {
                                android.Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        });
                    }

                } else if (itemId == 2) {
                    // Gallery is clicked, we don't need any permission to launch Gallery
                    Log.d(TAG, "onMenuItemClick: Đã nhấn vào thư viện...");
                    pickImageGallery();
                }

                return false;
            }
        });

    }

    private final ActivityResultLauncher<String[]> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: " + result.toString());
                    // Check if all permissions are granted
                    boolean areAllGranted = true; // Iterate through the result values to check each permission
                    for (Boolean isGranted : result.values()) {
                        // Update the flag indicating whether all permissions are granted
                        areAllGranted = areAllGranted && isGranted;
                    }

                    if (areAllGranted) {
                        // All Permission Camera, Storage are granted, we can now launch camera to capture image
                        pickImageCamera();
                    } else {
                        // Camera or Storage or Both permission are denied, can't launch camera capture image
                        Log.d(TAG, "onActivityResult: Tất cả hoặc một trong hai quyền bị từ chối...");
                        MyUtils.toast(ProfileEditActivity.this, "Quyền Camera hoặc Lưu trữ hoặc cả hai đều bị từ chối...");
                    }
                }
            }
    );

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");

        // Intent to launch Image Picker e.g Gallery
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Image Picked from Gallery, get data/intent from ActivityResult
                        Intent data = result.getData();
                        // get uri of image picked
                        imageUri = data.getData();

                        Log.d(TAG, "onActivityResult: Hình ảnh được chọn từ Thư viện: " + imageUri);

                        try {
                            // set to profileIv
                            try {
                                Glide.with(ProfileEditActivity.this)
                                        .load(imageUri)
                                        .placeholder(R.drawable.person_black)
                                        .into(binding.profileIv);
                            } catch (Exception e) {
                                Log.e(TAG, "onActivityResult: ", e);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "onActivityResult: ", e);
                        }
                    } else {
                        // cancelled
                        Log.d(TAG, "onActivityResult: Đã hủy...");
                        MyUtils.toast(ProfileEditActivity.this, "Đã hủy...!");
                    }
                }
            }
    );

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "temp_images");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "temp_images_description");
        // store the camera image in imageUri variable
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        // Intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // check if image is captured or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: Hình ảnh đã chọn: " + imageUri);

                        // set to profileIv
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.person_black)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onActivityResult: ", e);
                        }
                    } else {
                        // cancelled
                        Log.d(TAG, "onActivityResult: Đã hủy...");
                        MyUtils.toast(ProfileEditActivity.this, "Đã hủy...");
                    }
                }
            }
    );

    private void loadMyInfo() {
        Log.d(TAG, "loadMyInfo: ");

        // format birthday input
        BirthdateInputHandler.setupDateInput(binding.dobEt);

        // Reference of current user info in Firebase Realtime Database to get user info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // get user info, spellings should be same as in firebase realtime database
                        String dob = "" + snapshot.child("dob").getValue();
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String phoneCode = "" + snapshot.child("phoneCode").getValue();
                        String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                        String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        myUserType = "" + snapshot.child("userType").getValue();

                        // concatenate phone code and phone number to make full phone number
//                        String phone = phoneCode + phoneNumber;
                        String phone = phoneNumber;

                        /* check user type, if Email/Google then don't allow user to edit/update
                           email. If Phone then don't allow user to edit phone number */
                        if (myUserType.equals(MyUtils.USER_TYPE_EMAIL) || myUserType.equals(MyUtils.USER_TYPE_GOOGLE)) {
                            // user type is Email or Google, don't allow to edit email
                            binding.emailTil.setEnabled(false);
                            binding.emailEt.setEnabled(false);
                        } else {
                            // user type is Phone, don't allow to edit phone
                            binding.phoneNumberTil.setEnabled(false);
                            binding.phoneNumberEt.setEnabled(false);
                            binding.countryCodePicker.setEnabled(false);
                        }

                        // set data to UI
                        binding.emailEt.setText(email);
                        binding.dobEt.setText(dob);
                        binding.nameEt.setText(name);
                        binding.fullNameTv.setText(name);
                        binding.phoneNumberEt.setText(phone);

                        try {
                            int phoneCodeInt = Integer.parseInt(phoneCode.replace("+", ""));
                            binding.countryCodePicker.setCountryForPhoneCode(phoneCodeInt);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }

                        // set profile image to profileIv
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_black)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}