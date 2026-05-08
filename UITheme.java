package com.dhruv.ui; // Declares this file belongs to the 'ui' package — all Swing GUI classes live here

import javax.swing.*;                    // Imports all Swing components: JLabel, JTextField, JButton, JProgressBar, JWindow, JPanel, Timer, etc.
import javax.swing.border.AbstractBorder; // Abstract base class for implementing custom border painting — extended by RoundedBorder
import java.awt.*;                        // Imports AWT classes: Color, Font, Cursor, Graphics, Graphics2D, Insets, Dimension, GradientPaint, etc.
import java.awt.event.*;                  // Imports AWT event classes: FocusAdapter, FocusEvent, MouseAdapter, MouseEvent — used for hover and focus effects
import java.awt.geom.RoundRectangle2D;   // Provides the 2D rounded rectangle shape used when custom-painting buttons and card panels

/**
 * UITheme — Centralised design system for the entire Swing application.
 *
 * This class follows the "constants + factory methods" pattern:
 *   - Static final Color/Font/int constants define the design tokens (palette, typography, sizes)
 *   - Static factory methods (createLabel, createTextField, etc.) produce pre-styled Swing components
 *   - Inner classes (GradientPanel, RoundedBorder) extend Swing classes with custom rendering
 *
 * Using a single UITheme class ensures visual consistency across LoginUI and PasswordManagerGUI
 * — change one constant here and it updates every screen automatically.
 */
public class UITheme {

    // ── Color Palette: Ultra-Dark Charcoal / Slate with Blue Undertones ──────
    public static final Color BG_PRIMARY     = new Color(8, 12, 18);   // #080C12 — deepest background; near-black charcoal (used for JFrame background)
    public static final Color BG_SECONDARY   = new Color(14, 19, 25);  // #0E1319 — card/panel background; slightly lighter than primary to create depth
    public static final Color BG_TERTIARY    = new Color(21, 27, 35);  // #151B23 — input field background; lightest background level in the hierarchy
    public static final Color BORDER_DEFAULT = new Color(30, 38, 50);  // #1E2632 — very subtle blue-grey border; defines edges without being harsh
    public static final Color BORDER_FOCUS   = new Color(70, 130, 200);// #4682C8 — muted steel blue; highlights focused input fields

    public static final Color ACCENT_BLUE    = new Color(70, 140, 210);  // #468CD2 — muted steel blue; used for 'Search', 'View All', and table headers
    public static final Color ACCENT_GREEN   = new Color(50, 160, 70);   // #32A046 — deep forest green; used for 'Add', 'Create Vault', and success toasts
    public static final Color ACCENT_RED     = new Color(200, 65, 58);   // #C8413A — dark crimson; used for 'Delete', 'Lock', and error feedback
    public static final Color ACCENT_PURPLE  = new Color(140, 110, 200); // #8C6EC8 — muted lavender; used for 'Generate Password' button
    public static final Color ACCENT_ORANGE  = new Color(180, 130, 30);  // #B4821E — deep amber; used for 'Medium' password strength indicator

    public static final Color TEXT_PRIMARY   = new Color(200, 210, 220); // #C8D2DC — cool grey-white; main readable text colour
    public static final Color TEXT_SECONDARY = new Color(110, 120, 135); // #6E7887 — blue-grey muted; used for labels and subtitles
    public static final Color TEXT_MUTED     = new Color(70, 80, 95);    // #46505F — darkest visible text; used for input placeholder text

    public static final Color GRADIENT_START = new Color(10, 15, 22);  // Near-black starting colour for the header gradient (left side)
    public static final Color GRADIENT_END   = new Color(18, 28, 42);  // Very dark navy ending colour for the header gradient (right side)

    // ── Typography: Segoe UI for UI text, Consolas for monospaced output ──────
    public static final Font FONT_HEADING_LG  = new Font("Segoe UI", Font.BOLD, 26);  // Large bold heading — used for "Secure Vault" title in LoginUI
    public static final Font FONT_HEADING     = new Font("Segoe UI", Font.BOLD, 18);  // Standard bold heading — used for "Password Vault" in the header
    public static final Font FONT_BODY        = new Font("Segoe UI", Font.PLAIN, 14); // Regular body text — used for input fields and general labels
    public static final Font FONT_BODY_BOLD   = new Font("Segoe UI", Font.BOLD, 14);  // Bold body text — used for section titles like "Add / Search Credentials"
    public static final Font FONT_SMALL       = new Font("Segoe UI", Font.PLAIN, 12); // Small text — used for subtitles and field labels
    public static final Font FONT_SMALL_BOLD  = new Font("Segoe UI", Font.BOLD, 12);  // Small bold — used for field label headers like "Website", "Username"
    public static final Font FONT_MONO        = new Font("Consolas", Font.PLAIN, 13); // Monospaced font — used in the output JTextArea for aligned credential display
    public static final Font FONT_BUTTON      = new Font("Segoe UI", Font.BOLD, 13);  // Bold button text — ensures button labels are visually prominent

    // ── Dimensions: Standard sizes used across all components ────────────────
    public static final int CORNER_RADIUS = 12;  // Rounded corner radius in pixels applied to cards, inputs, and buttons for a modern "pill" look
    public static final int INPUT_HEIGHT  = 44;  // Fixed height in pixels for all input fields — ensures consistent vertical rhythm across the form
    public static final int BUTTON_HEIGHT = 42;  // Fixed height in pixels for all action buttons — slightly shorter than inputs for visual hierarchy

    // ── Component Factories ───────────────────────────────────────────────────

    /**
     * createLabel — Creates a pre-styled JLabel with the specified text, font, and colour.
     *
     * @param text  The label text to display
     * @param font  The Font to apply (use UITheme.FONT_* constants)
     * @param color The foreground text colour (use UITheme.TEXT_* or ACCENT_* constants)
     * @return      A fully styled JLabel ready to be added to a panel
     */
    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);                      // Create a standard Swing label with the given text
        label.setFont(font);                                  // Apply the specified font (size, weight, family)
        label.setForeground(color);                           // Set the text colour
        label.setAlignmentX(Component.LEFT_ALIGNMENT);        // Align to the left inside BoxLayout containers (prevents centering which is the BoxLayout default)
        return label;                                         // Return the configured label to the caller
    }

    /**
     * createTextField — Creates a styled JTextField with placeholder text support.
     *
     * Swing's JTextField does not natively support placeholder text, so paintComponent
     * is overridden to draw the placeholder manually when the field is empty and unfocused.
     *
     * @param placeholder The grey hint text shown when the field is empty (e.g. "e.g. github.com")
     * @return            A fully styled text field with placeholder support
     */
    public static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField() {           // Anonymous subclass of JTextField that overrides rendering for placeholder support
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);                // Call the parent paintComponent first — draws the actual text input content
                if (getText().isEmpty() && !hasFocus()) { // Only draw placeholder when the field is empty AND not focused (typing hides it)
                    Graphics2D g2 = (Graphics2D) g.create(); // Create a scratch Graphics2D context; .create() prevents modifying the original graphics state
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Enable anti-aliasing for smooth text rendering
                    g2.setColor(TEXT_MUTED);               // Use the darkest muted text colour for placeholder (barely visible — intentional)
                    g2.setFont(FONT_BODY);                 // Use the standard body font for the placeholder
                    Insets insets = getInsets();            // Get the field's current padding insets to correctly position the placeholder text
                    g2.drawString(placeholder, insets.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2); // Draw placeholder text vertically centred inside the field
                    g2.dispose();                          // Dispose the scratch graphics context to release system resources
                }
            }
        };
        styleInputField(field); // Apply the common dark styling (background, border, size, focus listener) to this field
        return field;           // Return the fully styled text field
    }

    /**
     * createPasswordField — Creates a styled JPasswordField with placeholder text support.
     *
     * Identical to createTextField but uses JPasswordField, which masks characters as
     * bullet points (•) for password entry while still supporting placeholder text.
     *
     * @param placeholder The hint text displayed when the field is empty and unfocused
     * @return            A fully styled password field with placeholder support
     */
    public static JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField() { // Anonymous subclass of JPasswordField for placeholder support
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);                    // Draw the actual masked input content first
                if (getPassword().length == 0 && !hasFocus()) { // Show placeholder only when field is empty (getPassword() returns char[]) and unfocused
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Smooth text rendering
                    g2.setColor(TEXT_MUTED);                // Muted colour for barely-visible placeholder
                    g2.setFont(FONT_BODY);
                    Insets insets = getInsets();
                    g2.drawString(placeholder, insets.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2); // Centre placeholder vertically
                    g2.dispose();
                }
            }
        };
        styleInputField(field); // Apply common input styling (same method works since JPasswordField extends JTextField)
        return field;
    }

    /**
     * styleInputField — Applies the shared dark styling and focus-glow behaviour to any JTextField.
     *
     * Private helper used by both createTextField and createPasswordField to avoid duplicating code.
     * Focus listeners dynamically swap the border colour between BORDER_DEFAULT and BORDER_FOCUS.
     *
     * @param field The JTextField (or JPasswordField) to style in-place
     */
    private static void styleInputField(JTextField field) {
        field.setFont(FONT_BODY);                // Apply the standard body font to display typed text
        field.setBackground(BG_TERTIARY);        // Dark input background — clearly distinct from the card background (BG_SECONDARY)
        field.setForeground(TEXT_PRIMARY);       // Light text colour so typed characters are readable on the dark background
        field.setCaretColor(ACCENT_BLUE);        // Set the text cursor (caret) to blue — a subtle branding touch that's also more visible than white
        field.setOpaque(true);                   // true means the background colour is actually painted (false would make it transparent)
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CORNER_RADIUS, BORDER_DEFAULT), // Outer: custom rounded border in the default subtle colour
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));  // Inner: padding inside the border so text doesn't touch the edges
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, INPUT_HEIGHT)); // Prevent BoxLayout from making the field taller than INPUT_HEIGHT
        field.setPreferredSize(new Dimension(200, INPUT_HEIGHT));              // Preferred size: auto-width, fixed height of 44px
        field.setAlignmentX(Component.LEFT_ALIGNMENT);                         // Align left inside BoxLayout so it doesn't float to centre

        // Focus Listener — changes border colour to indicate the active input field
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { // Called when the user clicks into this field or tabs to it
                field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(CORNER_RADIUS, ACCENT_BLUE),         // Swap to blue focus glow border
                        BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                field.repaint(); // Force immediate repaint so the border update is visible instantly
            }

            @Override
            public void focusLost(FocusEvent e) { // Called when the user clicks away or tabs out of this field
                field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(CORNER_RADIUS, BORDER_DEFAULT),      // Revert to the default subtle border
                        BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                field.repaint(); // Force repaint to show the restored default border
            }
        });
    }

    /**
     * createButton — Creates a rounded gradient JButton with hover and press animations.
     *
     * Swing buttons are plain rectangles by default, so paintComponent is overridden here
     * to draw: (1) a solid base colour fill, (2) a subtle top-light gradient overlay,
     * and (3) a semi-transparent white hover/press overlay that intensifies on interaction.
     *
     * @param text      The button label text (may include emoji, e.g. "➕  Add")
     * @param baseColor The primary background colour of the button (use UITheme.ACCENT_* constants)
     * @return          A fully styled, interactive JButton
     */
    public static JButton createButton(String text, Color baseColor) {
        JButton button = new JButton(text) {       // Anonymous subclass of JButton for custom painting
            private float hoverAlpha = 0f;         // Instance variable tracking the current hover overlay opacity (0 = no hover, 0.15 = hovered, 0.3 = pressed)

            {   // Instance initialiser block — runs when each button is created
                setContentAreaFilled(false);        // Disable Swing's default button background fill so our custom paintComponent controls all rendering
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { // Mouse cursor enters the button area
                        hoverAlpha = 0.15f;         // Set a light white overlay (15% opacity) to indicate hover
                        repaint();                  // Trigger repaint so the new hoverAlpha is drawn
                    }

                    @Override
                    public void mouseExited(MouseEvent e) { // Mouse cursor leaves the button area
                        hoverAlpha = 0f;            // Remove the overlay — button returns to its base colour
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) { // Mouse button is held down
                        hoverAlpha = 0.3f;          // Stronger overlay (30% opacity) simulates a "pressed" dimming effect
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) { // Mouse button is released (but still hovered)
                        hoverAlpha = 0.15f;         // Return to hover state (15%) since the cursor is still over the button
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); // Scratch Graphics2D context to avoid affecting other components' rendering
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Anti-aliasing for smooth rounded corners

                // Define the rounded rectangle shape that clips all our drawing operations
                RoundRectangle2D.Float rr = new RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS); // Full-size rounded rect with 12px corner radius

                // Layer 1 — Base colour fill (solid flat colour matching the accent)
                g2.setColor(baseColor);         // Set the paint to the button's accent colour (e.g. ACCENT_GREEN)
                g2.fill(rr);                    // Fill the rounded rectangle with the base colour

                // Layer 2 — Subtle top-light gradient overlay (lighter top, darker bottom)
                GradientPaint gp = new GradientPaint(0, 0,
                        new Color(255, 255, 255, 30),  // Top: 11% white — simulates light hitting the top of the button
                        0, getHeight(),
                        new Color(0, 0, 0, 30));        // Bottom: 11% black — simulates shadow at the bottom
                g2.setPaint(gp);                // Set the gradient as the current paint
                g2.fill(rr);                    // Paint the gradient over the base colour

                // Layer 3 — Dynamic hover/press overlay (only drawn when mouse is over the button)
                if (hoverAlpha > 0) {
                    g2.setColor(new Color(255, 255, 255, (int) (hoverAlpha * 255))); // White overlay with opacity scaled by hoverAlpha (0–1 → 0–255)
                    g2.fill(rr); // Paint the semi-transparent white over the button to brighten it on hover
                }

                g2.dispose();           // Release the scratch Graphics2D context
                super.paintComponent(g); // Let Swing draw the button text and icon on top of our custom background
            }
        };

        button.setFont(FONT_BUTTON);                              // Bold 13pt font for button labels
        button.setForeground(Color.WHITE);                        // White text — high contrast on all accent colours
        button.setFocusPainted(false);                            // Disable Swing's default dotted focus outline (looks bad on custom painted buttons)
        button.setBorderPainted(false);                           // Disable Swing's default border — our rounded shape IS the border
        button.setOpaque(false);                                  // false = the component doesn't fill its rectangular bounds (rounded shape handles painting)
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));         // Show a pointer/hand cursor on hover — universal UI convention for clickable elements
        button.setPreferredSize(new Dimension(140, BUTTON_HEIGHT)); // Default preferred size: 140px wide, 42px tall
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Internal padding so text doesn't touch the button edges

        return button; // Return the fully configured custom button
    }

    /**
     * createCardPanel — Creates a rounded dark panel used as a card container.
     *
     * The panel's paintComponent is overridden to draw a rounded rectangle
     * with BG_SECONDARY fill and a BORDER_DEFAULT outline, instead of
     * Swing's default rectangular opaque panel.
     *
     * @return An empty JPanel with card-style rounded styling applied
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel() { // Anonymous JPanel subclass for custom rounded painting
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); // Scratch context
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Smooth edges
                g2.setColor(BG_SECONDARY);  // Fill colour — slightly lighter than the window background to create a floating card effect
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS); // Fill the entire panel bounds with a rounded rectangle
                g2.setColor(BORDER_DEFAULT); // Switch to the subtle border colour
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS); // Draw the border outline; -1 ensures the stroke stays within the component bounds
                g2.dispose(); // Release the scratch context
            }
        };
        panel.setOpaque(false);  // false = the rectangular JPanel background is not painted — only our custom rounded rectangle is drawn
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // 20px internal padding on all sides so child components have breathing room
        return panel; // Return the card panel ready for child components to be added
    }

    // ── Inner Class: GradientPanel ────────────────────────────────────────────

    /**
     * GradientPanel — A JPanel that paints a horizontal linear gradient background.
     *
     * Used for the header bar in both LoginUI and PasswordManagerGUI to add a
     * depth effect (slightly different shade on the left vs right).
     */
    public static class GradientPanel extends JPanel {
        private final Color startColor; // The colour at the left (start) of the gradient
        private final Color endColor;   // The colour at the right (end) of the gradient

        /** Constructor with explicit start and end colours */
        public GradientPanel(Color start, Color end) {
            this.startColor = start; // Store the start colour for use in paintComponent
            this.endColor = end;     // Store the end colour for use in paintComponent
            setOpaque(false);        // Don't let Swing paint a solid rectangular background — our gradient handles it
        }

        /** Default constructor uses the UITheme's default gradient colours */
        public GradientPanel() {
            this(GRADIENT_START, GRADIENT_END); // Delegate to the 2-arg constructor with theme defaults
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); // Scratch Graphics2D context
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Smooth rendering
            GradientPaint gp = new GradientPaint(0, 0, startColor, getWidth(), 0, endColor); // Horizontal gradient from (x=0) startColor to (x=width) endColor
            g2.setPaint(gp);                                // Set the gradient as the current paint source
            g2.fillRect(0, 0, getWidth(), getHeight());     // Fill the entire panel bounds with the gradient (a rectangle, not rounded — the header spans the full width)
            g2.dispose();                                   // Release the scratch context
            super.paintComponent(g);                        // Call super AFTER our fill so child components are painted on top of the gradient
        }
    }

    // ── Inner Class: RoundedBorder ────────────────────────────────────────────

    /**
     * RoundedBorder — Custom Swing border with rounded corners.
     *
     * Extends AbstractBorder (not LineBorder which is always rectangular) to draw
     * a rounded rectangle outline in any given colour and radius.
     * Used by input fields (styleInputField) to achieve the rounded look.
     */
    public static class RoundedBorder extends AbstractBorder {
        private final int radius; // Corner arc radius in pixels — matches UITheme.CORNER_RADIUS (12) for consistency
        private final Color color; // The border stroke colour — set to BORDER_DEFAULT normally, ACCENT_BLUE when focused

        /** @param radius Corner arc radius in pixels  @param color Stroke colour */
        public RoundedBorder(int radius, Color color) {
            this.radius = radius; // Store radius
            this.color = color;   // Store colour
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); // Scratch context overlaid on the component's graphics
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Anti-aliased smooth corners
            g2.setColor(color);                          // Use the configured border colour (default or focus glow)
            g2.setStroke(new BasicStroke(1.5f));         // 1.5px stroke width — thin enough to be subtle, thick enough to be visible
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius); // Draw rounded rectangle border; w-1/h-1 keeps stroke inside component bounds
            g2.dispose(); // Release the scratch context
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 4, 4, 4); // 4px padding on all sides between the border line and the component content
        }

        @Override
        public boolean isBorderOpaque() {
            return false; // false = the border does NOT fill its background — transparent to the panel's background shows through
        }
    }

    // ── Toast Notification ─────────────────────────────────────────────────────

    /**
     * showToast — Displays a brief animated message (toast notification) at the bottom of the window.
     *
     * The toast appears as a coloured pill that:
     *   1. Fades in over ~300ms (10 frames × 30ms, +0.1 opacity per frame)
     *   2. Holds fully visible for 2 seconds
     *   3. Fades out over ~300ms
     *   4. Disposes itself — no memory leak
     *
     * Uses javax.swing.Timer (the Swing-safe timer) so UI updates happen on the Event Dispatch Thread (EDT).
     *
     * @param parent  The component whose parent window is used to position the toast
     * @param message The text to display in the toast
     * @param bgColor The background colour of the toast (e.g. ACCENT_GREEN for success, ACCENT_RED for error)
     */
    public static void showToast(JComponent parent, String message, Color bgColor) {
        JWindow toast = new JWindow(SwingUtilities.getWindowAncestor(parent)); // JWindow is a borderless window; it floats above the parent window
        JLabel label = new JLabel(message, SwingConstants.CENTER); // Centred label using the message text
        label.setFont(FONT_BODY_BOLD);      // Bold font so the toast message is immediately readable at a glance
        label.setForeground(Color.WHITE);   // White text for high contrast on coloured backgrounds
        label.setOpaque(true);              // Opaque so the background colour is painted (label is not transparent)
        label.setBackground(bgColor);       // Set the toast pill background to the caller-supplied colour
        label.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24)); // Generous padding gives the toast a "pill" proportioned look
        toast.add(label);                   // Add the label as the only content of the borderless JWindow
        toast.pack();                       // Size the window exactly to the label's preferred size

        // Position at bottom-center of parent window
        Window ancestor = SwingUtilities.getWindowAncestor(parent); // Get the top-level JFrame or JDialog that contains this component
        if (ancestor != null) {
            int x = ancestor.getX() + (ancestor.getWidth() - toast.getWidth()) / 2; // Horizontally centre the toast relative to the parent window
            int y = ancestor.getY() + ancestor.getHeight() - toast.getHeight() - 60; // Position 60px above the bottom edge of the parent window
            toast.setLocation(x, y); // Move the toast JWindow to the calculated position on screen
        }

        toast.setOpacity(0f);   // Start fully transparent (opacity 0 = invisible)
        toast.setVisible(true); // Make it visible but transparent — it will fade in via the Timer below

        // ── Fade In Timer ──
        Timer fadeIn = new Timer(30, null); // Swing Timer fires every 30ms on the EDT — safe for GUI updates
        fadeIn.addActionListener(e -> {
            float opacity = toast.getOpacity() + 0.1f; // Increase opacity by 10% on each 30ms tick → full opacity in ~300ms
            if (opacity >= 1f) {           // When fully opaque, stop fading in
                toast.setOpacity(1f);      // Clamp to exactly 1.0 to avoid floating point overshoot
                fadeIn.stop();             // Stop the fade-in timer

                // ── Hold Timer — wait 2 seconds at full opacity before fading out ──
                Timer hold = new Timer(2000, ev -> {
                    // ── Fade Out Timer ──
                    Timer fadeOut = new Timer(30, null); // Another 30ms timer for the fade-out phase
                    fadeOut.addActionListener(e2 -> {
                        float op = toast.getOpacity() - 0.1f; // Decrease opacity by 10% per tick
                        if (op <= 0f) {         // When fully transparent
                            toast.setOpacity(0f);
                            toast.dispose();    // Destroy the JWindow and free its resources — prevents memory leaks
                            fadeOut.stop();     // Stop the fade-out timer
                        } else {
                            toast.setOpacity(op); // Update opacity for this frame
                        }
                    });
                    fadeOut.start(); // Begin fading out after the 2-second hold
                });
                hold.setRepeats(false); // The hold timer fires only once — it's a one-shot delay, not a repeating interval
                hold.start();           // Start the 2-second hold countdown
            } else {
                toast.setOpacity(opacity); // Still fading in — update to the new opacity value
            }
        });
        fadeIn.start(); // Begin the fade-in animation immediately
    }

    // ── Password Strength Utilities ────────────────────────────────────────────

    /**
     * getPasswordStrength — Calculates a 0–3 integer score for a password's strength.
     *
     * Scoring criteria (each rule adds 1 point):
     *   +1 if length ≥ 8 characters
     *   +1 if contains both uppercase AND lowercase letters
     *   +1 if contains at least one digit
     *   +1 if contains at least one special character (non-alphanumeric)
     * Maximum score is capped at 3 (used by the 3-segment JProgressBar in LoginUI).
     *
     * @param password The plain-text password to evaluate
     * @return         Integer score from 0 (empty/very weak) to 3 (strong)
     */
    public static int getPasswordStrength(String password) {
        if (password.isEmpty()) return 0; // Empty password gets a score of 0 immediately
        int score = 0;                    // Accumulator — starts at 0, incremented for each satisfied criterion

        if (password.length() >= 8)                                  score++; // +1 for minimum length (8 chars is the most common policy minimum)
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) score++; // +1 for mixed case — regex .*[A-Z].* means "contains at least one uppercase letter"
        if (password.matches(".*\\d.*"))                             score++; // +1 for containing at least one digit (\\d matches any digit character 0-9)
        if (password.matches(".*[^a-zA-Z0-9].*"))                   score++; // +1 for at least one special character ([^a-zA-Z0-9] = NOT a letter or digit)

        return Math.min(score, 3); // Cap at 3 — the JProgressBar has a max of 3 so scores of 4 must not exceed the bar's range
    }

    /**
     * getStrengthLabel — Maps a numeric strength score to a human-readable string.
     *
     * @param strength Score from getPasswordStrength (0–3)
     * @return         "Weak", "Medium", "Strong", or "" for score 0
     */
    public static String getStrengthLabel(int strength) {
        return switch (strength) { // Java 14+ switch expression — returns a value directly
            case 1 -> "Weak";   // Score 1: only one criterion met — password is too simple
            case 2 -> "Medium"; // Score 2: two criteria met — adequate but could be stronger
            case 3 -> "Strong"; // Score 3: three or more criteria met — good password
            default -> "";      // Score 0: empty password — no label shown
        };
    }

    /**
     * getStrengthColor — Maps a numeric strength score to a display colour.
     *
     * @param strength Score from getPasswordStrength (0–3)
     * @return         Red for Weak, Orange/Amber for Medium, Green for Strong, or the border default for 0
     */
    public static Color getStrengthColor(int strength) {
        return switch (strength) { // Switch expression maps each score to the corresponding accent colour
            case 1 -> ACCENT_RED;    // Weak  → red    (danger colour — prompts improvement)
            case 2 -> ACCENT_ORANGE; // Medium → amber  (warning colour)
            case 3 -> ACCENT_GREEN;  // Strong → green  (success colour)
            default -> BORDER_DEFAULT; // Score 0 → subtle grey (progress bar invisible when empty)
        };
    }
}
