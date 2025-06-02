package com.example.realestate.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityMainBinding;
import com.example.realestate.fragments.ChatListFragment;
import com.example.realestate.fragments.FavoriteListFragment;
import com.example.realestate.fragments.HomeFragment;
import com.example.realestate.fragments.ProfileFragment;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    // View Binding
    private ActivityMainBinding binding;
    // Firebase
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_main.xml = ActivityMainBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
        // check if user is logged in or not
        if(firebaseAuth.getCurrentUser() == null) {{
            // user is not logged in, move to LoginOptionsActivity
            startLoginOptionsActivity();
        }}

        // by default (when app open) show HomeFragment
        showHomeFragment();

        // handle bottomNv item clicks to navigate between fragments
        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // get id of the menu item clicked
                int itemId = item.getItemId();
                // check which item is clicked and show fragment accordingly
                if(itemId == R.id.item_home) {
                    // Home item clicked, show HomeFragment
                    showHomeFragment();
                    // Return true so bottom navigation menu become selected
                    return true;
                } else if (itemId == R.id.item_chat) {
                    // Chats item clicked, show ChatsListFragment

                    if (firebaseAuth.getCurrentUser() == null) {
                        // Not, Logged-In, show message toast
                        MyUtils.toast(MainActivity.this, "Login Required...!");
                        // Return false so bottom navigation menu doesn't become selected
                        return false;
                    } else {
                        // Logged-In, open ChatListFragment
                        showChatListFragment();
                        // Return true so bottom navigation menu become selected
                        return true;
                    }
                } else if (itemId == R.id.item_favorite) {
                    // Favorites item clicked, show FavoriteListFragment

                    if (firebaseAuth.getCurrentUser() == null) {
                        // Not, Logged-In, show message toast
                        MyUtils.toast(MainActivity.this, "Login Required...!");
                        // Return false so bottom navigation menu doesn't become selected
                        return false;
                    } else {
                        // Logged-In, open FavoriteListFragment
                        showFavoriteListFragment();
                        // Return true so bottom navigation menu become selected
                        return true;
                    }
                } else if (itemId == R.id.item_profile) {
                    // Profile item clicked, show ProfileFragment

                    if (firebaseAuth.getCurrentUser() == null) {
                        // Not, Logged-In, show message toast
                        MyUtils.toast(MainActivity.this, "Login Required...!");
                        // Return false so bottom navigation menu doesn't become selected
                        return false;
                    } else {
                        // Logged-In, open ProfileListFragment
                        showProfileFragment();
                        // Return true so bottom navigation menu become selected
                        return true;
                    }
                } else {
                    // Return false so bottom navigation menu doesn't become selected
                    return false;
                }
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

    private void startLoginOptionsActivity() {
        startActivity(new Intent(this, LoginOptionsActivity.class));
    }
}