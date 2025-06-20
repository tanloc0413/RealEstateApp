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
import android.widget.ArrayAdapter;

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
    private ArrayList<ModelProperty> allPropertiesList;
    private AdapterProperty adapterProperty;
    private SharedPreferences locationSp;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentAddress = "";
    private String currentCity = "";

    // Filter variables
    private String selectedPurpose = ""; // Mua/Thuê
    private String selectedCategory = "";
    private String selectedSubcategory = "";
    private double minPrice = 0.0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean isFilterVisible = false;

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

        setupFilterDropdowns();
        setupFilterListeners();
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

        binding.filterRv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFilterVisibility();
            }
        });

        View filterLayout = binding.getRoot().findViewById(R.id.fillterLayout);
        View cancelFilter = filterLayout.findViewById(R.id.cancelFillter);

        cancelFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterLayout.setVisibility(View.GONE);
            }
        });

        binding.fillterLayout.cancelFillter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFilter();
            }
        });
    }


    private void setupFilterDropdowns() {
        // Setup category dropdown using data from MyUtils
        String[] categories = new String[MyUtils.propertyTypes.length + 1];
        categories[0] = "Tất cả";
        System.arraycopy(MyUtils.propertyTypes, 0, categories, 1, MyUtils.propertyTypes.length);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_dropdown_item_1line, categories);
        binding.fillterLayout.propertyCategoryAct.setAdapter(categoryAdapter);

        // Setup subcategory dropdown (will be updated based on category selection)
        updateSubcategoryDropdown("Tất cả");

        // Category selection listener
        binding.fillterLayout.propertyCategoryAct.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCat = categories[position];
            updateSubcategoryDropdown(selectedCat);
        });
    }

    private void updateSubcategoryDropdown(String category) {
        String[] subcategories;

        switch (category) {
            case "Nhà ở":
                // Add "Tất cả" at the beginning of propertyTypesHomes array
                subcategories = new String[MyUtils.propertyTypesHomes.length + 1];
                subcategories[0] = "Tất cả";
                System.arraycopy(MyUtils.propertyTypesHomes, 0, subcategories, 1, MyUtils.propertyTypesHomes.length);
                break;
            case "Lô đất":
                // Add "Tất cả" at the beginning of propertyTypesPlots array
                subcategories = new String[MyUtils.propertyTypesPlots.length + 1];
                subcategories[0] = "Tất cả";
                System.arraycopy(MyUtils.propertyTypesPlots, 0, subcategories, 1, MyUtils.propertyTypesPlots.length);
                break;
            case "Thương mại - Dịch vụ":
                // Add "Tất cả" at the beginning of propertyTypesCommercial array
                subcategories = new String[MyUtils.propertyTypesCommercial.length + 1];
                subcategories[0] = "Tất cả";
                System.arraycopy(MyUtils.propertyTypesCommercial, 0, subcategories, 1, MyUtils.propertyTypesCommercial.length);
                break;
            default:
                subcategories = new String[]{"Tất cả"};
                break;
        }

        ArrayAdapter<String> subcategoryAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_dropdown_item_1line, subcategories);
        binding.fillterLayout.propertySubcategoryAct.setAdapter(subcategoryAdapter);

        // Clear the subcategory selection when category changes
        binding.fillterLayout.propertySubcategoryAct.setText("", false);
    }

    private void setupFilterListeners() {
        // Tab listeners (Mua/Thuê)
        binding.fillterLayout.tabBuyTv.setOnClickListener(v -> {
            Log.d(TAG, "Buy tab clicked");
            selectPurposeTab("Mua");
        });

        binding.fillterLayout.tabRentTv.setOnClickListener(v -> {
            Log.d(TAG, "Rent tab clicked");
            selectPurposeTab("Thuê");
        });

        // Reset filter
        binding.fillterLayout.resetBtn.setOnClickListener(v -> {
            Log.d(TAG, "Reset filter clicked");
            resetFilter();
        });

        // Apply filter
        binding.fillterLayout.applyBtn.setOnClickListener(v -> {
            Log.d(TAG, "Apply filter clicked");
            debugFilterValues();
            applyFilter();
        });
    }

    private void debugFilterValues() {
        Log.d(TAG, "=== FILTER VALUES DEBUG ===");
        Log.d(TAG, "selectedPurpose: '" + selectedPurpose + "'");
        Log.d(TAG, "selectedCategory: '" + binding.fillterLayout.propertyCategoryAct.getText().toString() + "'");
        Log.d(TAG, "selectedSubcategory: '" + binding.fillterLayout.propertySubcategoryAct.getText().toString() + "'");
        Log.d(TAG, "minPrice: '" + binding.fillterLayout.priceMinEt.getText().toString() + "'");
        Log.d(TAG, "maxPrice: '" + binding.fillterLayout.priceMaxEt.getText().toString() + "'");
        Log.d(TAG, "=== END FILTER DEBUG ===");
    }

    private void selectPurposeTab(String purpose) {
        Log.d(TAG, "selectPurposeTab: Selecting purpose = " + purpose);

        if (purpose.equals("Mua")) {
            binding.fillterLayout.tabBuyTv.setBackgroundResource(R.drawable.shape_rounded_white);
            binding.fillterLayout.tabBuyTv.setTextColor(getResources().getColor(R.color.colorPrimary));
            binding.fillterLayout.tabRentTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            binding.fillterLayout.tabRentTv.setTextColor(getResources().getColor(R.color.black));
        } else {
            binding.fillterLayout.tabRentTv.setBackgroundResource(R.drawable.shape_rounded_white);
            binding.fillterLayout.tabRentTv.setTextColor(getResources().getColor(R.color.colorPrimary));
            binding.fillterLayout.tabBuyTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            binding.fillterLayout.tabBuyTv.setTextColor(getResources().getColor(R.color.black));
        }
        selectedPurpose = purpose;
        Log.d(TAG, "selectPurposeTab: selectedPurpose updated to '" + selectedPurpose + "'");
    }

    private void toggleFilterVisibility() {
        Log.d(TAG, "toggleFilterVisibility: current state = " + isFilterVisible);
        if (isFilterVisible) {
            hideFilter();
        } else {
            showFilter();
        }
    }

    private void showFilter() {
        Log.d(TAG, "showFilter: showing filter layout");
        binding.fillterLayout.getRoot().setVisibility(View.VISIBLE);
        isFilterVisible = true;
    }

    private void hideFilter() {
        Log.d(TAG, "hideFilter: hiding filter layout");
        binding.fillterLayout.getRoot().setVisibility(View.GONE);
        isFilterVisible = false;
    }

    private void resetFilter() {
        Log.d(TAG, "resetFilter: Resetting all filters");

        // Reset all filter values
        selectedPurpose = "";
        selectedCategory = "";
        selectedSubcategory = "";
        minPrice = 0.0;
        maxPrice = Double.MAX_VALUE;

        // Reset UI
        selectPurposeTab("Mua"); // Default to "Mua"
        binding.fillterLayout.propertyCategoryAct.setText("", false);
        binding.fillterLayout.propertySubcategoryAct.setText("", false);
        binding.fillterLayout.priceMinEt.setText("0");
        binding.fillterLayout.priceMaxEt.setText("0");

        // Update filter display
        binding.filterSelectedTv.setText("Hiển thị tất cả");

        // Reset to show all properties
        propertyArrayList.clear();
        propertyArrayList.addAll(allPropertiesList);

        if (adapterProperty != null) {
            adapterProperty.notifyDataSetChanged();
            adapterProperty.updateFilterList(allPropertiesList);
        }

        Log.d(TAG, "resetFilter: Reset complete, showing " + propertyArrayList.size() + " properties");
    }

    private void applyFilter() {
        Log.d(TAG, "applyFilter: Starting filter application");

        // Validation - đảm bảo có dữ liệu để filter
        if (allPropertiesList == null || allPropertiesList.isEmpty()) {
            Log.w(TAG, "applyFilter: No properties to filter");
            MyUtils.toast(mContext, "Không có dữ liệu để lọc");
            return;
        }

        // Get filter values
        selectedCategory = binding.fillterLayout.propertyCategoryAct.getText().toString().trim();
        selectedSubcategory = binding.fillterLayout.propertySubcategoryAct.getText().toString().trim();

        String minPriceStr = binding.fillterLayout.priceMinEt.getText().toString().trim();
        String maxPriceStr = binding.fillterLayout.priceMaxEt.getText().toString().trim();

        // Debug log filter values
        debugFilterValues();

        try {
            minPrice = minPriceStr.isEmpty() || minPriceStr.equals("0") ? 0.0 : Double.parseDouble(minPriceStr);
            maxPrice = maxPriceStr.isEmpty() || maxPriceStr.equals("0") ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing price: ", e);
            MyUtils.toast(mContext, "Vui lòng nhập giá hợp lệ");
            return;
        }

        // Check if any filter is actually selected
        boolean hasAnyFilter = !selectedPurpose.isEmpty() ||
                (!selectedCategory.isEmpty() && !selectedCategory.equals("Tất cả")) ||
                (!selectedSubcategory.isEmpty() && !selectedSubcategory.equals("Tất cả")) ||
                minPrice > 0 || maxPrice < Double.MAX_VALUE;

        Log.d(TAG, "applyFilter: hasAnyFilter = " + hasAnyFilter);

        // Update filter display text
        updateFilterDisplayText();

        // Apply filter to the list
        applyFilterToList();

        // Debug adapter after filtering
        if (adapterProperty != null) {
            adapterProperty.debugAdapterData();
        }

        // Hide filter panel
        hideFilter();

        MyUtils.toast(mContext, "Đã áp dụng bộ lọc - Tìm thấy " + propertyArrayList.size() + " kết quả");
        Log.d(TAG, "applyFilter: Filter applied successfully - " + propertyArrayList.size() + " results");
    }

    private void debugPropertyData() {
        Log.d(TAG, "=== DEBUG PROPERTY DATA ===");
        Log.d(TAG, "Total properties loaded: " + (allPropertiesList != null ? allPropertiesList.size() : 0));

        if (allPropertiesList != null && !allPropertiesList.isEmpty()) {
            for (int i = 0; i < Math.min(5, allPropertiesList.size()); i++) {
                ModelProperty property = allPropertiesList.get(i);
                Log.d(TAG, "Property " + i + ":");
                Log.d(TAG, "  - Title: " + property.getTitle());
                Log.d(TAG, "  - Purpose: '" + property.getPurpose() + "'");
                Log.d(TAG, "  - Category: '" + property.getCategory() + "'");
                Log.d(TAG, "  - Subcategory: '" + property.getSubcategory() + "'");
                Log.d(TAG, "  - Price: " + property.getPrice());
            }
        }
        Log.d(TAG, "=== END DEBUG ===");
    }

    private void updateFilterDisplayText() {
        StringBuilder filterText = new StringBuilder();

        if (!selectedPurpose.isEmpty() && !selectedPurpose.equals("Tất cả")) {
            filterText.append(selectedPurpose);
        }

        if (!selectedCategory.isEmpty() && !selectedCategory.equals("Tất cả")) {
            if (filterText.length() > 0) filterText.append(" • ");
            filterText.append(selectedCategory);
        }

        if (!selectedSubcategory.isEmpty() && !selectedSubcategory.equals("Tất cả")) {
            if (filterText.length() > 0) filterText.append(" • ");
            filterText.append(selectedSubcategory);
        }

        if (minPrice > 0 || maxPrice < Double.MAX_VALUE) {
            if (filterText.length() > 0) filterText.append(" • ");
            if (maxPrice == Double.MAX_VALUE) {
                filterText.append("Từ ").append(MyUtils.formatCurrency(minPrice)).append("đ");
            } else {
                filterText.append(MyUtils.formatCurrency(minPrice)).append("đ - ")
                        .append(MyUtils.formatCurrency(maxPrice)).append("đ");
            }
        }

        if (filterText.length() == 0) {
            binding.filterSelectedTv.setText("Hiển thị tất cả");
        } else {
            binding.filterSelectedTv.setText(filterText.toString());
        }
    }

    private void applyFilterToList() {
        if (allPropertiesList == null || allPropertiesList.isEmpty()) {
            Log.d(TAG, "applyFilterToList: allPropertiesList is null or empty");
            return;
        }

        ArrayList<ModelProperty> filteredList = new ArrayList<>();

        Log.d(TAG, "applyFilterToList: Applying filters:");
        Log.d(TAG, "- Purpose: '" + selectedPurpose + "'");
        Log.d(TAG, "- Category: '" + selectedCategory + "'");
        Log.d(TAG, "- Subcategory: '" + selectedSubcategory + "'");
        Log.d(TAG, "- Price range: " + minPrice + " - " + maxPrice);
        Log.d(TAG, "- Total properties to filter: " + allPropertiesList.size());

        for (ModelProperty property : allPropertiesList) {
            boolean matchesFilter = true;

            // Filter by purpose (Mua/Thuê)
            if (!selectedPurpose.isEmpty() && !selectedPurpose.equals("Tất cả")) {
                // Map the filter purpose to property purpose
                String filterPurpose = selectedPurpose.equals("Mua") ? "Đăng bán" : "Cho thuê";
                Log.d(TAG, "Checking purpose: property='" + property.getPurpose() + "' vs filter='" + filterPurpose + "'");
                if (!property.getPurpose().trim().equalsIgnoreCase(filterPurpose.trim())) {
                    matchesFilter = false;
                    Log.d(TAG, "Purpose mismatch for property: " + property.getTitle());
                }
            }

            // Filter by category
            if (matchesFilter && !selectedCategory.isEmpty() && !selectedCategory.equals("Tất cả")) {
                Log.d(TAG, "Checking category: property='" + property.getCategory() + "' vs filter='" + selectedCategory + "'");
                if (!property.getCategory().trim().equalsIgnoreCase(selectedCategory.trim())) {
                    matchesFilter = false;
                    Log.d(TAG, "Category mismatch for property: " + property.getTitle());
                }
            }

            // Filter by subcategory
            if (matchesFilter && !selectedSubcategory.isEmpty() && !selectedSubcategory.equals("Tất cả")) {
                Log.d(TAG, "Checking subcategory: property='" + property.getSubcategory() + "' vs filter='" + selectedSubcategory + "'");
                if (!property.getSubcategory().trim().equalsIgnoreCase(selectedSubcategory.trim())) {
                    matchesFilter = false;
                    Log.d(TAG, "Subcategory mismatch for property: " + property.getTitle());
                }
            }

            // Filter by price range
            if (matchesFilter) {
                double propertyPrice = property.getPrice();
                Log.d(TAG, "Checking price: property=" + propertyPrice + " vs range [" + minPrice + ", " + maxPrice + "]");
                if (propertyPrice < minPrice || (maxPrice != Double.MAX_VALUE && propertyPrice > maxPrice)) {
                    matchesFilter = false;
                    Log.d(TAG, "Price mismatch for property: " + property.getTitle() + " (price: " + propertyPrice + ")");
                }
            }

            if (matchesFilter) {
                filteredList.add(property);
                Log.d(TAG, "✓ Property matches filter: " + property.getTitle());
            }
        }

        Log.d(TAG, "applyFilterToList: Found " + filteredList.size() + " matching properties out of " + allPropertiesList.size());

        // Update the main list that RecyclerView uses
        propertyArrayList.clear();
        propertyArrayList.addAll(filteredList);

        // Update the adapter
        if (adapterProperty != null) {
            adapterProperty.notifyDataSetChanged();
            // Also update the filter list in adapter for search functionality
            adapterProperty.updateFilterList(filteredList);
        }
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
        Log.d(TAG, "loadProperties: Starting to load properties");

        propertyArrayList = new ArrayList<>();
        allPropertiesList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                propertyArrayList.clear();
                allPropertiesList.clear();

                Log.d(TAG, "onDataChange: Found " + snapshot.getChildrenCount() + " properties in database");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        ModelProperty modelProperty = ds.getValue(ModelProperty.class);
                        if (modelProperty == null) {
                            Log.w(TAG, "onDataChange: Null property found, skipping");
                            continue;
                        }

                        double propertyLatitude = modelProperty.getLatitude();
                        double propertyLongitude = modelProperty.getLongitude();

                        double distance = MyUtils.caculateDistanceKm(
                                currentLatitude, currentLongitude,
                                propertyLatitude, propertyLongitude
                        );

                        Log.d(TAG, "onDataChange: Property '" + modelProperty.getTitle() + "' - Distance: " + distance + "km");

                        if (binding.cityTv.getText().toString().equals("Tất cả")) {
                            propertyArrayList.add(modelProperty);
                            allPropertiesList.add(modelProperty);
                            Log.d(TAG, "onDataChange: Added property (show all): " + modelProperty.getTitle());
                        } else {
                            if (distance <= MyUtils.MAX_DISTANCE_TO_LOAD_PROPERTIES) {
                                propertyArrayList.add(modelProperty);
                                allPropertiesList.add(modelProperty);
                                Log.d(TAG, "onDataChange: Added property (within distance): " + modelProperty.getTitle());
                            } else {
                                Log.d(TAG, "onDataChange: Skipped property (too far): " + modelProperty.getTitle());
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: Error processing property", e);
                    }
                }

                Log.d(TAG, "onDataChange: Final counts - propertyArrayList: " + propertyArrayList.size() +
                        ", allPropertiesList: " + allPropertiesList.size());

                // Debug dữ liệu
                debugPropertyData();

                // Tạo adapter
                adapterProperty = new AdapterProperty(mContext, propertyArrayList);
                binding.propertiesRv.setAdapter(adapterProperty);

                // Update the adapter's filter list as well
                if (adapterProperty != null) {
                    adapterProperty.updateFilterList(allPropertiesList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Database error: " + error.getMessage());
            }
        });
    }
}