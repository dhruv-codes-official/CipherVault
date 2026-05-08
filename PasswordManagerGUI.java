package com.dhruv.ui; // Declares the package for UI components

import com.dhruv.db.DataBaseConnection; // Imports DataBaseConnection to interact with the SQLite database

import javax.swing.*; // Imports all Swing components (JFrame, JPanel, JButton, etc.) for building the graphical interface
import java.awt.*; // Imports Abstract Window Toolkit classes (Color, Font, LayoutManagers) for UI styling and layout
import java.awt.event.*; // Imports event listener interfaces (ActionListener, MouseMotionAdapter) to handle user inputs
import java.util.List; // Imports the java.util.List interface to handle collections of PasswordEntry objects
import java.util.Arrays; // Imports Arrays utility class to safely handle and clear character arrays (like passwords)
import java.awt.datatransfer.StringSelection; // Allows packaging a string to be placed on the system clipboard
import java.awt.datatransfer.Clipboard; // Provides access to the system clipboard for copying passwords


public class PasswordManagerGUI extends JFrame { // Main GUI class extending JFrame to create a window

    private JTextField websiteField; // Text field where the user inputs the website URL or name
    private JTextField usernameField; // Text field where the user inputs their username or email
    private JPasswordField passwordField; // Password field where the user inputs the password; characters are masked
    private JComboBox<String> categoryBox; // Dropdown box allowing the user to select a category for the credential
    private JTextArea outputArea; // Multi-line text area to display system messages, search results, and logs
    private Timer autoLockTimer; // Timer object that counts down inactivity before locking the application
    private final int INACTIVITY_TIMEOUT = 30000; // Constant defining the inactivity timeout duration (30 seconds in milliseconds)

public PasswordManagerGUI() { // Constructor method called when the GUI window is created
    setTitle("Password Manager"); // Sets the title bar text of the application window to "Password Manager"
    setSize(600, 500); // Sets the default physical dimensions of the window to 600px width and 500px height
    setLocationRelativeTo(null); // Centers the window on the user's screen upon launch
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Tells the application to terminate completely when the window is closed

    startAutoLockTimer(); // Initializes and starts the inactivity auto-lock countdown timer

    addMouseMotionListener(new MouseMotionAdapter() { // Attaches a mouse motion listener to the main frame to track pointer movement
        public void mouseMoved(MouseEvent e) { // Called every time the mouse moves over the window
            resetAutoLockTimer(); // Resets the 30-second countdown timer because activity was detected
        }
    });

    addKeyListener(new KeyAdapter() { // Attaches a keyboard listener to the main frame to track key presses
        public void keyPressed(KeyEvent e) { // Called every time a key is pressed down
            resetAutoLockTimer(); // Resets the 30-second countdown timer because typing activity was detected
        }
    });

    // Also using AWT event listener for children if needed
    Toolkit.getDefaultToolkit().addAWTEventListener(event -> resetAutoLockTimer(), // Global event listener capturing UI events across all components
        AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK); // Listens specifically for keystrokes, mouse movement, and clicks

    JPanel mainPanel = new JPanel(); // Creates the main background panel to hold all other UI components
    mainPanel.setLayout(new BorderLayout(10, 10)); // Sets a BorderLayout with 10px horizontal/vertical gaps between regions
    mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Adds a 15-pixel empty padding around the inner edges of mainPanel

    // Form Panel
    JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10)); // Creates a grid layout panel with 4 rows, 2 columns, and 10px gaps for form fields

    formPanel.add(new JLabel("Website:")); // Adds a static text label identifying the website input field
    websiteField = new JTextField(); // Instantiates the text input field for the website
    formPanel.add(websiteField); // Adds the website text field to the grid form layout

    formPanel.add(new JLabel("Username:")); // Adds a static text label identifying the username input field
    usernameField = new JTextField(); // Instantiates the text input field for the username
    formPanel.add(usernameField); // Adds the username text field to the grid form layout

    formPanel.add(new JLabel("Password:")); // Adds a static text label identifying the password input field
    JPanel passwordWrapper = new JPanel(new BorderLayout()); // Creates a sub-panel with BorderLayout to group the password field and the 'eye' button
    passwordField = new JPasswordField(); // Instantiates the password input field where characters are masked
    passwordField.setEchoChar('•'); // Sets the replacement character to a solid bullet point for masked passwords
    passwordWrapper.add(passwordField, BorderLayout.CENTER); // Places the password field in the center, allowing it to stretch
    
    JToggleButton togglePwdBtn = new JToggleButton("👁"); // Creates a toggleable button with an eye icon for revealing passwords
    togglePwdBtn.setFocusable(false); // Prevents the eye button from receiving keyboard focus (avoids visual dotted borders when clicked)
    togglePwdBtn.addActionListener(e -> { // Lambda function triggered when the user clicks the eye toggle button
        if (togglePwdBtn.isSelected()) { // Checks if the toggle button is pressed down/active
            passwordField.setEchoChar((char) 0); // Disables the character mask (sets echo char to null), revealing plaintext
        } else { // If the toggle button is unpressed/inactive
            passwordField.setEchoChar('•'); // Re-enables the mask by restoring the solid bullet point character
        }
    });
    passwordWrapper.add(togglePwdBtn, BorderLayout.EAST); // Aligns the toggle button to the right side of the password field
    formPanel.add(passwordWrapper); // Adds the entire password wrapper (field + button) to the form grid layout

    formPanel.add(new JLabel("Category:")); // Adds a static text label identifying the category dropdown
    categoryBox = new JComboBox<>(new String[]{"General", "Social", "Banking", "Work"}); // Creates a drop-down menu with predefined category options
    formPanel.add(categoryBox); // Adds the category dropdown box to the form grid layout

    mainPanel.add(formPanel, BorderLayout.NORTH); // Places the completed 4x2 form panel at the top (North) of the main UI

    // Output Area
    outputArea = new JTextArea(); // Instantiates a multi-line text area to display feedback and results to the user
    outputArea.setEditable(false); // Locks the text area so users cannot manually type into the readout space
    outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Sets a fixed-width font (Monospaced) so column-alignment looks neat in logs
    
    mainPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER); // Wraps the output area in a scroll pane and puts it in the center area

    // Buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10)); // Creates a panel to house the action buttons, horizontally centered with gaps

    JButton addButton = new JButton("Add"); // Button labeled "Add" for saving a new credential
    JButton updateButton = new JButton("Update"); // Button labeled "Update" to modify an existing credential
    JButton searchButton = new JButton("Search"); // Button labeled "Search" to find a credential
    JButton deleteButton = new JButton("Delete"); // Button labeled "Delete" to remove a credential
    JButton viewButton = new JButton("View All"); // Button labeled "View All" to list all stored credentials
    JButton copyButton = new JButton("Copy"); // Button labeled "Copy" to copy a password to the clipboard

    buttonPanel.add(addButton); // Adds 'Add' button to the flow layout
    buttonPanel.add(updateButton); // Adds 'Update' button to the flow layout
    buttonPanel.add(searchButton); // Adds 'Search' button to the flow layout
    buttonPanel.add(deleteButton); // Adds 'Delete' button to the flow layout
    buttonPanel.add(viewButton); // Adds 'View All' button to the flow layout
    buttonPanel.add(copyButton); // Adds 'Copy' button to the flow layout

    // Button Actions

    addButton.addActionListener(e -> { // Lambda block triggered when the 'Add' button is clicked
        char[] pwd = passwordField.getPassword(); // Extracts the password securely into a character array (avoids String pooling in JVM memory)
        String pwdStr = new String(pwd); // Converts the character array into a String (Database handler accepts string)
        DataBaseConnection.insertPassword( // Calls the database handler to run the SQL INSERT operation
                websiteField.getText(), // Passes the typed website value
                usernameField.getText(), // Passes the typed username value
                pwdStr, // Passes the password string to be encrypted and stored
                (String) categoryBox.getSelectedItem() // Passes the chosen category from the dropdown 
        );
        Arrays.fill(pwd, '\0'); // Crucial Security Step: overwrites the char array with null bytes to clear it from active RAM immediately
        passwordField.setText(""); // Clears the visual UI password field
        outputArea.setText("Password added successfully."); // Displays a success message to the user in the output area
    });
    
    updateButton.addActionListener(e -> { // Triggered when 'Update' button is clicked
        char[] pwd = passwordField.getPassword(); // Securely fetches the new password from the UI field
        String pwdStr = new String(pwd); // Converts char[] to String for the DB API
        DataBaseConnection.updatePassword( // Calls the database handler to execute an SQL UPDATE command
                websiteField.getText(), // Uses website field as part of the primary key equivalent to locate the record
                usernameField.getText(), // Uses username as part of the key
                pwdStr, // The new password to replace the old one
                (String) categoryBox.getSelectedItem() // The new category
        );
        Arrays.fill(pwd, '\0'); // Erases raw password data from memory for security
        passwordField.setText(""); // Clears the UI text field to hide the updated password
        outputArea.setText("Password updated successfully."); // Displays success confirmation to the user
    });

    copyButton.addActionListener(e -> { // Triggered when 'Copy' is clicked
        String site = websiteField.getText(); // Reads target website from UI
        String user = usernameField.getText(); // Reads target username from UI
        if(site.isEmpty() || user.isEmpty()){ // Checks if either identifying field is left blank
            outputArea.setText("Please enter Website and Username to copy."); // Shows error if they are missing
            return; // Escapes the method early so it doesn't attempt an empty lookup
        }
        List<DataBaseConnection.PasswordEntry> entries = DataBaseConnection.getAllPasswordEntries(); // Retrieves ALL decrypted passwords from DB (Caution: heavy operation)
        for(DataBaseConnection.PasswordEntry entry : entries) { // Iterates over every retrieved credential object
            if(entry.website.equalsIgnoreCase(site) && entry.username.equalsIgnoreCase(user)) { // Case-insensitive match on both Website and Username
                StringSelection selection = new StringSelection(entry.password); // Wraps the matched plaintext password into a clipboard-compatible format
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard(); // Gets a reference to the Operating System's clipboard
                cb.setContents(selection, null); // Pushes the password onto the clipboard so the user can paste (Ctrl+V) it elsewhere
                outputArea.setText("Password copied! Clipboard will be cleared in 15 seconds."); // Alerts the user

                Timer wipeTimer = new Timer(15000, evt -> { // Creates a distinct, one-time timer set to trigger after 15,000ms (15 seconds)
                    try { // Wrapped in try-catch to avoid crashing if OS denies clipboard access
                        cb.setContents(new StringSelection(""), null); // Overwrites the system clipboard with an empty string, wiping the password
                        if(outputArea != null) { // Null check in case the UI was closed in the meantime
                            outputArea.append("\n[Auto-Clear] Clipboard wiped for security."); // Appends confirmation that the clipboard is clean
                        }
                    } catch (Exception ignored) {} // Ignores any OS-level errors during the wipe
                });
                wipeTimer.setRepeats(false); // Ensures the clipboard wipe only happens exactly once, not every 15s indefinitely
                wipeTimer.start(); // Starts the 15s countdown for the wipe trigger
                return; // Exits the method since the password was found and processed
            }
        }
        outputArea.setText("Record not found to copy."); // Fallback message if the loop finishes without finding a matching Website/Username combo
    });

searchButton.addActionListener(e -> { // Action for the 'Search' button
    String result = DataBaseConnection.searchPassword( // Calls DB method to lookup a specific record or partial matches
            websiteField.getText(), // Gets search query from website UI
            usernameField.getText() // Gets search query from username UI
    );
    outputArea.setText(result); // Injects the formatted search results string into the JTextArea
});

deleteButton.addActionListener(e -> { // Action for the 'Delete' button
    DataBaseConnection.deletePassword( // Calls DB method to execute a SQL DELETE statement
            websiteField.getText(), // Provides site name to delete
            usernameField.getText() // Provides username to delete
    );
    outputArea.setText("Password deleted successfully."); // Replaces the output log with a success message
});

    viewButton.addActionListener(e -> { // Action for the 'View All' button
        String result = DataBaseConnection.viewAllPasswords(); // Calls DB method to fetch everything and compile it into a single formatted string
        outputArea.setText(result); // Prints the massive multiline string of all credentials into the UI area
    });

    JPanel southWrapper = new JPanel(new BorderLayout()); // Creates a container panel for the south region to stack button sets vertically
    southWrapper.add(buttonPanel, BorderLayout.CENTER); // Places the main CRUD button panel in the center of the south wrapper

    JPanel bottomActionPanel = new JPanel(new FlowLayout()); // Creates another sub-panel for secondary bottom actions
    JButton analyticsBtn = new JButton("Analytics"); // Creates button to trigger basic password health checks
    analyticsBtn.addActionListener(e -> { // Defines behavior for analytics click
        List<DataBaseConnection.PasswordEntry> entries = DataBaseConnection.getAllPasswordEntries(); // Fetches all plain-text copies into RAM
        long weakCount = entries.stream() // Converts list to a Java Stream to cleanly run functional operations
            .filter(ent -> UITheme.getPasswordStrength(ent.password) < 2) // Filters stream to ONLY keep passwords scoring less than 2/4 strength
            .count(); // Counts how many weak passwords made it past the filter
        outputArea.setText("=== Password Analytics ===\n"); // Starts rendering report header
        outputArea.append("Total Passwords: " + entries.size() + "\n"); // Shows total stored
        outputArea.append("Weak Passwords: " + weakCount + "\n"); // Shows total deemed 'weak'
        if (weakCount > 0) { // If there is at least one weak password
            outputArea.append("Recommendation: Update your weak passwords for better security."); // Displays a warning suggestion
        }
    });
    
    JButton changeMasterBtn = new JButton("Change Master Password"); // Button to swap out the encryption master key
    changeMasterBtn.addActionListener(e -> { // Action definition
        new ChangeMasterPasswordDialog(PasswordManagerGUI.this).setVisible(true); // Pops up a separate JDialog modal over the main frame, passing current instance as parent
    });
    
    bottomActionPanel.add(analyticsBtn); // Nests analytics button into secondary bar
    bottomActionPanel.add(changeMasterBtn); // Nests change-master button into secondary bar

    southWrapper.add(bottomActionPanel, BorderLayout.SOUTH); // Stacks secondary actions under the main CRUD buttons
    mainPanel.add(southWrapper, BorderLayout.SOUTH); // Finally places the massive nested south rig onto the bottom of the main UI

    add(mainPanel); // Adds the entire assembled main panel onto the JFrame's ContentPane
    setVisible(true); // Commands the OS window manager to render the JFrame on the monitor
}

    private void startAutoLockTimer() { // Private utility method to boot the AFK timer
        autoLockTimer = new Timer(INACTIVITY_TIMEOUT, e -> { // Creates timer initialized with 30,000ms delay. Trigger mechanism runs when delay elapses.
            dispose(); // Destroys the current window (vault) instance, freeing memory for its components
            new LoginUI().setVisible(true); // Instantiates and spawns a brand new Login window, essentially "locking" the user out
        });
        autoLockTimer.setRepeats(false); // timer expires after one run instead of looping infinitely
        autoLockTimer.start(); // Begins ticking down
    }

    private void resetAutoLockTimer() { // Triggers every time user generates input (keys/mouse)
        if (autoLockTimer != null) { // Failsafe check ensuring timer isn't null
            autoLockTimer.restart(); // Natively stops and resets countdown to 0/30000ms
        }
    }

    public static void main(String[] args) { // Entry point for running the GUI directly for debug tests
        DataBaseConnection.initializeDatabase(); // Ensures the DB table framework exists before rendering UI
        new PasswordManagerGUI(); // Spawns the main window
    }
}
