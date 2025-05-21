package com.example.realestate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.realestate.databinding.ActivityMainBinding;
import com.example.realestate.fragment.ChatListFragment;
import com.example.realestate.fragment.FavoriteListFragment;
import com.example.realestate.fragment.HomeFragment;
import com.example.realestate.fragment.ProfileFragment;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    // View Binding
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // By default (when app open) show HomeFragment
        showHomeFragment();

        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if(itemId == R.id.item_home) {

                    showHomeFragment();
                } else if (itemId == R.id.item_chat) {

                    showChatListFragment();
                } else if (itemId == R.id.item_favorite) {

                    showFavoriteListFragment();
                } else if (itemId == R.id.item_profile) {

                    showProfileFragment();
                }
                return true;
            }
        });

    }

    private void showHomeFragment() {
        binding.toolbarTitleTv.setText("Home");

        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), homeFragment, "HomeFragment");
        fragmentTransaction.commit();
    }

    private void showChatListFragment() {
        binding.toolbarTitleTv.setText("Chat");

        ChatListFragment chatListFragment = new ChatListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), chatListFragment, "ChatListFragment");
        fragmentTransaction.commit();
    }

    private void showFavoriteListFragment() {
        binding.toolbarTitleTv.setText("Chat");

        FavoriteListFragment favoriteListFragment = new FavoriteListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), favoriteListFragment, "FavoriteListFragment");
        fragmentTransaction.commit();
    }

    private void showProfileFragment() {
        binding.toolbarTitleTv.setText("Chat");

        ProfileFragment profileFragment = new ProfileFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), profileFragment, "ProfileFragment");
        fragmentTransaction.commit();
    }
}