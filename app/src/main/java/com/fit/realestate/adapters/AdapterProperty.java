package com.fit.realestate.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.activities.PropertyDetailActivity;
import com.fit.realestate.databinding.RowPropertyBinding;
import com.fit.realestate.models.ModelProperty;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterProperty extends RecyclerView.Adapter<AdapterProperty.HolderProperty> implements Filterable {
    // view binding
    private RowPropertyBinding binding;
    private static final String TAG = "PROPERTY_TAG";
    // context of activity/fragment from where instance of AdapterAd class is created
    private Context context;
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // propertyArrayList the list of the Ads
    private ArrayList<ModelProperty> propertyArrayList;
    private ArrayList<ModelProperty> filterList;
    private Filter filter;

    public AdapterProperty(Context context, ArrayList<ModelProperty> propertyArrayList) {
        this.context = context;
        this.propertyArrayList = new ArrayList<>(propertyArrayList);
        this.filterList = new ArrayList<>(propertyArrayList);

        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderProperty onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = RowPropertyBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderProperty(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderProperty holder, int position) {
        ModelProperty modelProperty = propertyArrayList.get(position);

        String propertyId = modelProperty.getId();
        String title = modelProperty.getTitle();
        String description = modelProperty.getDescription();
        String address = modelProperty.getAddress();
        String purpose = modelProperty.getPurpose();
        String category = modelProperty.getCategory();
        String subcategory = modelProperty.getSubcategory();
        double price = modelProperty.getPrice();
        long timestamp = modelProperty.getTimestamp();
        String formattedPrice = MyUtils.formatCurrency(price);
        String formattedDate = MyUtils.formatTimestampDate(timestamp);
        String displayCategory = category.equals("Thương mại - Dịch vụ") ? "TMDV" : category;

        loadPropertyFirstImage(modelProperty, holder);

        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite(modelProperty, holder);
        }

        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.purposeTv.setText(purpose);
//        holder.categoryTv.setText(category);
        holder.categoryTv.setText(displayCategory);
        holder.subcategoryTv.setText(subcategory);
        holder.addressTv.setText(address);
        holder.dateTv.setText(formattedDate);
        holder.priceTv.setText(formattedPrice + "đ");

        // xử lý sự kiện nhấn yêu thích
        holder.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean favorite = modelProperty.isFavorite();

                if (favorite) {
                    MyUtils.removeFromFavorite(context, propertyId);
                } else {
                    MyUtils.addToFavorite(context, propertyId);
                }

//                checkIsFavorite(modelProperty, holder);

            }
        });

        // xử lý sự kiện nhấn card để xem bài đăng chi tiết
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: propertyId = " + propertyId);
                Intent intent = new Intent(context, PropertyDetailActivity.class);
                intent.putExtra("propertyId", propertyId);
                context.startActivity(intent);
            }
        });


    }

    // lấy hình ảnh đầu tiên
    private void loadPropertyFirstImage(ModelProperty modelProperty, HolderProperty holder) {
        Log.d(TAG, "loadPropertyFirstImage: ");

        String propertyId = modelProperty.getId();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).child("Images").limitToFirst(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String imageUrl = "" + ds.child("imageUrl").getValue();
                            Log.d(TAG, "onDataChange: imageUrl" + imageUrl);

                            try {
                                Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.apartment)
                                        .into(holder.propertyIv);
                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIsFavorite(ModelProperty modelProperty, HolderProperty holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(modelProperty.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean favorite = snapshot.exists();

                        modelProperty.setFavorite(favorite);

                        if (favorite) {
                            holder.favoriteBtn.setImageResource(R.drawable.fav_red);
                        } else {
                            holder.favoriteBtn.setImageResource(R.drawable.un_fav_black);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return propertyArrayList.size();
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    ArrayList<ModelProperty> filteredList = new ArrayList<>();

                    Log.d(TAG, "performFiltering: query = '" + constraint + "'");
                    Log.d(TAG, "performFiltering: filterList size = " + filterList.size());

                    if (constraint == null || constraint.length() == 0) {
                        // Nếu không có query, hiển thị tất cả items trong filterList
                        filteredList.addAll(filterList);
                        Log.d(TAG, "performFiltering: No query, showing all " + filterList.size() + " items");
                    } else {
                        String searchQuery = constraint.toString().toLowerCase().trim();
                        Log.d(TAG, "performFiltering: Searching for '" + searchQuery + "'");

                        for (ModelProperty property : filterList) {
                            try {
                                String title = property.getTitle() != null ? property.getTitle().toLowerCase() : "";
                                String description = property.getDescription() != null ? property.getDescription().toLowerCase() : "";
                                String category = property.getCategory() != null ? property.getCategory().toLowerCase() : "";
                                String subcategory = property.getSubcategory() != null ? property.getSubcategory().toLowerCase() : "";
                                String address = property.getAddress() != null ? property.getAddress().toLowerCase() : "";

                                if (title.contains(searchQuery) ||
                                        description.contains(searchQuery) ||
                                        category.contains(searchQuery) ||
                                        subcategory.contains(searchQuery) ||
                                        address.contains(searchQuery)) {
                                    filteredList.add(property);
                                    Log.d(TAG, "performFiltering: Found match: " + property.getTitle());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "performFiltering: Error processing property: " + property.getTitle(), e);
                            }
                        }

                        Log.d(TAG, "performFiltering: Found " + filteredList.size() + " matches");
                    }

                    results.values = filteredList;
                    results.count = filteredList.size();

                    return results;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults results) {
                    Log.d(TAG, "publishResults: Updating UI with " + results.count + " results");

                    propertyArrayList.clear();
                    if (results.values != null) {
                        propertyArrayList.addAll((ArrayList<ModelProperty>) results.values);
                    }
                    notifyDataSetChanged();

                    Log.d(TAG, "publishResults: propertyArrayList now has " + propertyArrayList.size() + " items");
                }
            };
        }

        return filter;
    }

    // Add this method to AdapterProperty class
    public void updateFilterList(ArrayList<ModelProperty> newFilterList) {
        Log.d(TAG, "updateFilterList: Updating filter list with " + newFilterList.size() + " items");

        this.filterList.clear();
        this.filterList.addAll(newFilterList);

        // Cũng cập nhật propertyArrayList để hiển thị ngay lập tức
        this.propertyArrayList.clear();
        this.propertyArrayList.addAll(newFilterList);

        notifyDataSetChanged();

        Log.d(TAG, "updateFilterList: Update complete. filterList: " + filterList.size() +
                ", propertyArrayList: " + propertyArrayList.size());
    }

    public void debugAdapterData() {
        Log.d(TAG, "=== ADAPTER DEBUG ===");
        Log.d(TAG, "propertyArrayList size: " + propertyArrayList.size());
        Log.d(TAG, "filterList size: " + filterList.size());

        if (!propertyArrayList.isEmpty()) {
            Log.d(TAG, "First few items in propertyArrayList:");
            for (int i = 0; i < Math.min(3, propertyArrayList.size()); i++) {
                ModelProperty property = propertyArrayList.get(i);
                Log.d(TAG, "  " + i + ": " + property.getTitle() + " - " + property.getPurpose());
            }
        }
        Log.d(TAG, "=== END ADAPTER DEBUG ===");
    }

    class HolderProperty extends RecyclerView.ViewHolder {
        // UI Views of the row_property.xml
        ShapeableImageView propertyIv;
        TextView titleTv, descriptionTv, purposeTv, categoryTv, subcategoryTv, addressTv, dateTv, priceTv;
        ImageButton favoriteBtn;

        public HolderProperty(@NonNull View itemView) {
            super(itemView);

            // init UI Views of the row_property.xml
            propertyIv = binding.propertyIv;
            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            purposeTv = binding.purposeTv;
            categoryTv = binding.categoryTv;
            subcategoryTv = binding.subcategoryTv;
            addressTv = binding.addressTv;
            dateTv = binding.dateTv;
            priceTv = binding.priceTv;
            favoriteBtn = binding.favoriteBtn;
//            materialCardView = binding.cardView;
        }
    }
}
