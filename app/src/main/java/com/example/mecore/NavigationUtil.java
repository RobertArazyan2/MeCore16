package com.example.mecore;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationUtil {

    private static final String TAG = "NavigationUtil";

    public static void setupBottomNavigationMenu(Activity currentActivity, BottomNavigationView bottomNavigationMenu, int selectedItemId) {
        // Set the selected item
        bottomNavigationMenu.setSelectedItemId(selectedItemId);

        // Set up the navigation listener
        bottomNavigationMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            String itemName;
            try {
                itemName = currentActivity.getResources().getResourceEntryName(itemId);
                Log.d(TAG, "Item selected in " + currentActivity.getClass().getSimpleName() + ": ID=" + itemId + ", Name=" + itemName);
            } catch (Exception e) {
                itemName = "Unknown";
                Log.e(TAG, "Failed to get resource name for ID: " + itemId, e);
            }

            if (itemId == R.id.navigation_main) {
                if (currentActivity instanceof MainActivity) {
                    Log.d(TAG, "Already on MainActivity");
                    return true;
                }
                Log.d(TAG, "Navigating to MainActivity");
                navigateToActivity(currentActivity, MainActivity.class);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                if (currentActivity instanceof ProfileActivity) {
                    Log.d(TAG, "Already on ProfileActivity");
                    return true;
                }
                Log.d(TAG, "Navigating to ProfileActivity");
                navigateToActivity(currentActivity, ProfileActivity.class);
                return true;
            } else if (itemId == R.id.navigation_chat) {
                if (currentActivity instanceof ChatListActivity) {
                    Log.d(TAG, "Already on ChatListActivity");
                    return true;
                }
                Log.d(TAG, "Navigating to ChatListActivity");
                navigateToActivity(currentActivity, ChatListActivity.class);
                return true;
            }
            Log.w(TAG, "Unknown item ID: " + itemId);
            return false;
        });
    }

    private static void navigateToActivity(Activity currentActivity, Class<?> targetActivityClass) {
        try {
            Intent intent = new Intent(currentActivity, targetActivityClass);
            // Clear the activity stack and bring the target activity to the front
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            currentActivity.startActivity(intent);
            Log.d(TAG, targetActivityClass.getSimpleName() + " started successfully");
            // Only finish the current activity if it's not the target activity
            if (!currentActivity.getClass().equals(targetActivityClass)) {
                currentActivity.finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start " + targetActivityClass.getSimpleName() + ": " + e.getMessage(), e);
            Toast.makeText(currentActivity, "Failed to open " + targetActivityClass.getSimpleName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}