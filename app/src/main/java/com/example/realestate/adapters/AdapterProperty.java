package com.example.realestate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.RowPropertyBinding;
import com.example.realestate.models.ModelProperty;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterProperty extends RecyclerView.Adapter<AdapterProperty.HolderProperty> {
    // view binding
    private RowPropertyBinding binding;
    private static final String TAG = "PROPERTY_TAG";
    // context of activity/fragment from where instance of AdapterAd class is created
    private Context context;
    // propertyArrayList the list of the Ads
    private ArrayList<ModelProperty> propertyArrayList;
    private FirebaseAuth firebaseAuth;

    public AdapterProperty(Context context, ArrayList<ModelProperty> propertyArrayList) {
        this.context = context;
        this.propertyArrayList = propertyArrayList;

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

        double price = modelProperty.getPrice();
        long timestamp = modelProperty.getTimestamp();
        String propertyId = modelProperty.getId();
        String title = modelProperty.getTitle();
        String description = modelProperty.getDescription();
        String address = modelProperty.getAddress();
        String purpose = modelProperty.getPurpose();
        String category = modelProperty.getCategory();
        String subcategory = modelProperty.getSubcategory();
        String formattedPrice = MyUtils.formatCurrency(price);
        String formattedDate = MyUtils.formatTimestampDate(timestamp);

        loadPropertyFirstImage(modelProperty, holder);

        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite(modelProperty, holder);
        }

        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.purposeTv.setText(purpose);
        holder.categoryTv.setText(category);
        holder.subcategoryTv.setText(subcategory);
        holder.addressTv.setText(address);
        holder.dateTv.setText(formattedDate);
        holder.priceTv.setText(formattedPrice);

        holder.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean favorite = modelProperty.isFavorite();

                if (favorite) {
                    MyUtils.removeFromFavorite(context, propertyId);
                } else {
                    MyUtils.addToFavorite(context, propertyId);
                }
            }
        });
    }

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
                            holder.favoriteBtn.setImageResource(R.drawable.un_fav_red);
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
            categoryTv = binding.purposeTv;
            subcategoryTv = binding.purposeTv;
            addressTv = binding.purposeTv;
            dateTv = binding.dateTv;
            priceTv = binding.priceTv;
            favoriteBtn = binding.favoriteBtn;
        }
    }
}
