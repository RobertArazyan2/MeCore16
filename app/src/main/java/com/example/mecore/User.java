package com.example.mecore;

import java.util.List;

public class User {
    private String userId;
    private String username;
    private List<String> selectedGames;

    public User() {
    }

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