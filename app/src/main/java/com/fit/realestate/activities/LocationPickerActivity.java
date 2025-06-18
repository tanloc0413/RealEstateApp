package com.fit.realestate.activities;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.mapbox.maps.ScreenCoordinate;
import com.mapbox.maps.plugin.Plugin;

import com.fit.realestate.R;
import com.fit.realestate.databinding.ActivityLocationPickerBinding;
import com.fit.realestate.models.ModelProperty;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.ImageHolder;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;



public class LocationPickerActivity extends AppCompatActivity {
    // view binding
    private ActivityLocationPickerBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "LOCATION_PICKER_TAG";
    private boolean isFollowingLocation = false;
    // Map view
    private MapView mapView;
    // Image Button
    ImageButton imageButton;
    // Image Button
    ImageButton shareImgBtn;
    // Floating Action Button
    ModelProperty modelProperty;
    // point
    Point point;
    // firebase
    DatabaseReference databaseReference = null;
    boolean focusLocation = true;
    private boolean shouldGeocodeOnce = false;


    // private Location mLastKnowLocation = null;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedAddress = "";
    private String selectedCity = "";
    private String selectedCountry = "";
    private String selectedState = "";

    private PointAnnotationManager selectedLocationAnnotationManager;
    private PointAnnotation selectedLocationAnnotation;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init view binding... activity_location_picker.xml = ActivityLocationPickerBinding
        binding = ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseApp.initializeApp(LocationPickerActivity.this);
        mapView = binding.mapView;
        imageButton = binding.toolbarGpsBtn;
        shareImgBtn = binding.shareLocation;

        modelProperty = new ModelProperty();
        AnnotationPlugin annotationPlugin2 = mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
        selectedLocationAnnotationManager =
                PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin2, new AnnotationConfig());

        if (ActivityCompat.checkSelfPermission(
                LocationPickerActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        imageButton.setVisibility(View.VISIBLE);

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        longPressRunnable = new Runnable() {
                            @Override
                            public void run() {
                                // Chuyển đổi tọa độ screen thành tọa độ map
                                ScreenCoordinate screenCoordinate = new ScreenCoordinate(event.getX(), event.getY());
                                Point mapPoint = binding.mapView.getMapboxMap().coordinateForPixel(screenCoordinate);

                                // Xử lý long press
                                handleLongPress(mapPoint);
                            }
                        };
                        longPressHandler.postDelayed(longPressRunnable, 800); // 800ms cho long press
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_MOVE:
                        if (longPressRunnable != null) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                            longPressRunnable = null;
                        }
                        break;
                }
                return false;
            }
        });

        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            //        binding.mapView.getMapboxMap().loadStyleUri(Style.SATELLITE, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                binding.mapView.getMapboxMap().setCamera(new CameraOptions.Builder().zoom(20.0).build());
                LocationComponentPlugin locationComponentPlugin = getLocationComponent(binding.mapView);
                locationComponentPlugin.setEnabled(true);
                LocationPuck2D locationPuck2D = new LocationPuck2D();

                AnnotationPlugin annotationPlugin2 = mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
                selectedLocationAnnotationManager =
                        PointAnnotationManagerKt.createPointAnnotationManager(
                                annotationPlugin2, new AnnotationConfig());

//                locationPuck2D.setBearingImage(AppCompatResources.getDrawable(
//                        LocationPickerActivity.this, R.drawable.baseline_location)
//                );
                locationPuck2D.setBearingImage(ImageHolder.from(R.drawable.location_white));

                locationComponentPlugin.setLocationPuck(locationPuck2D);
                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                getGestures(binding.mapView).addOnMoveListener(onMoveListener);

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location_white);
                AnnotationPlugin annotationPlugin = mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
                PointAnnotationManager pointAnnotationManager =
                        PointAnnotationManagerKt.createPointAnnotationManager(
                                annotationPlugin, new AnnotationConfig());

//                PointAnnotationManager pointAnnotationManager = PointAnnotationManagerKt
//                        .createPointAnnotationManager(annotationPlugin, new AnnotationConfig());


                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Bật chế độ following location
                        isFollowingLocation = true;

                        // Thêm listeners để theo dõi vị trí
                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                        getGestures(binding.mapView).addOnMoveListener(onMoveListener);

                        // Đổi icon thành GPS white
                        imageButton.setImageDrawable(AppCompatResources.getDrawable(
                                LocationPickerActivity.this,
                                R.drawable.gps_white
                        ));

                        // CHỈ geocode 1 lần khi bấm nút, không tự động liên tục
                        if (LocationPickerActivity.this.point != null) {
                            // Zoom vào vị trí hiện tại
                            binding.mapView.getMapboxMap().setCamera(new CameraOptions
                                    .Builder()
                                    .center(LocationPickerActivity.this.point)
                                    .zoom(18.0)
                                    .build());

                            // Geocode địa chỉ CHỈ 1 LÀN duy nhất
                            geocodeLocation(LocationPickerActivity.this.point);

                            // Tắt chế độ following để không geocode nữa
                            isFollowingLocation = false;
                        } else {
                            // Nếu chưa có point, hiển thị thông báo và đợi GPS
                            binding.searchEt.setText("Đang định vị...");
                            Toast.makeText(LocationPickerActivity.this,
                                    "Đang xác định vị trí GPS...", Toast.LENGTH_SHORT).show();

                            // Đặt flag để geocode khi có vị trí GPS lần đầu
                            shouldGeocodeOnce = true;
                        }

                        imageButton.setVisibility(View.VISIBLE);

//                        isFollowingLocation = true;
//
//                        // Xóa marker đã chọn nếu có (vì giờ sẽ follow GPS)
//                        if (selectedLocationAnnotation != null) {
//                            selectedLocationAnnotationManager.delete(selectedLocationAnnotation);
//                            selectedLocationAnnotation = null;
//                        }
//
//                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
//                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
//                        getGestures(binding.mapView).addOnMoveListener(onMoveListener);
//
//                        imageButton.setImageDrawable(AppCompatResources.getDrawable(
//                                LocationPickerActivity.this,
//                                R.drawable.gps_white
//                        ));
//
//                        // Phóng to vào vị trí hiện tại
//                        if (LocationPickerActivity.this.point != null) {
//                            binding.mapView.getMapboxMap().setCamera(new CameraOptions
//                                    .Builder()
//                                    .center(LocationPickerActivity.this.point)
//                                    .zoom(18.0)
//                                    .build());
//                        }
//
//                        imageButton.setVisibility(View.VISIBLE);
                    }
                });

                FirebaseDatabase.getInstance().getReference().child("Location").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pointAnnotationManager.deleteAll();
                        snapshot.getChildren().forEach(new Consumer<DataSnapshot>() {
                            @Override
                            public void accept(DataSnapshot dataSnapshot) {
                                ModelProperty modelProperty = dataSnapshot.getValue(ModelProperty.class);

                                if (modelProperty != null && !modelProperty.getId().equals(
                                        LocationPickerActivity.this.modelProperty.getId())) {
                                    PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                                            .withTextAnchor(TextAnchor.CENTER)
                                            .withIconImage(bitmap)
                                            .withPoint(Point.fromLngLat(
                                                            modelProperty.getLongitude(),
                                                            modelProperty.getLatitude()
                                                    )
                                            );
                                    pointAnnotationManager.create(pointAnnotationOptions);
                                }
                            }
                        });

                        pointAnnotationManager.addClickListener(new OnPointAnnotationClickListener() {
                            @Override
                            public boolean onAnnotationClick(@NonNull PointAnnotation pointAnnotation) {
                                snapshot.getChildren().forEach(new Consumer<DataSnapshot>() {
                                    @Override
                                    public void accept(DataSnapshot dataSnapshot) {
                                        ModelProperty modelProperty = dataSnapshot.getValue(ModelProperty.class);

                                        if (modelProperty != null && pointAnnotation.getPoint().longitude()
                                                == modelProperty.getLongitude() &&
                                                pointAnnotation.getPoint().latitude()
                                                        == modelProperty.getLatitude()) {
                                            Toast.makeText(LocationPickerActivity.this,
                                                    "Clicked: " + modelProperty.getId() +
                                                            " Marker: ", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                return true;
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                shareImgBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (databaseReference == null) {
                            Toast.makeText(LocationPickerActivity.this,
                                    "Đang chia sẻ", Toast.LENGTH_SHORT).show();
                            databaseReference = FirebaseDatabase.getInstance()
                                    .getReference().child("sharedLocation").push();
                            modelProperty = new ModelProperty();
                            modelProperty.setId(databaseReference.getKey());
//                            location.setName("Username");
                            modelProperty.setLongitude(point.longitude());
                            modelProperty.setLatitude(point.latitude());
                            databaseReference.setValue(modelProperty);
//                            textView1.setText("Stop sharing");
                            binding.notification.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(LocationPickerActivity.this,
                                    "Đang chia sẻ", Toast.LENGTH_SHORT).show();
                            databaseReference.removeValue();
                            databaseReference = null;
//                            textView1.setText("Stop sharing");
//                            binding.notification.setVisibility(View.GONE);
                            binding.notification.setText("Dừng chia sẻ");
//                            binding.notification.setVisibility(View.VISIBLE);
//
//                            new Handler().postDelayed(() -> {
//                                binding.notification.setVisibility(View.GONE);
//                            }, 2000);
                        }

                    }
                });
            }
        });

        binding.doneBtn.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLatitude != null ? selectedLatitude : 0);
            resultIntent.putExtra("longitude", selectedLongitude != null ? selectedLongitude : 0);
            resultIntent.putExtra("address", selectedAddress != null ? selectedAddress : "");
            resultIntent.putExtra("city", selectedCity != null ? selectedCity : "");
            resultIntent.putExtra("country", selectedCountry != null ? selectedCountry : "");
            resultIntent.putExtra("state", selectedState != null ? selectedState : "");

            setResult(RESULT_OK, resultIntent);
            finish(); // kết thúc Activity và trả kết quả về PostAddActivity
        });


    }

    private ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        Toast.makeText(LocationPickerActivity.this,
                                "Permission Granted!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final OnIndicatorBearingChangedListener onIndicatorBearingChangedListener = new OnIndicatorBearingChangedListener() {
        @Override
        public void onIndicatorBearingChanged(double v) {
            binding.mapView.getMapboxMap().setCamera(new CameraOptions.Builder().bearing(v).build());
        }
    };

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener =
            new OnIndicatorPositionChangedListener() {
        @Override
        public void onIndicatorPositionChanged(@NonNull Point point) {
            binding.mapView.getMapboxMap().setCamera(new CameraOptions
                    .Builder()
                    .center(point)
                    .zoom(16.0)
                    .build());
            getGestures(binding.mapView).setFocalPoint(binding.mapView.getMapboxMap().pixelForCoordinate(point));
            LocationPickerActivity.this.point = point;

            selectedLatitude = point.latitude();
            selectedLongitude = point.longitude();

            if (shouldGeocodeOnce) {
                shouldGeocodeOnce = false;
                geocodeLocation(point);
            }
        }
    };

    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
            getLocationComponent(binding.mapView).removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
            getLocationComponent(binding.mapView).removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
            getGestures(binding.mapView).removeOnMoveListener(onMoveListener);
            imageButton.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector moveGestureDetector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

        }
    };

//    private void updateCamera(Point point, Double bearing) {
//        MapAnimationOptions mapAnimationOptions = new MapAnimationOptions
//                .Builder()
//                .duration(1500L)
//                .build();
//        CameraOptions cameraOptions = new CameraOptions
//                .Builder()
//                .center(point)
//                .zoom(18.0)
//                .bearing(bearing)
//                .pitch(45.0)
//                .padding(
//                        new EdgeInsets(
//                                1000.0,
//                                0.0,
//                                0.0,
//                                0.0
//                        )
//                )
//                .build();
//        getCamera(binding.mapView).easeTo(cameraOptions, mapAnimationOptions);
//    }

//    private void handleLongPress(Point point) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // Xóa marker cũ nếu có
//                if (selectedLocationAnnotation != null) {
//                    selectedLocationAnnotationManager.delete(selectedLocationAnnotation);
//                }
//
//                // Tạo marker mới tại vị trí được nhấn
//                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location_white);
//                PointAnnotationOptions selectedLocationOptions = new PointAnnotationOptions()
//                        .withTextAnchor(TextAnchor.CENTER)
//                        .withIconImage(bitmap)
//                        .withPoint(point);
//
//                selectedLocationAnnotation = selectedLocationAnnotationManager.create(selectedLocationOptions);
//
//                // Cập nhật vị trí hiện tại
//                LocationPickerActivity.this.point = point;
//                selectedLatitude = point.latitude();
//                selectedLongitude = point.longitude();
//
//                // Hiển thị toast thông báo
//                Toast.makeText(LocationPickerActivity.this,
//                        "Đã chọn vị trí: " + String.format("%.6f", point.latitude()) +
//                                ", " + String.format("%.6f", point.longitude()),
//                        Toast.LENGTH_SHORT).show();
//
//                // Tắt chế độ follow location
//                isFollowingLocation = false;
//
//                // Xóa listeners để ngừng theo dõi vị trí GPS
//                LocationComponentPlugin locationComponentPlugin = getLocationComponent(binding.mapView);
//                locationComponentPlugin.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
//                locationComponentPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
//                getGestures(binding.mapView).removeOnMoveListener(onMoveListener);
//
//                // Đổi icon button về trạng thái ban đầu (thay bằng icon GPS gốc của bạn)
//                imageButton.setImageDrawable(AppCompatResources.getDrawable(
//                        LocationPickerActivity.this,
//                        R.drawable.baseline_location // thay bằng icon GPS gốc của bạn
//                ));
//
//
//
//            }
//        });
//
//        Geocoder geocoder = new Geocoder(LocationPickerActivity.this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(point.latitude(), point.longitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                Address address = addresses.get(0);
//                String fullAddress = "";
//
//                String subLocality = address.getSubLocality(); // phường/xã
//                String locality = address.getLocality(); // quận/huyện hoặc thành phố nhỏ
//                String adminArea = address.getAdminArea(); // tỉnh/thành phố
//
//                if (subLocality != null) fullAddress += subLocality + ", ";
//                if (locality != null) fullAddress += locality + ", ";
//                if (adminArea != null) fullAddress += adminArea;
//
//                // Gán vào EditText
//                binding.searchEt.setText(fullAddress);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void handleLongPress(Point point) {
        // Cập nhật vị trí được chọn
        LocationPickerActivity.this.point = point;
        selectedLatitude = point.latitude();
        selectedLongitude = point.longitude();

        runOnUiThread(() -> {
            // Xóa marker cũ nếu có
            if (selectedLocationAnnotation != null) {
                selectedLocationAnnotationManager.delete(selectedLocationAnnotation);
            }

            // Tạo marker mới
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location_white);
            PointAnnotationOptions selectedLocationOptions = new PointAnnotationOptions()
                    .withTextAnchor(TextAnchor.CENTER)
                    .withIconImage(bitmap)
                    .withPoint(point);

            selectedLocationAnnotation = selectedLocationAnnotationManager.create(selectedLocationOptions);

            // Hiển thị tọa độ được chọn
            Toast.makeText(LocationPickerActivity.this,
                    "Đã chọn vị trí: " + String.format("%.6f", point.latitude()) +
                            ", " + String.format("%.6f", point.longitude()),
                    Toast.LENGTH_SHORT).show();

            // Dừng theo dõi GPS
            isFollowingLocation = false;

            LocationComponentPlugin locationComponentPlugin = getLocationComponent(binding.mapView);
            locationComponentPlugin.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
            locationComponentPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
            getGestures(binding.mapView).removeOnMoveListener(onMoveListener);

            // Cập nhật lại icon
            imageButton.setImageDrawable(AppCompatResources.getDrawable(
                    LocationPickerActivity.this,
                    R.drawable.baseline_location
            ));

            // Hiển thị loading trong searchEt
            binding.searchEt.setText("Đang tìm địa chỉ...");
        });

        // Xử lý lấy địa chỉ trong thread riêng
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(LocationPickerActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(point.latitude(), point.longitude(), 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    // Lấy các thành phần địa chỉ
                    String streetName = address.getThoroughfare(); // Tên đường
                    String streetNumber = address.getSubThoroughfare(); // Số nhà
                    String subLocality = address.getSubLocality(); // Phường/Xã
                    String locality = address.getLocality(); // Quận/Huyện
                    String adminArea = address.getAdminArea(); // Tỉnh/Thành phố
                    String countryName = address.getCountryName(); // Quốc gia
                    String featureName = address.getFeatureName(); // Tên địa điểm cụ thể (nếu có)

                    // Tạo chuỗi địa chỉ chi tiết
                    StringBuilder detailedAddress = new StringBuilder();

                    // Thêm tên địa điểm cụ thể (nếu có)
                    if (featureName != null && !featureName.isEmpty() &&
                            !featureName.equals(streetNumber) && !featureName.matches("\\d+")) {
                        detailedAddress.append(featureName).append(", ");
                    }

                    // Thêm số nhà và tên đường
                    if (streetNumber != null && !streetNumber.isEmpty()) {
                        detailedAddress.append(streetNumber).append(" ");
                    }
                    if (streetName != null && !streetName.isEmpty()) {
                        detailedAddress.append(streetName).append(", ");
                    }

                    // Thêm phường/xã
                    if (subLocality != null && !subLocality.isEmpty()) {
                        detailedAddress.append(subLocality).append(", ");
                    }

                    // Thêm quận/huyện
                    if (locality != null && !locality.isEmpty()) {
                        detailedAddress.append(locality).append(", ");
                    }

                    // Thêm tỉnh/thành phố
                    if (adminArea != null && !adminArea.isEmpty()) {
                        detailedAddress.append(adminArea);
                    }

                    // Thêm quốc gia (nếu cần)
                    if (countryName != null && !countryName.isEmpty() && !countryName.equals("Vietnam") && !countryName.equals("Việt Nam")) {
                        detailedAddress.append(", ").append(countryName);
                    }

                    // Xử lý trường hợp chuỗi kết thúc bằng dấu phẩy
                    String finalAddress = detailedAddress.toString();
                    if (finalAddress.endsWith(", ")) {
                        finalAddress = finalAddress.substring(0, finalAddress.length() - 2);
                    }

                    // Nếu không có thông tin chi tiết, sử dụng getAddressLine(0)
                    if (finalAddress.isEmpty()) {
                        finalAddress = address.getAddressLine(0);
                    }

                    // Lưu thông tin vào các biến class
                    selectedAddress = finalAddress;
                    selectedCity = locality != null ? locality : "";
                    selectedState = adminArea != null ? adminArea : "";
                    selectedCountry = countryName != null ? countryName : "";

                    // Cập nhật UI
                    final String addressToShow = finalAddress;
                    handler.post(() -> {
                        binding.searchEt.setText(addressToShow);

                        // Log thông tin chi tiết để debug
                        Log.d(TAG, "Địa chỉ chi tiết:");
                        Log.d(TAG, "- Tên địa điểm: " + (featureName != null ? featureName : "N/A"));
                        Log.d(TAG, "- Số nhà: " + (streetNumber != null ? streetNumber : "N/A"));
                        Log.d(TAG, "- Tên đường: " + (streetName != null ? streetName : "N/A"));
                        Log.d(TAG, "- Phường/Xã: " + (subLocality != null ? subLocality : "N/A"));
                        Log.d(TAG, "- Quận/Huyện: " + (locality != null ? locality : "N/A"));
                        Log.d(TAG, "- Tỉnh/TP: " + (adminArea != null ? adminArea : "N/A"));
                        Log.d(TAG, "- Quốc gia: " + (countryName != null ? countryName : "N/A"));
                    });

                } else {
                    handler.post(() -> {
                        binding.searchEt.setText("Không tìm thấy địa chỉ");
                        Toast.makeText(LocationPickerActivity.this, "Không tìm thấy địa chỉ tại vị trí này", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> {
                    binding.searchEt.setText("Lỗi khi lấy địa chỉ");
                    Toast.makeText(LocationPickerActivity.this, "Lỗi khi lấy địa chỉ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void geocodeLocation(Point point) {
        // Hiển thị loading trong searchEt
        runOnUiThread(() -> binding.searchEt.setText("Đang tìm địa chỉ..."));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(LocationPickerActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(point.latitude(), point.longitude(), 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    // Lấy các thành phần địa chỉ
                    String streetName = address.getThoroughfare(); // Tên đường
                    String streetNumber = address.getSubThoroughfare(); // Số nhà
                    String subLocality = address.getSubLocality(); // Phường/Xã
                    String locality = address.getLocality(); // Quận/Huyện
                    String adminArea = address.getAdminArea(); // Tỉnh/Thành phố
                    String countryName = address.getCountryName(); // Quốc gia
                    String featureName = address.getFeatureName(); // Tên địa điểm cụ thể (nếu có)

                    // Tạo chuỗi địa chỉ chi tiết
                    StringBuilder detailedAddress = new StringBuilder();

                    // Thêm tên địa điểm cụ thể (nếu có)
                    if (featureName != null && !featureName.isEmpty() &&
                            !featureName.equals(streetNumber) && !featureName.matches("\\d+")) {
                        detailedAddress.append(featureName).append(", ");
                    }

                    // Thêm số nhà và tên đường
                    if (streetNumber != null && !streetNumber.isEmpty()) {
                        detailedAddress.append(streetNumber).append(" ");
                    }
                    if (streetName != null && !streetName.isEmpty()) {
                        detailedAddress.append(streetName).append(", ");
                    }

                    // Thêm phường/xã
                    if (subLocality != null && !subLocality.isEmpty()) {
                        detailedAddress.append(subLocality).append(", ");
                    }

                    // Thêm quận/huyện
                    if (locality != null && !locality.isEmpty()) {
                        detailedAddress.append(locality).append(", ");
                    }

                    // Thêm tỉnh/thành phố
                    if (adminArea != null && !adminArea.isEmpty()) {
                        detailedAddress.append(adminArea);
                    }

                    // Thêm quốc gia (nếu cần)
                    if (countryName != null && !countryName.isEmpty() && !countryName.equals("Vietnam") && !countryName.equals("Việt Nam")) {
                        detailedAddress.append(", ").append(countryName);
                    }

                    // Xử lý trường hợp chuỗi kết thúc bằng dấu phẩy
                    String finalAddress = detailedAddress.toString();
                    if (finalAddress.endsWith(", ")) {
                        finalAddress = finalAddress.substring(0, finalAddress.length() - 2);
                    }

                    // Nếu không có thông tin chi tiết, sử dụng getAddressLine(0)
                    if (finalAddress.isEmpty()) {
                        finalAddress = address.getAddressLine(0);
                    }

                    // Lưu thông tin vào các biến class
                    selectedAddress = finalAddress;
                    selectedCity = locality != null ? locality : "";
                    selectedState = adminArea != null ? adminArea : "";
                    selectedCountry = countryName != null ? countryName : "";

                    // Cập nhật UI
                    final String addressToShow = finalAddress;
                    handler.post(() -> {
                        binding.searchEt.setText(addressToShow);

                        // Log thông tin chi tiết để debug
                        Log.d(TAG, "GPS Địa chỉ chi tiết:");
                        Log.d(TAG, "- Tên địa điểm: " + (featureName != null ? featureName : "N/A"));
                        Log.d(TAG, "- Số nhà: " + (streetNumber != null ? streetNumber : "N/A"));
                        Log.d(TAG, "- Tên đường: " + (streetName != null ? streetName : "N/A"));
                        Log.d(TAG, "- Phường/Xã: " + (subLocality != null ? subLocality : "N/A"));
                        Log.d(TAG, "- Quận/Huyện: " + (locality != null ? locality : "N/A"));
                        Log.d(TAG, "- Tỉnh/TP: " + (adminArea != null ? adminArea : "N/A"));
                        Log.d(TAG, "- Quốc gia: " + (countryName != null ? countryName : "N/A"));
                    });

                } else {
                    handler.post(() -> {
                        binding.searchEt.setText("Không tìm thấy địa chỉ GPS");
                        Toast.makeText(LocationPickerActivity.this, "Không tìm thấy địa chỉ tại vị trí GPS", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> {
                    binding.searchEt.setText("Lỗi khi lấy địa chỉ GPS");
                    Toast.makeText(LocationPickerActivity.this, "Lỗi khi lấy địa chỉ GPS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    @SuppressLint("Lifecycle")
    @Override
    protected void onStart() {
        super.onStart();
        binding.mapView.onStart();
    }

    @SuppressLint("Lifecycle")
    @Override
    protected void onStop() {
        super.onStop();
        binding.mapView.onStop();
    }

    @SuppressLint("Lifecycle")
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @SuppressLint("Lifecycle")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.mapView.onDestroy();
    }

}