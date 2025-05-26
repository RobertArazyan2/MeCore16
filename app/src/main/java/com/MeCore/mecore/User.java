package com.MeCore.mecore;

import java.util.List;

public class User {
    private final String userId;
    private final String username;
    private final List<String> selectedGames;

    public User(String userId, String username, List<String> selectedGames) {
        this.userId = userId;
        this.username = username;
        this.selectedGames = selectedGames;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getSelectedGames() {
        return selectedGames;
    }
}