package com.MeCore.mecore;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class NavigationUtil {

    private static final String TAG = "NavigationUtil";

    // Map of navigation item IDs to their corresponding activity classes
    private static final Map<Integer, Class<?>> ACTIVITY_MAP = new HashMap<>();
    // Map of navigation item IDs to their positions (for determining animation direction)
    private static final Map<Integer, Integer> ITEM_POSITION = new HashMap<>();

    static {
        // Activity mappings
        ACTIVITY_MAP.put(R.id.navigation_main, MainActivity.class);
        ACTIVITY_MAP.put(R.id.navigation_chat, ChatListActivity.class);
        ACTIVITY_MAP.put(R.id.navigation_profile, ProfileActivity.class);

        // Position mappings (based on menu order: Main -> Chats -> Profile)
        ITEM_POSITION.put(R.id.navigation_main, 0);
        ITEM_POSITION.put(R.id.navigation_chat, 1);
        ITEM_POSITION.put(R.id.navigation_profile, 2);
    }

    public static void setupBottomNavigationMenu(Activity currentActivity, BottomNavigationView bottomNavigationMenu, int selectedItemId) {
        // Set the selected item
        bottomNavigationMenu.setSelectedItemId(selectedItemId);

        // Set up the navigation listener
        bottomNavigationMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Item selected in " + currentActivity.getClass().getSimpleName() + ": ID=" + itemId);

            // Log icon details for debugging
            if (item.getIcon() != null) {
                Log.d(TAG, "Icon bounds for item " + itemId + ": " + item.getIcon().getBounds().width() + "x" + item.getIcon().getBounds().height());
            } else {
                Log.e(TAG, "Icon for item " + itemId + " is null or not loaded");
            }

            if (itemId == selectedItemId) {
                Log.d(TAG, "Already on the current activity");
                return true;
            }

            Class<?> targetActivityClass = ACTIVITY_MAP.get(itemId);
            if (targetActivityClass != null) {
                Log.d(TAG, "Navigating to " + targetActivityClass.getSimpleName());
                navigateToActivity(currentActivity, targetActivityClass, selectedItemId, itemId);
                return true;
            } else {
                Log.w(TAG, "Unknown item ID: " + itemId);
                return false;
            }
        });
    }

    private static void navigateToActivity(Activity currentActivity, Class<?> targetActivityClass, int currentItemId, int targetItemId) {
        try {
            Intent intent = new Intent(currentActivity, targetActivityClass);
            // Clear the activity stack and bring the target activity to the front
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // Determine animation direction based on menu item positions
            int currentPosition = ITEM_POSITION.getOrDefault(currentItemId, 0);
            int targetPosition = ITEM_POSITION.getOrDefault(targetItemId, 0);
            boolean isMovingRight = targetPosition > currentPosition;

            currentActivity.startActivity(intent);
            // Apply animation based on direction
            if (isMovingRight) {
                currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                currentActivity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }

            Log.d(TAG, targetActivityClass.getSimpleName() + " started successfully");
            // Only finish the current activity if it's not the target activity
            if (!currentActivity.getClass().equals(targetActivityClass)) {
                currentActivity.finish();
                if (isMovingRight) {
                    currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    currentActivity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start " + targetActivityClass.getSimpleName() + ": " + e.getMessage(), e);
            Toast.makeText(currentActivity, "Failed to open " + targetActivityClass.getSimpleName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}