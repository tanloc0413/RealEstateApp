package com.fit.realestate.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.adapters.AdapterImagePicked;
import com.fit.realestate.databinding.ActivityPropertyDetailBinding;
import com.fit.realestate.databinding.ActivityPropertyEditBinding;
import com.fit.realestate.models.ModelImagePicked;
import com.fit.realestate.models.ModelProperty;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

public class PropertyEditActivity extends AppCompatActivity {
    private ActivityPropertyEditBinding binding;
    private static final String TAG = "PROPERTY_DETAIL_EDIT";
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private Context mContext;

    private String propertyId = "";
    private String currentPropertyUid = "";

    // Arrays for dropdown options
    private String[] homeSubcategories = {"Căn hộ", "Nhà phố", "Biệt thự", "Nhà mặt tiền"};
    private String[] plotSubcategories = {"Đất nền", "Đất thổ cư", "Đất công nghiệp", "Đất nông nghiệp"};
    private String[] commercialSubcategories = {"Cửa hàng", "Văn phòng", "Nhà kho", "Khách sạn"};
    private String[] areaSizeUnits = {"m²", "ha", "acre"};

    // Variables for form data
    private String selectedPurpose = "Đăng bán";
    private String selectedCategory = "Nhà ở";
    private String selectedSubcategory = "";
    private String selectedAreaSizeUnit = "m²";

    // Images
    private ArrayList<ModelImagePicked> imagePickedArrayList;
    private AdapterImagePicked adapterImagesPicked;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPropertyEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init/setup ProgressDialog to show while account verification
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        mContext = this;

        // Get property ID from intent
        propertyId = getIntent().getStringExtra("propertyId");
        if (TextUtils.isEmpty(propertyId)) {
            Toast.makeText(this, "Không tìm thấy ID bất động sản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init/setup ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Setup UI components
        setupUI();
        loadPropertyData();

        // Back button
        binding.toolbarBackBtn.setOnClickListener(v -> finish());

        // Submit button
        binding.submitBtn.setOnClickListener(v -> validateAndUpdateProperty());

        // Pick images
        binding.pickImagesTv.setOnClickListener(v -> {
            // Implement image picker functionality here
            Toast.makeText(this, "Image picker functionality", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupUI() {
        // Setup images RecyclerView
        imagePickedArrayList = new ArrayList<>();
        adapterImagesPicked = new AdapterImagePicked(this, imagePickedArrayList);
        binding.imagesRv.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        binding.imagesRv.setAdapter(adapterImagesPicked);

        // Setup purpose radio group
        binding.purposeRg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.purposeSellRb) {
                selectedPurpose = "Đăng bán";
            } else if (checkedId == R.id.purposeRentRb) {
                selectedPurpose = "Cho thuê";
            }
        });

        // Setup category tabs
        setupCategoryTabs();

        // Setup area size unit dropdown
        setupAreaSizeUnitDropdown();
    }

    private void setupCategoryTabs() {
        binding.propertyCategoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        selectedCategory = "Nhà ở";
                        setupSubcategoryDropdown(homeSubcategories);
                        break;
                    case 1:
                        selectedCategory = "Lô đất";
                        setupSubcategoryDropdown(plotSubcategories);
                        break;
                    case 2:
                        selectedCategory = "TMDV";
                        setupSubcategoryDropdown(commercialSubcategories);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set default selection
        binding.propertyCategoryTabLayout.getTabAt(0).select();
        setupSubcategoryDropdown(homeSubcategories);
    }

    private void setupSubcategoryDropdown(String[] subcategories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subcategories);
        binding.propertySubcategoryAct.setAdapter(adapter);
        binding.propertySubcategoryAct.setOnItemClickListener((parent, view, position, id) -> {
            selectedSubcategory = subcategories[position];
        });
    }

    private void setupAreaSizeUnitDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, areaSizeUnits);
        binding.areaSizeUnitAct.setAdapter(adapter);
        binding.areaSizeUnitAct.setOnItemClickListener((parent, view, position, id) -> {
            selectedAreaSizeUnit = areaSizeUnits[position];
        });
        // Set default
        binding.areaSizeUnitAct.setText(selectedAreaSizeUnit, false);
    }

    private void loadPropertyData() {
        Log.d(TAG, "loadPropertyData: Loading property with ID: " + propertyId);

        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();

                if (!snapshot.exists()) {
                    Toast.makeText(PropertyEditActivity.this, "Không tìm thấy bất động sản", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Get property data
                String title = "" + snapshot.child("title").getValue();
                String address = "" + snapshot.child("address").getValue();
                Double price = snapshot.child("price").getValue(Double.class);
                String purpose = "" + snapshot.child("purpose").getValue();
                String category = "" + snapshot.child("category").getValue();
                String subcategory = "" + snapshot.child("subcategory").getValue();
                String floors = "" + snapshot.child("floors").getValue();
                String bedrooms = "" + snapshot.child("bedRooms").getValue();
                String bathrooms = "" + snapshot.child("bathRooms").getValue();
                String areaSize = "" + snapshot.child("areaSize").getValue();
                String areaSizeUnit = "" + snapshot.child("areaSizeUnit").getValue();
                String uid = "" + snapshot.child("uid").getValue();
                String description = "" + snapshot.child("description").getValue();

                currentPropertyUid = uid;

                // Check if current user is the owner
                if (firebaseAuth.getCurrentUser() == null ||
                        !firebaseAuth.getUid().equals(currentPropertyUid)) {
                    Toast.makeText(PropertyEditActivity.this, "Bạn không có quyền chỉnh sửa bài đăng này", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Populate form with existing data
                populateForm(title, address, price, purpose, category, subcategory,
                        floors, bedrooms, bathrooms, areaSize, areaSizeUnit, description);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e(TAG, "loadPropertyData: Error loading property", error.toException());
                Toast.makeText(PropertyEditActivity.this, "Lỗi khi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateForm(String title, String address, Double price, String purpose,
                              String category, String subcategory, String floors, String bedrooms,
                              String bathrooms, String areaSize, String areaSizeUnit, String description) {

        // Set basic info
        binding.titleEt.setText(title);
        binding.locationAct.setText(address);
        binding.priceEt.setText(price != null ? String.valueOf(price) : "");
        binding.descriptionEt.setText(description);

        // Set purpose
        selectedPurpose = purpose;
        if ("Đăng bán".equals(purpose)) {
            binding.purposeSellRb.setChecked(true);
        } else {
            binding.purposeRentRb.setChecked(true);
        }

        // Set category and subcategory
        selectedCategory = category;
        selectedSubcategory = subcategory;

        int categoryIndex = 0;
        if ("Nhà ở".equals(category)) {
            categoryIndex = 0;
            setupSubcategoryDropdown(homeSubcategories);
        } else if ("Lô đất".equals(category)) {
            categoryIndex = 1;
            setupSubcategoryDropdown(plotSubcategories);
        } else if ("TMDV".equals(category)) {
            categoryIndex = 2;
            setupSubcategoryDropdown(commercialSubcategories);
        }

        binding.propertyCategoryTabLayout.getTabAt(categoryIndex).select();
        binding.propertySubcategoryAct.setText(subcategory, false);

        // Set property details
        binding.floorsEt.setText(floors);
        binding.bedRoomEt.setText(bedrooms);
        binding.bathRoomEt.setText(bathrooms);
        binding.areaSizeEt.setText(areaSize);

        // Set area size unit
        selectedAreaSizeUnit = areaSizeUnit;
        binding.areaSizeUnitAct.setText(areaSizeUnit, false);
    }

    private void validateAndUpdateProperty() {
        Log.d(TAG, "validateAndUpdateProperty: Starting validation");

        // Get input values
        String title = binding.titleEt.getText().toString().trim();
        String address = binding.locationAct.getText().toString().trim();
        String priceStr = binding.priceEt.getText().toString().trim();
        String floors = binding.floorsEt.getText().toString().trim();
        String bedrooms = binding.bedRoomEt.getText().toString().trim();
        String bathrooms = binding.bathRoomEt.getText().toString().trim();
        String areaSize = binding.areaSizeEt.getText().toString().trim();
        String description = binding.descriptionEt.getText().toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(title)) {
            binding.titleTil.setError("Vui lòng nhập tiêu đề");
            binding.titleEt.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            binding.locationTil.setError("Vui lòng nhập địa chỉ");
            binding.locationAct.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            binding.priceTil.setError("Vui lòng nhập giá");
            binding.priceEt.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedSubcategory)) {
            Toast.makeText(this, "Vui lòng chọn loại bất động sản", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse price
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            binding.priceTil.setError("Giá không hợp lệ");
            binding.priceEt.requestFocus();
            return;
        }

        // Clear errors
        binding.titleTil.setError(null);
        binding.locationTil.setError(null);
        binding.priceTil.setError(null);

        // Update property
        updateProperty(title, address, price, floors, bedrooms, bathrooms, areaSize, description);
    }

    private void updateProperty(String title, String address, double price, String floors,
                                String bedrooms, String bathrooms, String areaSize, String description) {

        Log.d(TAG, "updateProperty: Updating property with ID: " + propertyId);

        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.show();

        // Create update map
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("address", address);
        updates.put("price", price);
        updates.put("purpose", selectedPurpose);
        updates.put("category", selectedCategory);
        updates.put("subcategory", selectedSubcategory);
        updates.put("floors", floors);
        updates.put("bedRooms", bedrooms);
        updates.put("bathRooms", bathrooms);
        updates.put("areaSize", areaSize);
        updates.put("areaSizeUnit", selectedAreaSizeUnit);
        updates.put("description", description);
        updates.put("timestamp", System.currentTimeMillis());

        // Update in Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateProperty: Property updated successfully");
                    progressDialog.dismiss();
                    MyUtils.toast(PropertyEditActivity.this, "Đã cập nhật thành công!");

                    // Return to previous activity
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateProperty: Failed to update property", e);
                    progressDialog.dismiss();
                    MyUtils.toast(PropertyEditActivity.this, "Lỗi khi cập nhật: " + e.getMessage());
                });
    }
}