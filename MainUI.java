package com.dhruv.ui;
import com.dhruv.db.DataBaseConnection;

/**
 * Hello world!
 *
 */
public class MainUI {
public static void main(String[] args) {

    DataBaseConnection.initializeDatabase();

    DataBaseConnection.insertPassword("github.com", "dhruv", "123456", "General");
    DataBaseConnection.insertPassword("gmail.com", "dhruv", "mypassword", "General");
    DataBaseConnection.deletePassword("gmail.com", "dhruv");
    DataBaseConnection.searchPassword("github.com", "dhruv");
    DataBaseConnection.searchPassword("amazon.com", "dhruv");

    System.out.println("\nStored Passwords:");
    DataBaseConnection.viewAllPasswords();
}
}
