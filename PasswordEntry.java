package com.dhruv.model;

public class PasswordEntry {

    private int id;
    private String website;
    private String username;
    private String password;

    public PasswordEntry(String website, String username, String password) {
        this.website = website;
        this.username = username;
        this.password = password;
    }

    public PasswordEntry(int id, String website, String username, String password) {
        this.id = id;
        this.website = website;
        this.username = username;
        this.password = password;
    }

    public int getId() { return id; }
    public String getWebsite() { return website; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
