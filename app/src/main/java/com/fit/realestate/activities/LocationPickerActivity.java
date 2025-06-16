package com.fit.realestate.activities;

import static android.view.View.GONE;
import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
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
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

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
    private boolean isLongPress = false;

    // private Location mLastKnowLocation = null;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedAddress = "";
    private String selectedCity = "";
    private String selectedCountry = "";
    private String selectedState = "";

    private PointAnnotationManager selectedLocationAnnotationManager;
    private PointAnnotation selectedLocationAnnotation;
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;


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

        mapView.setOnTouchListener(new View.OnTouchListener() {
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
                return false; // Cho phép các gesture khác hoạt động
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
                        PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin2, new AnnotationConfig());

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
                        PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, new AnnotationConfig());

//                PointAnnotationManager pointAnnotationManager = PointAnnotationManagerKt
//                        .createPointAnnotationManager(annotationPlugin, new AnnotationConfig());


                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
//                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
//                        getGestures(binding.mapView).addOnMoveListener(onMoveListener);
//                        imageButton.setImageDrawable(AppCompatResources.getDrawable(
//                                LocationPickerActivity.this,
//                                R.drawable.gps_white
//                        ));
//                        // hide
//                        imageButton.setVisibility(View.GONE);

//                        isFollowingLocation = true;
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
//                        imageButton.setVisibility(View.VISIBLE);

//                        isFollowingLocation = true;
//
//                        // Thêm listeners để theo dõi vị trí
//                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
//                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
//                        getGestures(binding.mapView).addOnMoveListener(onMoveListener);
//
//                        // Đổi icon thành GPS white
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
//                                    .zoom(18.0) // Zoom level cao để phóng to
//                                    .build());
//                        } else {
//                            // Nếu chưa có point, zoom với vị trí mặc định
//                            Toast.makeText(LocationPickerActivity.this,
//                                    "Đang xác định vị trí...", Toast.LENGTH_SHORT).show();
//                        }
//
//                        imageButton.setVisibility(View.VISIBLE);

                        isFollowingLocation = true;

                        // Xóa marker đã chọn nếu có (vì giờ sẽ follow GPS)
                        if (selectedLocationAnnotation != null) {
                            selectedLocationAnnotationManager.delete(selectedLocationAnnotation);
                            selectedLocationAnnotation = null;
                        }

                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                        getGestures(binding.mapView).addOnMoveListener(onMoveListener);

                        imageButton.setImageDrawable(AppCompatResources.getDrawable(
                                LocationPickerActivity.this,
                                R.drawable.gps_white
                        ));

                        // Phóng to vào vị trí hiện tại
                        if (LocationPickerActivity.this.point != null) {
                            binding.mapView.getMapboxMap().setCamera(new CameraOptions
                                    .Builder()
                                    .center(LocationPickerActivity.this.point)
                                    .zoom(18.0)
                                    .build());
                        }

                        imageButton.setVisibility(View.VISIBLE);
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

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = new OnIndicatorPositionChangedListener() {
        @Override
        public void onIndicatorPositionChanged(@NonNull Point point) {
            binding.mapView.getMapboxMap().setCamera(new CameraOptions
                    .Builder()
                    .center(point)
                    .zoom(16.0)
                    .build());
            getGestures(binding.mapView).setFocalPoint(binding.mapView.getMapboxMap().pixelForCoordinate(point));
            LocationPickerActivity.this.point = point;
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

    private void handleLongPress(Point point) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Xóa marker cũ nếu có
                if (selectedLocationAnnotation != null) {
                    selectedLocationAnnotationManager.delete(selectedLocationAnnotation);
                }

                // Tạo marker mới tại vị trí được nhấn
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location_white);
                PointAnnotationOptions selectedLocationOptions = new PointAnnotationOptions()
                        .withTextAnchor(TextAnchor.CENTER)
                        .withIconImage(bitmap)
                        .withPoint(point);

                selectedLocationAnnotation = selectedLocationAnnotationManager.create(selectedLocationOptions);

                // Cập nhật vị trí hiện tại
                LocationPickerActivity.this.point = point;
                selectedLatitude = point.latitude();
                selectedLongitude = point.longitude();

                // Hiển thị toast thông báo
                Toast.makeText(LocationPickerActivity.this,
                        "Đã chọn vị trí: " + String.format("%.6f", point.latitude()) +
                                ", " + String.format("%.6f", point.longitude()),
                        Toast.LENGTH_SHORT).show();

                // Tắt chế độ follow location
                isFollowingLocation = false;

                // Xóa listeners để ngừng theo dõi vị trí GPS
                LocationComponentPlugin locationComponentPlugin = getLocationComponent(binding.mapView);
                locationComponentPlugin.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                locationComponentPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                getGestures(binding.mapView).removeOnMoveListener(onMoveListener);

                // Đổi icon button về trạng thái ban đầu (thay bằng icon GPS gốc của bạn)
                imageButton.setImageDrawable(AppCompatResources.getDrawable(
                        LocationPickerActivity.this,
                        R.drawable.baseline_location // thay bằng icon GPS gốc của bạn
                ));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }


}