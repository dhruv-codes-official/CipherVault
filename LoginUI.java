package com.dhruv.ui; // Declares this file belongs to the 'ui' package — all Swing window classes are grouped here

import javax.swing.*;                      // Imports all Swing components: JFrame, JPanel, JLabel, JButton, JPasswordField, JProgressBar, BoxLayout, BorderFactory, Timer, etc.
import javax.swing.event.DocumentEvent;    // Event fired when text is inserted or removed in a document (used to detect password typing in real-time)
import javax.swing.event.DocumentListener; // Listener interface for document change events — used to update the strength bar as the user types

import com.dhruv.security.PasswordHasher; // Import for generating a random salt and hashing the master password using PBKDF2

import java.awt.*;          // Imports AWT classes: Color, Font, Component, Dimension, FlowLayout, BorderLayout, BoxLayout, etc.
import java.util.Base64;    // Java standard Base64 encoder — converts raw byte[] salt to a text string for DB storage

import com.dhruv.db.DataBaseConnection;   // Import to call getMasterPassword() (check if setup done) and saveMasterPassword() (persist hash+salt on first run)
import com.dhruv.security.EncryptionService; // Import to call initializeKey() — derives the AES-256 encryption key from the master password after login

/**
 * LoginUI — The master password entry window (first screen the user sees).
 *
 * Operates in two modes detected at construction time:
 *   FIRST-RUN (setup) mode — if no master password exists in the DB yet:
 *     Shows a "Create Master Password" form with a strength bar and confirm field.
 *   LOGIN mode — if a master password hash already exists in the DB:
 *     Shows a simpler "Unlock Vault" form with just the master password field.
 *
 * On successful authentication, LoginUI disposes itself and opens PasswordManagerGUI.
 */
public class LoginUI extends JFrame { // JFrame = a top-level Swing window with a title bar, borders, and close button

    // ── State determined at construction time ─────────────────────────────────
    String[] masterData = DataBaseConnection.getMasterPassword(); // Query the DB for the stored [hash, salt] array; returns null if no master password has been set yet
    boolean firstRun = (masterData == null);                      // true = this is the first time the app runs (setup mode); false = returning user (login mode)

    // ── UI Component Fields ────────────────────────────────────────────────────
    private JPasswordField masterPasswordField;  // The primary password input — always shown in both modes
    private JPasswordField confirmPasswordField; // The "re-enter password" field — only shown in firstRun (setup) mode
    private JButton actionButton;
    private int failedAttempts = 0;

    // Password strength components — only created and used in firstRun mode
    private JProgressBar strengthBar;   // A thin bar (3 segments, values 0–3) that fills with colour to indicate password strength
    private JLabel strengthLabel;       // Text label showing "Weak" / "Medium" / "Strong" next to the bar

    /**
     * LoginUI constructor — builds and displays the authentication window.
     * Automatically detects whether to show the setup form or the login form
     * based on whether the 'settings' table already has a row.
     */
    public LoginUI() {
        setTitle("Secure Password Manager");               // Window title bar text
        setSize(520, firstRun ? 580 : 460);               // Taller window for setup mode (needs confirm field + strength bar); shorter for login mode
        setResizable(false);                               // Prevent resizing — the fixed layout is not designed to re-flow at different sizes
        setLocationRelativeTo(null);                       // Centre the window on screen relative to no parent (null = screen centre)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   // Terminate the JVM when the user clicks the window's X button
        getContentPane().setBackground(UITheme.BG_PRIMARY); // Set the JFrame's content pane background to the app's darkest colour

        // ── Root Panel: BorderLayout divides window into North (header) / Centre (form) / South (button) ──
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0)); // BorderLayout: positions children in NORTH, SOUTH, EAST, WEST, CENTER regions; 0,0 = no gaps
        mainPanel.setBackground(UITheme.BG_PRIMARY); // Same dark background as the window itself

        // ── NORTH: Gradient Header with App Icon and Title ────────────────────
        UITheme.GradientPanel header = new UITheme.GradientPanel(); // Custom panel that paints a subtle dark-to-navy horizontal gradient
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS)); // BoxLayout Y_AXIS stacks children vertically (icon → title → subtitle)
        header.setBorder(BorderFactory.createEmptyBorder(32, 30, 28, 30)); // 32px top padding, 28px bottom — creates generous spacing inside the header

        JLabel icon = new JLabel("🛡️"); // Shield emoji used as an app icon; cheaper than loading an image resource
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48)); // 48pt emoji font so the shield renders large and clearly
        icon.setAlignmentX(Component.CENTER_ALIGNMENT); // BoxLayout Y_AXIS: individually align this component to centre (other alignment settings are per-component)
        header.add(icon);                    // Add the shield icon to the header
        header.add(Box.createVerticalStrut(8)); // Add an invisible 8px vertical spacer between the icon and the title

        JLabel title = UITheme.createLabel("Secure Vault", UITheme.FONT_HEADING_LG, UITheme.TEXT_PRIMARY); // Large bold "Secure Vault" heading
        title.setAlignmentX(Component.CENTER_ALIGNMENT); // Centre-align the title label in the BoxLayout column
        header.add(title);                   // Add the title label to the header
        header.add(Box.createVerticalStrut(4)); // 4px spacer between title and subtitle

        JLabel subtitle = UITheme.createLabel(
                firstRun ? "Create your master password to get started" : "Enter your master password to unlock", // Dynamic subtitle based on mode
                UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY); // Small muted text — secondary information
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT); // Centre-align the subtitle
        header.add(subtitle); // Add the subtitle to the header

        mainPanel.add(header, BorderLayout.NORTH); // Place the header panel in the NORTH (top) region of the BorderLayout

        // ── CENTER: Form Card ─────────────────────────────────────────────────
        JPanel cardWrapper = new JPanel(new BorderLayout()); // Wrapper panel provides margins around the card
        cardWrapper.setOpaque(false);                         // Transparent — shows the mainPanel's BG_PRIMARY background through
        cardWrapper.setBackground(UITheme.BG_PRIMARY);
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30)); // 20px top, 30px left/right, 10px bottom margin around the card

        JPanel card = UITheme.createCardPanel(); // Rounded dark card panel (BG_SECONDARY fill + BORDER_DEFAULT outline)
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); // Stack form fields vertically within the card

        // -- Master Password Label --
        JLabel masterLabel = UITheme.createLabel(
                firstRun ? "🔑  Create Master Password" : "🔑  Master Password", // Label text differs by mode
                UITheme.FONT_SMALL_BOLD, UITheme.TEXT_SECONDARY); // Small bold grey label above the input
        card.add(masterLabel);                    // Add label to card
        card.add(Box.createVerticalStrut(8));     // 8px gap between label and input field

        masterPasswordField = UITheme.createPasswordField("Enter master password…"); // Create the styled password field with placeholder text
        card.add(masterPasswordField); // Add the password field to the card

        // ── Strength Bar (only shown in first-run setup mode) ─────────────────
        if (firstRun) {
            card.add(Box.createVerticalStrut(8)); // 8px gap between the password field and the strength bar

            // JProgressBar used here as a simple colour-filled strength indicator (not a traditional loading bar)
            strengthBar = new JProgressBar(0, 3);                   // Range 0–3: matches getPasswordStrength() output (0=empty, 1=weak, 2=medium, 3=strong)
            strengthBar.setValue(0);                                 // Start at 0 (empty bar) since no password has been typed yet
            strengthBar.setStringPainted(false);                     // Don't paint the default percentage text (e.g. "67%") — we use strengthLabel instead
            strengthBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 6)); // Thin 6px height — looks like a slim indicator bar
            strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));   // Cap at 6px height in BoxLayout also
            strengthBar.setBackground(UITheme.BG_TERTIARY);         // Dark unfilled background (track) colour
            strengthBar.setForeground(UITheme.BORDER_DEFAULT);      // Default fill colour when empty (nearly invisible)
            strengthBar.setBorderPainted(false);                     // Remove the default JProgressBar border for a cleaner look
            strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);    // Align left in the BoxLayout column
            card.add(strengthBar);                                   // Add the strength bar to the card

            card.add(Box.createVerticalStrut(4)); // 4px gap between bar and label
            strengthLabel = UITheme.createLabel("", UITheme.FONT_SMALL, UITheme.TEXT_MUTED); // Empty label initially — updated dynamically as the user types
            card.add(strengthLabel); // Add the strength text label

            // ── Live Strength Update via DocumentListener ──────────────────────
            // DocumentListener fires on every insert, delete, or change to the field's text —
            // this is more reliable than KeyListener which misses paste operations.
            masterPasswordField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e)  { updateStrength(); } // Called when characters are TYPED or PASTED into the field
                public void removeUpdate(DocumentEvent e)  { updateStrength(); } // Called when characters are DELETED from the field
                public void changedUpdate(DocumentEvent e) { updateStrength(); } // Called for attribute changes (e.g. style); rarely fires for plain text fields
            });

            // ── Confirm Password Field (only in setup mode) ────────────────────
            card.add(Box.createVerticalStrut(16)); // 16px gap between master and confirm fields for visual separation

            JLabel confirmLabel = UITheme.createLabel("✓  Confirm Password", UITheme.FONT_SMALL_BOLD, UITheme.TEXT_SECONDARY); // Label above the confirm field
            card.add(confirmLabel);
            card.add(Box.createVerticalStrut(8)); // 8px gap between label and input

            confirmPasswordField = UITheme.createPasswordField("Re-enter master password…"); // Second password field with its own placeholder
            card.add(confirmPasswordField); // Add confirm field to the card
        }

        cardWrapper.add(card, BorderLayout.CENTER); // Place the card panel in the centre of the wrapper
        mainPanel.add(cardWrapper, BorderLayout.CENTER); // Place the card wrapper in the CENTER region of the main layout

        // ── SOUTH: Action Button + Footer ─────────────────────────────────────
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)); // Vertically stack button → footer text
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 30, 20, 30)); // Tight top margin, generous bottom padding

        // The primary action button — label and colour change based on mode
        actionButton = UITheme.createButton(
                firstRun ? "🔐  Create Vault" : "🔓  Unlock Vault", // Different emoji + label per mode
                UITheme.ACCENT_GREEN); // Green = positive/safe action (vault creation or unlock)
        actionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.BUTTON_HEIGHT)); // Stretch full width, fixed height
        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Centre within BoxLayout
        actionButton.addActionListener(e -> { // Lambda: anonymous ActionListener — called when the button is clicked
            if (firstRun) {
                createMasterPassword(); // First run: validate fields and hash + save the new master password
            } else {
                login(); // Returning user: verify the entered password against the stored hash
            }
        });
        bottomPanel.add(actionButton); // Add the action button to the bottom panel

        bottomPanel.add(Box.createVerticalStrut(16)); // 16px gap between button and footer text

        // Footer text — describes the security model in brief
        JLabel footer = UITheme.createLabel("AES-256 Encrypted  ·  Offline Vault  ·  Zero Knowledge",
                UITheme.FONT_SMALL, UITheme.TEXT_MUTED); // Muted tiny text — reassuring but non-intrusive
        footer.setAlignmentX(Component.CENTER_ALIGNMENT); // Centre below the button
        bottomPanel.add(footer); // Add footer label

        mainPanel.add(bottomPanel, BorderLayout.SOUTH); // Place bottom panel in the SOUTH region

        add(mainPanel);    // Add the fully assembled panel tree to the JFrame's content pane
        setVisible(true);  // Make the window visible — this triggers the first paint and renders all components
    }

    /**
     * updateStrength — Reads the current master password field content and updates
     * the strength bar colour/value and strength label text.
     *
     * Called by the DocumentListener every time the user types or deletes a character.
     */
    private void updateStrength() {
        String pw = new String(masterPasswordField.getPassword()); // Convert the char[] from JPasswordField to a String for the strength algorithm
        int strength = UITheme.getPasswordStrength(pw);            // Calculate strength score 0–3 based on length, case, digits, symbols
        strengthBar.setValue(strength);                            // Update the progress bar to show the new score (0 = empty, 3 = full bar)
        strengthBar.setForeground(UITheme.getStrengthColor(strength)); // Change bar colour: red/amber/green based on score
        strengthLabel.setText(UITheme.getStrengthLabel(strength)); // Update the text label: "", "Weak", "Medium", or "Strong"
        strengthLabel.setForeground(UITheme.getStrengthColor(strength)); // Match the label colour to the bar colour for visual consistency
    }

    // ── Action Methods ─────────────────────────────────────────────────────────

    /**
     * createMasterPassword — Validates the setup form and persists the new master password.
     *
     * Validation steps:
     *   1. Confirm field must not be empty
     *   2. Both password fields must match
     *   3. Password must meet complexity requirements (regex)
     *   4. Password must be at least 8 characters
     *
     * If valid: generates a salt, hashes the password with PBKDF2, saves hash+salt to DB,
     * initialises the AES encryption key, shows a success toast, then opens PasswordManagerGUI.
     */
    private void createMasterPassword() {
        if (confirmPasswordField.getPassword().length == 0) { // Guard: user must fill in the confirm field before proceeding
            showError("Confirm password is required");
            return; // Stop execution — do not proceed to hashing
        }

        try {
            String password = new String(masterPasswordField.getPassword()); // Convert char[] to String for comparison and validation
            String confirm  = new String(confirmPasswordField.getPassword()); // Convert the confirm field's char[] to String

            if (!password.equals(confirm)) { // The two fields must be identical — prevents typos in the master password
                showError("Passwords do not match!");
                return;
            }

            // Regex validation: password must contain at least one uppercase, one lowercase, one digit, and one symbol
            if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=]).{8,}$")) {
                // (?=.*[A-Z])   = lookahead: must contain at least one uppercase letter
                // (?=.*[a-z])   = lookahead: must contain at least one lowercase letter
                // (?=.*\\d)     = lookahead: must contain at least one digit
                // (?=.*[@#$%^&+=]) = lookahead: must contain at least one allowed symbol
                // .{8,}         = overall length must be at least 8 characters
                showError("Password must contain:\n• Uppercase letter\n• Lowercase letter\n• Number\n• Symbol (@#$%^&+=)");
                return;
            }

            if (password.length() < 8) { // Redundant double-check — the regex above already enforces length, but this gives a clearer error message
                showError("Master password must be at least 8 characters.");
                return;
            }

            byte[] salt = PasswordHasher.generateSalt();                // Generate a cryptographically random 16-byte salt using SecureRandom
            String hash = PasswordHasher.hashPassword(password, salt);  // Derive the PBKDF2 hash of the password using the fresh salt (65536 iterations, SHA-256)

            DataBaseConnection.saveMasterPassword(
                    hash,
                    Base64.getEncoder().encodeToString(salt)); // Encode the raw salt bytes as Base64 so it can be stored as TEXT in SQLite

            EncryptionService.initializeKey(password, salt); // Derive the AES-256 encryption key from the master password and store it in memory for the session

            UITheme.showToast(getRootPane(), "✓ Vault created successfully!", UITheme.ACCENT_GREEN); // Show a green success toast in the window

            // Brief delay so the user can read the toast, then transition to the main vault screen
            Timer t = new Timer(800, ev -> { // Swing Timer fires after 800ms on the EDT — safe for UI operations
                dispose();            // Close and destroy this LoginUI window
                new PasswordManagerGUI(); // Open the main vault dashboard window
            });
            t.setRepeats(false); // One-shot timer — fires once at 800ms, then stops
            t.start();           // Start the countdown

        } catch (Exception e) {
            showError("Error: " + e.getMessage()); // Show any unexpected exception message to the user
            e.printStackTrace();                    // Also print the full stack trace to stderr for debugging
        }
    }

    /**
     * login — Verifies the entered master password against the stored PBKDF2 hash.
     *
     * Process:
     *   1. Read the stored hash and salt from the database (already loaded in masterData)
     *   2. Re-derive the PBKDF2 hash of the entered password using the stored salt
     *   3. Compare the derived hash to the stored hash
     *   4. If they match, initialize the AES key and open the vault dashboard
     *   5. If not, show an error and clear the password field
     */
    private void login() {
        try {
            String inputPassword = new String(masterPasswordField.getPassword()); // Get the password the user typed, converting char[] to String for processing

            // Read the stored hash and salt from the masterData array loaded in the constructor
            String storedHash = masterData[0];                          // Index 0 = master_hash column from the settings table
            byte[] salt = Base64.getDecoder().decode(masterData[1]);    // Index 1 = salt column (Base64) → decode back to raw byte[] for PBKDF2 re-derivation

            String inputHash = PasswordHasher.hashPassword(inputPassword, salt); // Re-derive the PBKDF2 hash of the input password using the SAME salt as stored; same password → same hash

            if (inputHash.equals(storedHash)) { // Compare hashes as Strings (constant-time equals would be more secure but Java's String.equals is acceptable here)
                failedAttempts = 0; // reset attempts on success
                EncryptionService.initializeKey(inputPassword, salt); // Derive the AES-256 key from the verified password — now the vault can encrypt/decrypt

                UITheme.showToast(getRootPane(), "✓ Login successful!", UITheme.ACCENT_GREEN); // Show green toast confirming successful authentication

                Timer t = new Timer(800, ev -> { // 800ms delay so the user can see the success toast before the window transitions
                    dispose();            // Close and destroy the LoginUI window
                    new PasswordManagerGUI(); // Open the main vault dashboard
                });
                t.setRepeats(false); // Fire once only
                t.start();

            } else {
                failedAttempts++;
                if (failedAttempts >= 3) {
                    showError("Too many incorrect attempts.\nLogin locked for 30 seconds.");
                    actionButton.setEnabled(false);
                    masterPasswordField.setEnabled(false);
                    masterPasswordField.setText("");
                    
                    Timer lockTimer = new Timer(30000, evt -> {
                        failedAttempts = 0;
                        actionButton.setEnabled(true);
                        masterPasswordField.setEnabled(true);
                        UITheme.showToast(getRootPane(), "🔓 Login unlocked.", UITheme.ACCENT_GREEN);
                    });
                    lockTimer.setRepeats(false);
                    lockTimer.start();
                } else {
                    showError("Incorrect Master Password.\nAttempts left: " + (3 - failedAttempts)); // Password mismatch — inform the user
                    masterPasswordField.setText(""); // Clear the password field so the user can try again without manually selecting and deleting
                }
            }

        } catch (Exception e) {
            showError("Error: " + e.getMessage()); // Show unexpected exception details
            e.printStackTrace();                    // Print full stack trace for debugging
        }
    }

    /**
     * showError — Displays a modal error dialog with the given message.
     *
     * JOptionPane.showMessageDialog blocks the current thread until the user clicks OK.
     *
     * @param message The error message to display (can be multi-line using \n)
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Authentication Failed", JOptionPane.ERROR_MESSAGE);
        // 'this' = parent frame (centres the dialog)
        // ERROR_MESSAGE = shows the platform's error icon (red X on Windows) in the dialog
    }
}
