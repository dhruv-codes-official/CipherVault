package com.dhruv.ui;

import com.dhruv.db.DataBaseConnection;
import com.dhruv.security.EncryptionService;
import com.dhruv.security.PasswordHasher;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;
import java.util.List;

public class ChangeMasterPasswordDialog extends JDialog {

    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public ChangeMasterPasswordDialog(JFrame parent) {
        super(parent, "Change Master Password", true);
        setSize(400, 350);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        formPanel.add(new JLabel("Old Master Password:"));
        oldPasswordField = new JPasswordField();
        formPanel.add(oldPasswordField);

        formPanel.add(new JLabel("New Master Password:"));
        newPasswordField = new JPasswordField();
        formPanel.add(newPasswordField);

        formPanel.add(new JLabel("Confirm New Password:"));
        confirmPasswordField = new JPasswordField();
        formPanel.add(confirmPasswordField);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton changeBtn = new JButton("Change Password");
        JButton cancelBtn = new JButton("Cancel");

        changeBtn.addActionListener(e -> handleChangePassword());
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(changeBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void handleChangePassword() {
        String oldPw = new String(oldPasswordField.getPassword());
        String newPw = new String(newPasswordField.getPassword());
        String confirmPw = new String(confirmPasswordField.getPassword());

        if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPw.equals(confirmPw)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPw.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=]).{8,}$")) {
            JOptionPane.showMessageDialog(this, "New password must contain:\n• Uppercase letter\n• Lowercase letter\n• Number\n• Symbol (@#$%^&+=)\n• Min length 8", 
                "Weak Password", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Validate old password
            String[] masterData = DataBaseConnection.getMasterPassword();
            if (masterData == null) {
                JOptionPane.showMessageDialog(this, "No master password set.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String storedHash = masterData[0];
            byte[] oldSalt = Base64.getDecoder().decode(masterData[1]);
            String inputHash = PasswordHasher.hashPassword(oldPw, oldSalt);

            if (!inputHash.equals(storedHash)) {
                JOptionPane.showMessageDialog(this, "Incorrect old password.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, 
                "Changing the master password will decrypt and re-encrypt all stored passwords.\nThis may take a moment. Do you wish to proceed?", 
                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) return;

            // 1. Fetch ALL passwords decrypted with CURRENT old key
            List<DataBaseConnection.PasswordEntry> allEntries = DataBaseConnection.getAllPasswordEntries();

            // 2. Generate new salt and hash new master password
            byte[] newSalt = PasswordHasher.generateSalt();
            String newHash = PasswordHasher.hashPassword(newPw, newSalt);

            // 3. Initialize the encryption service with the NEW key
            EncryptionService.initializeKey(newPw, newSalt);

            // 4. Update all passwords in the database. 
            // DataBaseConnection.updatePassword uses EncryptionService.encrypt() which now uses the NEW key!
            for (DataBaseConnection.PasswordEntry entry : allEntries) {
                DataBaseConnection.updatePassword(entry.website, entry.username, entry.password, entry.category);
                // Wipe the password string array from memory (best effort for String)
            }

            // 5. Update master password in database
            DataBaseConnection.saveMasterPassword(newHash, Base64.getEncoder().encodeToString(newSalt));

            JOptionPane.showMessageDialog(this, "Master Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Wipe sensitive arrays
            java.util.Arrays.fill(oldPasswordField.getPassword(), '\0');
            java.util.Arrays.fill(newPasswordField.getPassword(), '\0');
            java.util.Arrays.fill(confirmPasswordField.getPassword(), '\0');
            
            dispose();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
