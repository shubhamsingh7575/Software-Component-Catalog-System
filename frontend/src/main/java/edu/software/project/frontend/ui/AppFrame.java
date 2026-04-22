package edu.software.project.frontend.ui;

import edu.software.project.frontend.api.ApiClient;
import edu.software.project.frontend.api.ApiException;
import edu.software.project.frontend.model.Catalogue;
import edu.software.project.frontend.model.CatalogueRequest;
import edu.software.project.frontend.model.Component;
import edu.software.project.frontend.model.ComponentRequest;
import edu.software.project.frontend.model.ComponentType;
import edu.software.project.frontend.model.LoginRequest;
import edu.software.project.frontend.model.RegisterRequest;
import edu.software.project.frontend.model.Session;
import edu.software.project.frontend.model.UserProfile;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

public class AppFrame extends JFrame {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String PREF_BASE_URL = "baseUrl";
    private static final String PREF_TOKEN = "token";
    private static final Color PAGE = new Color(0xF6F7F9);
    private static final Color PANEL = Color.WHITE;
    private static final Color PANEL_STRONG = new Color(0xF0F4F8);
    private static final Color INK = new Color(0x1F2937);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color ACCENT = new Color(0x2563EB);
    private static final Color ACCENT_DARK = new Color(0x1D4ED8);
    private static final Color TEAL = new Color(0x0F172A);
    private static final Color LINE = new Color(0xD7DEE7);
    private static final Color INPUT_BG = Color.WHITE;
    private static final Color INPUT_BG_FOCUSED = new Color(0xF8FBFF);
    private static final Color INPUT_BG_DISABLED = new Color(0xEEF2F7);
    private static final Color INPUT_BORDER = new Color(0xCBD5E1);
    private static final Color INPUT_BORDER_FOCUSED = ACCENT;
    private static final Color INPUT_TEXT = INK;
    private static final Color INPUT_PLACEHOLDER = new Color(0x9CA3AF);
    private static final Color INPUT_SELECTION = new Color(0xBFDBFE);
    private static final String VIEW_AUTH = "auth";
    private static final String VIEW_CATALOGUES = "catalogues";

    private final ApiClient apiClient = new ApiClient();
    private final Preferences preferences = Preferences.userNodeForPackage(AppFrame.class);

    private final PlaceholderTextField baseUrlField = new PlaceholderTextField("http://localhost:8080", 24);
    private final JLabel sessionLabel = new JLabel("Not signed in");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton logoutButton = new JButton("Logout");

    private final AuthPanel authPanel = new AuthPanel();
    private final CatalogueWorkspacePanel catalogueWorkspacePanel = new CatalogueWorkspacePanel();
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    private Session session;

    public AppFrame() {
        super("Software Component Catalogue");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1320, 820);
        getContentPane().setBackground(PAGE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(buildTopBar(), BorderLayout.NORTH);

        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        contentPanel.add(authPanel, VIEW_AUTH);
        contentPanel.add(catalogueWorkspacePanel, VIEW_CATALOGUES);
        add(contentPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        updateSession(null);
        restorePersistedSession();
    }

    private JPanel buildTopBar() {
        JPanel panel = new CardPanel();
        panel.setLayout(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel left = transparent(new BorderLayout(0, 10));
        JLabel title = new JLabel("Component Catalog Studio");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Design catalogues, curate reusable components, and keep the workspace in sync.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        left.add(title, BorderLayout.NORTH);
        left.add(subtitle, BorderLayout.SOUTH);
        panel.add(left, BorderLayout.CENTER);

        JPanel right = transparent(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        sessionLabel.setForeground(INK);
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(Font.BOLD, 13f));
        styleActionButton(logoutButton, false);
        logoutButton.addActionListener(event -> {
            if (session == null) {
                updateSession(null);
                return;
            }
            runAction("Logout", () -> {
                Session current = session;
                apiClient.logout(current.baseUrl(), current.token());
                return null;
            }, ignored -> {
                clearPersistedSession();
                updateSession(null);
            });
        });
        right.add(sessionLabel);
        right.add(logoutButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void updateSession(Session newSession) {
        session = newSession;
        authPanel.refreshState();
        catalogueWorkspacePanel.refreshState();

        if (newSession == null) {
            contentLayout.show(contentPanel, VIEW_AUTH);
            sessionLabel.setText("Not signed in");
            statusLabel.setText("Sign in to manage catalogues.");
            logoutButton.setEnabled(false);
            return;
        }

        persistSession(newSession);
        contentLayout.show(contentPanel, VIEW_CATALOGUES);
        sessionLabel.setText(newSession.user().username() + " (" + newSession.user().role() + ")");
        statusLabel.setText("Authenticated against " + newSession.baseUrl());
        logoutButton.setEnabled(true);
        catalogueWorkspacePanel.loadInitialCatalogues();
    }

    private String currentBaseUrl() {
        return baseUrlField.getText().trim().isEmpty() ? DEFAULT_BASE_URL : baseUrlField.getText().trim();
    }

    private void styleActionButton(JButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(button.isEnabled() ? LINE : PANEL_STRONG, 1, true),
                new EmptyBorder(10, 18, 10, 18)
        ));
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setForeground(button.isEnabled() ? INK : MUTED);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setMargin(new Insets(0, 0, 0, 0));
    }

    private void styleTextInput(JComponent component, int width) {
        component.setPreferredSize(new Dimension(width, 40));
        if (component instanceof JTextField textField) {
            textField.setForeground(INPUT_TEXT);
            textField.setCaretColor(INPUT_TEXT);
            textField.setSelectionColor(INPUT_SELECTION);
            textField.setSelectedTextColor(INK);
            textField.setDisabledTextColor(MUTED);
            textField.setOpaque(false);
            textField.setBorder(new EmptyBorder(10, 12, 10, 12));
            textField.setBackground(INPUT_BG);
        } else {
            component.setForeground(INK);
            component.setBackground(Color.WHITE);
            component.setOpaque(true);
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));
        }
    }

    private void styleTextArea(JTextArea textArea, boolean codeLike) {
        textArea.setBorder(new EmptyBorder(14, 14, 14, 14));
        textArea.setBackground(codeLike ? new Color(0xFBFDFF) : Color.WHITE);
        textArea.setForeground(INPUT_TEXT);
        textArea.setCaretColor(INPUT_TEXT);
        textArea.setSelectionColor(INPUT_SELECTION);
        textArea.setSelectedTextColor(INK);
        textArea.setDisabledTextColor(MUTED);
        if (codeLike) {
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        }
    }

    private void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xE7F1F0));
        table.setSelectionForeground(INK);
        table.setBackground(Color.WHITE);
        table.setForeground(INK);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setOpaque(true);
        header.setBackground(PANEL_STRONG);
        header.setForeground(INK);
        header.setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private void restorePersistedSession() {
        String persistedToken = preferences.get(PREF_TOKEN, "").trim();
        String persistedBaseUrl = preferences.get(PREF_BASE_URL, "").trim();
        if (!persistedBaseUrl.isEmpty()) {
            baseUrlField.setText(persistedBaseUrl);
        }
        if (persistedToken.isEmpty()) {
            return;
        }

        statusLabel.setText("Restoring saved session...");
        new SwingWorker<Session, Void>() {
            @Override
            protected Session doInBackground() {
                String baseUrl = persistedBaseUrl.isEmpty() ? DEFAULT_BASE_URL : persistedBaseUrl;
                UserProfile profile = apiClient.getCurrentUser(baseUrl, persistedToken);
                return new Session(baseUrl, persistedToken, profile);
            }

            @Override
            protected void done() {
                try {
                    updateSession(get());
                    statusLabel.setText("Restored saved session");
                } catch (Exception exception) {
                    clearPersistedSession();
                    statusLabel.setText("Saved session expired. Sign in again.");
                }
            }
        }.execute();
    }

    private void persistSession(Session session) {
        preferences.put(PREF_BASE_URL, session.baseUrl());
        preferences.put(PREF_TOKEN, session.token());
    }

    private void clearPersistedSession() {
        preferences.remove(PREF_TOKEN);
        String currentBaseUrl = baseUrlField.getText().trim();
        if (!currentBaseUrl.isEmpty()) {
            preferences.put(PREF_BASE_URL, currentBaseUrl);
        }
    }

    private <T> void runAction(String actionName, Callable<T> task, ResultConsumer<T> consumer) {
        statusLabel.setText(actionName + "...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    consumer.accept(result);
                    if (statusLabel.getText().startsWith(actionName)) {
                        statusLabel.setText(actionName + " complete");
                    }
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    String message = cause instanceof ApiException apiException
                            ? apiException.getMessage()
                            : cause.getMessage();
                    statusLabel.setText(actionName + " failed");
                    JOptionPane.showMessageDialog(AppFrame.this, message, "Request Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private Session requireSession() {
        if (session == null) {
            throw new ApiException("Sign in first.");
        }
        return session;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private CatalogueRequest promptCatalogue(Catalogue existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.name(), 24);
        JTextField descriptionField = new JTextField(existing == null ? "" : existing.description(), 24);
        JTextField keywordsField = new JTextField(existing == null ? "" : existing.keywords(), 24);
        JPanel panel = formPanel(
                fieldRow("Name", nameField),
                fieldRow("Description", descriptionField),
                fieldRow("Keywords", keywordsField)
        );
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                existing == null ? "Create Catalogue" : "Update Catalogue",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new CatalogueRequest(nameField.getText().trim(), descriptionField.getText().trim(), keywordsField.getText().trim());
    }

    private ComponentRequest promptComponent(Component existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.name(), 24);
        JTextField descriptionField = new JTextField(existing == null ? "" : existing.description(), 24);
        JTextField keywordsField = new JTextField(existing == null ? "" : existing.keywords(), 24);
        JTextArea bodyArea = new JTextArea(existing == null ? "" : existing.body(), 10, 24);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JComboBox<ComponentType> typeBox = new JComboBox<>(new DefaultComboBoxModel<>(ComponentType.values()));
        typeBox.setSelectedItem(existing == null ? ComponentType.CODE : existing.type());

        JPanel panel = formPanel(
                fieldRow("Name", nameField),
                fieldRow("Description", descriptionField),
                fieldRow("Keywords", keywordsField),
                fieldRow("Body", new JScrollPane(bodyArea)),
                fieldRow("Type", typeBox)
        );
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                existing == null ? "Add Component" : "Update Component",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new ComponentRequest(
                nameField.getText().trim(),
                descriptionField.getText().trim(),
                keywordsField.getText().trim(),
                bodyArea.getText(),
                (ComponentType) typeBox.getSelectedItem()
        );
    }

    private JPanel formPanel(JComponent... rows) {
        JPanel panel = transparent(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JComponent row : rows) {
            row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            panel.add(row);
            panel.add(Box.createVerticalStrut(8));
        }
        return panel;
    }

    private JPanel fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 8));
        row.setOpaque(false);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(MUTED);
        labelComponent.setPreferredSize(new Dimension(100, 24));
        row.add(labelComponent, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        int rowHeight = Math.max(field.getPreferredSize().height, 32) + 8;
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
        return row;
    }

    private JPanel wrapInCard(String title, String subtitle, JComponent body) {
        return wrapInCard(title, subtitle, null, body);
    }

    private JPanel wrapInCard(String title, String subtitle, JComponent headerActions, JComponent body) {
        JPanel panel = new CardPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.add(sectionHeader(title, subtitle, headerActions), BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JComponent sectionHeader(String title, String subtitle) {
        return sectionHeader(title, subtitle, null);
    }

    private JComponent sectionHeader(String title, String subtitle, JComponent trailing) {
        JPanel panel = transparent(new BorderLayout(12, 4));
        JPanel textBlock = transparent(new BorderLayout(0, 4));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 13f));
        textBlock.add(titleLabel, BorderLayout.NORTH);
        textBlock.add(subtitleLabel, BorderLayout.CENTER);
        panel.add(textBlock, BorderLayout.CENTER);
        if (trailing != null) {
            panel.add(trailing, BorderLayout.EAST);
        }
        return panel;
    }

    private final class AuthPanel extends JPanel {
        private final PlaceholderTextField loginEmailField = new PlaceholderTextField("Email", 24);
        private final PlaceholderPasswordField loginPasswordField = new PlaceholderPasswordField("Password", 24);

        private final PlaceholderTextField registerNameField = new PlaceholderTextField("Username", 24);
        private final PlaceholderTextField registerEmailField = new PlaceholderTextField("Email", 24);
        private final PlaceholderPasswordField registerPasswordField = new PlaceholderPasswordField("Password", 24);
        private final PlaceholderPasswordField registerConfirmField = new PlaceholderPasswordField("Confirm password", 24);

        private AuthPanel() {
            setOpaque(false);
            setLayout(new GridLayout(1, 2, 12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            add(buildLoginPanel());
            add(buildRegisterPanel());
        }

        private JPanel buildLoginPanel() {
            JButton loginButton = new JButton("Log In");
            styleActionButton(loginButton, true);
            styleTextInput(loginEmailField, 280);
            styleTextInput(loginPasswordField, 280);
            styleTextInput(baseUrlField, 280);
            loginButton.addActionListener(event -> runAction("Login", () -> {
                String baseUrl = currentBaseUrl();
                var auth = apiClient.login(baseUrl, new LoginRequest(
                        loginEmailField.getText().trim(),
                        new String(loginPasswordField.getPassword())
                ));
                UserProfile profile = apiClient.getCurrentUser(baseUrl, auth.token());
                return new Session(baseUrl, auth.token(), profile);
            }, AppFrame.this::updateSession));

            JPanel panel = new CardPanel();
            panel.setLayout(new BorderLayout(0, 16));
            panel.add(sectionHeader("Welcome Back", "Resume work with your existing catalogue account."), BorderLayout.NORTH);
            panel.add(formPanel(
                    fieldRow("Server", baseUrlField),
                    fieldRow("Email", loginEmailField),
                    fieldRow("Password", loginPasswordField),
                    fieldRow("", loginButton)
            ), BorderLayout.CENTER);
            return panel;
        }

        private JPanel buildRegisterPanel() {
            JButton registerButton = new JButton("Register");
            styleActionButton(registerButton, false);
            styleTextInput(registerNameField, 280);
            styleTextInput(registerEmailField, 280);
            styleTextInput(registerPasswordField, 280);
            styleTextInput(registerConfirmField, 280);
            registerButton.addActionListener(event -> runAction("Register", () -> {
                String baseUrl = currentBaseUrl();
                var auth = apiClient.register(baseUrl, new RegisterRequest(
                        registerNameField.getText().trim(),
                        registerEmailField.getText().trim(),
                        new String(registerPasswordField.getPassword()),
                        new String(registerConfirmField.getPassword())
                ));
                UserProfile profile = apiClient.getCurrentUser(baseUrl, auth.token());
                return new Session(baseUrl, auth.token(), profile);
            }, AppFrame.this::updateSession));

            JPanel panel = new CardPanel();
            panel.setLayout(new BorderLayout(0, 16));
            panel.add(sectionHeader("Create Account", "Start a fresh workspace and keep your catalogues synced."), BorderLayout.NORTH);
            panel.add(formPanel(
                    fieldRow("Username", registerNameField),
                    fieldRow("Email", registerEmailField),
                    fieldRow("Password", registerPasswordField),
                    fieldRow("Confirm", registerConfirmField),
                    fieldRow("", registerButton)
            ), BorderLayout.CENTER);
            return panel;
        }

        private void refreshState() {
        }
    }

    private final class CatalogueWorkspacePanel extends JPanel {
        private final CatalogueTableModel catalogueTableModel = new CatalogueTableModel();
        private final JTable catalogueTable = new JTable(catalogueTableModel);
        private final ComponentTableModel componentTableModel = new ComponentTableModel();
        private final JTable componentTable = new JTable(componentTableModel);
        private final JTextArea catalogueDetailsArea = new JTextArea();
        private final JLabel componentNameValue = detailValueLabel();
        private final JLabel componentDescriptionValue = detailValueLabel();
        private final JTextArea componentBodyArea = new JTextArea();
        private final PlaceholderTextField catalogueSearchField = new PlaceholderTextField("Search catalogues", 20);
        private final PlaceholderTextField componentSearchField = new PlaceholderTextField("Search components", 20);

        private CatalogueWorkspacePanel() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

            JPanel actions = new CardPanel();
            actions.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
            JButton refreshAllButton = new JButton("Refresh All");
            JButton refreshMineButton = new JButton("Refresh Mine");
            JButton createCatalogueButton = new JButton("Create Catalogue");
            JButton updateCatalogueButton = new JButton("Update Catalogue");
            JButton deleteCatalogueButton = new JButton("Delete Catalogue");
            JButton addComponentButton = new JButton("Add Component");
            JButton updateComponentButton = new JButton("Update Component");
            JButton deleteComponentButton = new JButton("Delete Component");
            JButton useComponentButton = new JButton("Record Use");

            styleActionButton(refreshAllButton, false);
            styleActionButton(refreshMineButton, false);
            styleActionButton(createCatalogueButton, true);
            styleActionButton(updateCatalogueButton, false);
            styleActionButton(deleteCatalogueButton, false);
            styleActionButton(addComponentButton, true);
            styleActionButton(updateComponentButton, false);
            styleActionButton(deleteComponentButton, false);
            styleActionButton(useComponentButton, false);

            refreshAllButton.addActionListener(event -> loadCatalogues(false));
            refreshMineButton.addActionListener(event -> loadCatalogues(true));
            createCatalogueButton.addActionListener(event -> createCatalogue());
            updateCatalogueButton.addActionListener(event -> updateCatalogue());
            deleteCatalogueButton.addActionListener(event -> deleteCatalogue());
            addComponentButton.addActionListener(event -> createComponent());
            updateComponentButton.addActionListener(event -> updateComponent());
            deleteComponentButton.addActionListener(event -> deleteComponent());
            useComponentButton.addActionListener(event -> recordUsage());

            actions.add(refreshAllButton);
            actions.add(refreshMineButton);
            actions.add(createCatalogueButton);
            actions.add(updateCatalogueButton);
            actions.add(deleteCatalogueButton);
            actions.add(addComponentButton);
            actions.add(updateComponentButton);
            actions.add(deleteComponentButton);
            actions.add(useComponentButton);

            styleTable(catalogueTable);
            styleTable(componentTable);
            catalogueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            catalogueTable.getSelectionModel().addListSelectionListener(event -> onCatalogueSelected());
            componentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            componentTable.getSelectionModel().addListSelectionListener(event -> showComponentDetails(selectedComponent()));

            JSplitPane horizontalSplit = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    buildCatalogueCard(),
                    buildDetailsPane()
            );
            horizontalSplit.setOpaque(false);
            horizontalSplit.setBorder(BorderFactory.createEmptyBorder());
            horizontalSplit.setResizeWeight(0.42);

            add(actions, BorderLayout.NORTH);
            add(horizontalSplit, BorderLayout.CENTER);
        }

        private void loadInitialCatalogues() {
            runAction("Load catalogues", () -> {
                Session current = requireSession();
                return apiClient.getCatalogues(current.baseUrl(), current.token());
            }, catalogues -> {
                catalogueSearchField.setText("");
                componentSearchField.setText("");
                catalogueTableModel.setItems(catalogues);
                componentTableModel.setItems(List.of());
                showCatalogueDetails(null);
                showComponentDetails(null);
                if (!catalogues.isEmpty()) {
                    catalogueTable.setRowSelectionInterval(0, 0);
                    onCatalogueSelected();
                }
            });
        }

        private JPanel buildDetailsPane() {
            JPanel panel = transparent(new BorderLayout(10, 10));

            catalogueDetailsArea.setEditable(false);
            catalogueDetailsArea.setLineWrap(true);
            catalogueDetailsArea.setWrapStyleWord(true);
            styleTextArea(catalogueDetailsArea, false);

            componentBodyArea.setEditable(false);
            componentBodyArea.setLineWrap(true);
            componentBodyArea.setWrapStyleWord(true);
            styleTextArea(componentBodyArea, true);

            JScrollPane catalogueDetailsScroll = new JScrollPane(catalogueDetailsArea);
            catalogueDetailsScroll.setPreferredSize(new Dimension(420, 170));
            catalogueDetailsScroll.setBorder(BorderFactory.createEmptyBorder());

            JScrollPane componentTableScroll = new JScrollPane(componentTable);
            componentTableScroll.setBorder(BorderFactory.createEmptyBorder());

            JSplitPane componentSplit = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    buildComponentsCard(componentTableScroll),
                    wrapInCard("Component Detail", "Content, metadata, and usage signals.", buildComponentDetailPanel())
            );
            componentSplit.setOpaque(false);
            componentSplit.setBorder(BorderFactory.createEmptyBorder());
            componentSplit.setResizeWeight(0.55);

            JSplitPane verticalSplit = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    wrapInCard("Catalogue Detail", "Context, ownership, and the current inventory.", catalogueDetailsScroll),
                    componentSplit
            );
            verticalSplit.setOpaque(false);
            verticalSplit.setBorder(BorderFactory.createEmptyBorder());
            verticalSplit.setResizeWeight(0.34);

            panel.add(verticalSplit, BorderLayout.CENTER);
            return panel;
        }

        private void loadCatalogues(boolean mineOnly) {
            runAction(mineOnly ? "Load my catalogues" : "Load catalogues", () -> {
                Session current = requireSession();
                return mineOnly
                        ? apiClient.getMyCatalogues(current.baseUrl(), current.token())
                        : apiClient.getCatalogues(current.baseUrl(), current.token());
            }, catalogues -> {
                catalogueSearchField.setText("");
                componentSearchField.setText("");
                catalogueTableModel.setItems(catalogues);
                componentTableModel.setItems(List.of());
                showCatalogueDetails(null);
                showComponentDetails(null);
            });
        }

        private void searchCatalogues() {
            String keywords = catalogueSearchField.getText().trim();
            if (keywords.isEmpty()) {
                loadInitialCatalogues();
                return;
            }
            runAction("Search catalogues", () -> {
                Session current = requireSession();
                return apiClient.searchCatalogues(current.baseUrl(), current.token(), keywords);
            }, catalogues -> {
                catalogueTableModel.setItems(catalogues);
                componentTableModel.setItems(List.of());
                showCatalogueDetails(null);
                showComponentDetails(null);
                if (!catalogues.isEmpty()) {
                    catalogueTable.setRowSelectionInterval(0, 0);
                    onCatalogueSelected();
                }
            });
        }

        private void searchComponents() {
            Catalogue selectedCatalogue = selectedCatalogue();
            if (selectedCatalogue == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue first.");
                return;
            }
            String keywords = componentSearchField.getText().trim();
            if (keywords.isEmpty()) {
                componentTableModel.setItems(selectedCatalogue.components());
                showComponentDetails(null);
                return;
            }
            runAction("Search components", () -> {
                Session current = requireSession();
                return apiClient.searchComponents(current.baseUrl(), current.token(), selectedCatalogue.id(), keywords);
            }, components -> {
                componentTableModel.setItems(components);
                showComponentDetails(null);
                if (!components.isEmpty()) {
                    componentTable.setRowSelectionInterval(0, 0);
                    showComponentDetails(componentTableModel.getSelected(0));
                }
            });
        }

        private void createCatalogue() {
            CatalogueRequest request = promptCatalogue(null);
            if (request == null) {
                return;
            }
            runAction("Create catalogue", () -> {
                Session current = requireSession();
                apiClient.createCatalogue(current.baseUrl(), current.token(), request);
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, catalogues -> {
                catalogueTableModel.setItems(catalogues);
                statusLabel.setText("Catalogue created");
            });
        }

        private void updateCatalogue() {
            Catalogue selected = selectedCatalogue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue first.");
                return;
            }
            CatalogueRequest request = promptCatalogue(selected);
            if (request == null) {
                return;
            }
            runAction("Update catalogue", () -> {
                Session current = requireSession();
                apiClient.updateCatalogue(current.baseUrl(), current.token(), selected.id(), request);
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, this::replaceCataloguesAndKeepSelection);
        }

        private void deleteCatalogue() {
            Catalogue selected = selectedCatalogue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue first.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete catalogue " + selected.name() + " and all of its components?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            runAction("Delete catalogue", () -> {
                Session current = requireSession();
                apiClient.deleteCatalogue(current.baseUrl(), current.token(), selected.id());
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, catalogues -> {
                catalogueTableModel.setItems(catalogues);
                componentTableModel.setItems(List.of());
                showCatalogueDetails(null);
                showComponentDetails(null);
            });
        }

        private void createComponent() {
            Catalogue selectedCatalogue = selectedCatalogue();
            if (selectedCatalogue == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue first.");
                return;
            }
            ComponentRequest request = promptComponent(null);
            if (request == null) {
                return;
            }
            runAction("Add component", () -> {
                Session current = requireSession();
                apiClient.createComponent(current.baseUrl(), current.token(), selectedCatalogue.id(), request);
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, this::replaceCataloguesAndKeepSelection);
        }

        private void updateComponent() {
            Catalogue selectedCatalogue = selectedCatalogue();
            Component selectedComponent = selectedComponent();
            if (selectedCatalogue == null || selectedComponent == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue and component first.");
                return;
            }
            ComponentRequest request = promptComponent(selectedComponent);
            if (request == null) {
                return;
            }
            runAction("Update component", () -> {
                Session current = requireSession();
                apiClient.updateComponent(
                        current.baseUrl(),
                        current.token(),
                        selectedCatalogue.id(),
                        selectedComponent.id(),
                        request
                );
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, catalogues -> replaceCataloguesAndKeepSelection(catalogues, selectedCatalogue.id(), selectedComponent.id()));
        }

        private void deleteComponent() {
            Catalogue selectedCatalogue = selectedCatalogue();
            Component selectedComponent = selectedComponent();
            if (selectedCatalogue == null || selectedComponent == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue and component first.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete component " + selectedComponent.name() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            runAction("Delete component", () -> {
                Session current = requireSession();
                apiClient.deleteComponent(current.baseUrl(), current.token(), selectedCatalogue.id(), selectedComponent.id());
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, catalogues -> replaceCataloguesAndKeepSelection(catalogues, selectedCatalogue.id(), -1));
        }

        private void recordUsage() {
            Catalogue selectedCatalogue = selectedCatalogue();
            Component selectedComponent = selectedComponent();
            if (selectedCatalogue == null || selectedComponent == null) {
                JOptionPane.showMessageDialog(this, "Select a catalogue and component first.");
                return;
            }
            runAction("Record usage", () -> {
                Session current = requireSession();
                apiClient.useComponent(current.baseUrl(), current.token(), selectedCatalogue.id(), selectedComponent.id());
                return apiClient.getCatalogues(current.baseUrl(), current.token());
            }, catalogues -> replaceCataloguesAndKeepSelection(catalogues, selectedCatalogue.id(), selectedComponent.id()));
        }

        private Catalogue selectedCatalogue() {
            return catalogueTableModel.getSelected(catalogueTable.getSelectedRow());
        }

        private Component selectedComponent() {
            return componentTableModel.getSelected(componentTable.getSelectedRow());
        }

        private void onCatalogueSelected() {
            Catalogue selected = selectedCatalogue();
            componentSearchField.setText("");
            componentTableModel.setItems(selected == null ? List.of() : selected.components());
            showCatalogueDetails(selected);
            showComponentDetails(null);
        }

        private JPanel buildCatalogueCard() {
            styleTextInput(catalogueSearchField, 180);
            JButton searchButton = new JButton("Search");
            styleActionButton(searchButton, false);
            searchButton.addActionListener(event -> searchCatalogues());

            JPanel tools = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            tools.add(catalogueSearchField);
            tools.add(searchButton);
            return wrapInCard("Catalogues", "Choose a catalogue to manage its components.", tools, new JScrollPane(catalogueTable));
        }

        private JPanel buildComponentsCard(JComponent body) {
            styleTextInput(componentSearchField, 180);
            JButton searchButton = new JButton("Search");
            styleActionButton(searchButton, false);
            searchButton.addActionListener(event -> searchComponents());

            JPanel tools = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            tools.add(componentSearchField);
            tools.add(searchButton);
            return wrapInCard("Components", "Everything inside the selected catalogue lives here.", tools, body);
        }

        private void showCatalogueDetails(Catalogue catalogue) {
            if (catalogue == null) {
                catalogueDetailsArea.setText(session == null
                        ? "Sign in to load catalogues."
                        : "Select a catalogue to view and manage its components.");
                return;
            }
            catalogueDetailsArea.setText("""
                    Catalogue ID: %d
                    Name: %s
                    Owner: %s (#%d)

                    Description:
                    %s

                    Keywords:
                    %s

                    Components: %d
                    """.formatted(
                    catalogue.id(),
                    valueOrEmpty(catalogue.name()),
                    valueOrEmpty(catalogue.ownerUsername()),
                    catalogue.ownerId(),
                    valueOrEmpty(catalogue.description()),
                    valueOrEmpty(catalogue.keywords()),
                    catalogue.components().size()
            ));
        }

        private void showComponentDetails(Component component) {
            if (component == null) {
                componentNameValue.setText("No component selected");
                componentDescriptionValue.setText("-");
                componentBodyArea.setText("Select a component inside the current catalogue.");
                return;
            }
            componentNameValue.setText(valueOrEmpty(component.name()));
            componentDescriptionValue.setText(valueOrEmpty(component.description()));
            componentBodyArea.setText(valueOrEmpty(component.body()));
            componentBodyArea.setCaretPosition(0);
        }

        private JPanel buildComponentDetailPanel() {
            JPanel panel = transparent(new GridLayout(1, 2, 14, 0));

            JPanel left = transparent(new BorderLayout(0, 12));
            left.add(detailItem("Title", componentNameValue), BorderLayout.NORTH);
            left.add(detailItem("Description", componentDescriptionValue), BorderLayout.CENTER);

            JScrollPane bodyScroll = new JScrollPane(componentBodyArea);
            bodyScroll.setBorder(BorderFactory.createLineBorder(LINE, 1, true));
            bodyScroll.setPreferredSize(new Dimension(420, 180));

            JPanel bodyPanel = transparent(new BorderLayout(0, 8));
            JLabel bodyTitle = new JLabel("Body");
            bodyTitle.setForeground(MUTED);
            bodyTitle.setFont(bodyTitle.getFont().deriveFont(Font.BOLD, 12f));
            bodyPanel.add(bodyTitle, BorderLayout.NORTH);
            bodyPanel.add(bodyScroll, BorderLayout.CENTER);

            panel.add(left);
            panel.add(bodyPanel);
            return panel;
        }

        private JPanel detailItem(String label, JLabel valueLabel) {
            JPanel item = transparent(new BorderLayout(0, 4));
            JLabel title = new JLabel(label);
            title.setForeground(MUTED);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
            item.add(title, BorderLayout.NORTH);
            item.add(valueLabel, BorderLayout.CENTER);
            return item;
        }

        private void replaceCataloguesAndKeepSelection(List<Catalogue> catalogues) {
            Catalogue selectedCatalogue = selectedCatalogue();
            long catalogueId = selectedCatalogue == null ? -1 : selectedCatalogue.id();
            replaceCataloguesAndKeepSelection(catalogues, catalogueId, -1);
        }

        private void replaceCataloguesAndKeepSelection(List<Catalogue> catalogues, long catalogueId, long componentId) {
            catalogueTableModel.setItems(catalogues);

            int catalogueRow = catalogueTableModel.indexOf(catalogueId);
            if (catalogueRow < 0) {
                componentTableModel.setItems(List.of());
                showCatalogueDetails(null);
                showComponentDetails(null);
                return;
            }

            catalogueTable.setRowSelectionInterval(catalogueRow, catalogueRow);
            Catalogue selected = catalogueTableModel.getSelected(catalogueRow);
            componentTableModel.setItems(selected.components());
            showCatalogueDetails(selected);

            int componentRow = componentTableModel.indexOf(componentId);
            if (componentRow >= 0) {
                componentTable.setRowSelectionInterval(componentRow, componentRow);
                showComponentDetails(componentTableModel.getSelected(componentRow));
            } else {
                componentTable.clearSelection();
                showComponentDetails(null);
            }
        }

        private void refreshState() {
            catalogueTableModel.setItems(List.of());
            componentTableModel.setItems(List.of());
            showCatalogueDetails(null);
            showComponentDetails(null);
        }
    }

    private static final class CatalogueTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Name", "Owner", "Keywords", "Components"};
        private List<Catalogue> items = new ArrayList<>();

        public void setItems(List<Catalogue> items) {
            this.items = new ArrayList<>(items);
            fireTableDataChanged();
        }

        public Catalogue getSelected(int row) {
            return row >= 0 && row < items.size() ? items.get(row) : null;
        }

        public int indexOf(long catalogueId) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id() == catalogueId) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Catalogue item = items.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> item.id();
                case 1 -> item.name();
                case 2 -> item.ownerUsername();
                case 3 -> item.keywords();
                case 4 -> item.components().size();
                default -> "";
            };
        }
    }

    private static final class ComponentTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Name", "Type", "Usage", "Search Hits", "Misses"};
        private List<Component> items = new ArrayList<>();

        public void setItems(List<Component> items) {
            this.items = new ArrayList<>(items);
            fireTableDataChanged();
        }

        public Component getSelected(int row) {
            return row >= 0 && row < items.size() ? items.get(row) : null;
        }

        public int indexOf(long componentId) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id() == componentId) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Component item = items.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> item.id();
                case 1 -> item.name();
                case 2 -> item.type();
                case 3 -> item.usageCount();
                case 4 -> item.searchHitCount();
                case 5 -> item.searchedButNotUsedCount();
                default -> "";
            };
        }
    }

    private static final class CardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(PANEL);
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            graphics2D.setColor(LINE);
            graphics2D.setStroke(new BasicStroke(1f));
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            graphics2D.dispose();
        }

        private CardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(12, 12, 12, 12));
        }
    }

    @FunctionalInterface
    private interface ResultConsumer<T> {
        void accept(T value);
    }

    private static JLabel detailValueLabel() {
        JLabel label = new JLabel("-");
        label.setForeground(INK);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        return label;
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 12, 10, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            paintFieldSurface(this, graphics);
            super.paintComponent(graphics);
            if (!getText().isEmpty() || isFocusOwner()) {
                return;
            }
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.setColor(INPUT_PLACEHOLDER);
            graphics2D.drawString(placeholder, getInsets().left + 2, graphics.getFontMetrics().getMaxAscent() + getInsets().top + 2);
            graphics2D.dispose();
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            paintFieldOutline(this, graphics);
        }
    }

    private static final class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;

        private PlaceholderPasswordField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 12, 10, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            paintFieldSurface(this, graphics);
            super.paintComponent(graphics);
            if (getPassword().length > 0 || isFocusOwner()) {
                return;
            }
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.setColor(INPUT_PLACEHOLDER);
            graphics2D.drawString(placeholder, getInsets().left + 2, graphics.getFontMetrics().getMaxAscent() + getInsets().top + 2);
            graphics2D.dispose();
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            paintFieldOutline(this, graphics);
        }
    }

    private static void paintFieldSurface(JTextField field, Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(resolveFieldBackground(field));
        graphics2D.fillRoundRect(0, 0, field.getWidth(), field.getHeight(), 12, 12);
        graphics2D.dispose();
    }

    private static void paintFieldOutline(JTextField field, Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(field.isFocusOwner() && field.isEnabled() ? INPUT_BORDER_FOCUSED : INPUT_BORDER);
        graphics2D.drawRoundRect(0, 0, field.getWidth() - 1, field.getHeight() - 1, 12, 12);
        graphics2D.dispose();
    }

    private static Color resolveFieldBackground(JTextField field) {
        if (!field.isEnabled() || !field.isEditable()) {
            return INPUT_BG_DISABLED;
        }
        return field.isFocusOwner() ? INPUT_BG_FOCUSED : INPUT_BG;
    }
}
