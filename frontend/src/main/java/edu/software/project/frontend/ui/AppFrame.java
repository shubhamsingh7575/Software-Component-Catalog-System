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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AppFrame extends JFrame {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final ApiClient apiClient = new ApiClient();

    private final JTextField baseUrlField = new JTextField(DEFAULT_BASE_URL, 24);
    private final JLabel sessionLabel = new JLabel("Not signed in");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton logoutButton = new JButton("Logout");

    private final AuthPanel authPanel = new AuthPanel();
    private final CataloguePanel cataloguePanel = new CataloguePanel();
    private final ComponentPanel componentPanel = new ComponentPanel();

    private Session session;

    public AppFrame() {
        super("Software Component Catalogue");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1260, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        add(buildTopBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Authentication", authPanel);
        tabs.addTab("Catalogues", cataloguePanel);
        tabs.addTab("Components", componentPanel);
        add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        updateSession(null);
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(new JLabel("Base URL"));
        left.add(baseUrlField);
        panel.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutButton.addActionListener(event -> {
            if (session == null) {
                updateSession(null);
                return;
            }
            runAction("Logout", () -> {
                Session current = session;
                apiClient.logout(current.baseUrl(), current.token());
                return null;
            }, ignored -> updateSession(null));
        });
        right.add(sessionLabel);
        right.add(logoutButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void updateSession(Session newSession) {
        this.session = newSession;
        authPanel.refreshState();
        cataloguePanel.refreshState();
        componentPanel.refreshState();

        if (newSession == null) {
            sessionLabel.setText("Not signed in");
            statusLabel.setText("Sign in to browse catalogues and components.");
            logoutButton.setEnabled(false);
            return;
        }

        sessionLabel.setText(newSession.user().username() + " (" + newSession.user().role() + ")");
        logoutButton.setEnabled(true);
        statusLabel.setText("Authenticated against " + newSession.baseUrl());
    }

    private String currentBaseUrl() {
        return baseUrlField.getText().trim().isEmpty() ? DEFAULT_BASE_URL : baseUrlField.getText().trim();
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
        int result = JOptionPane.showConfirmDialog(this, panel,
                existing == null ? "Create Catalogue" : "Update Catalogue",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new CatalogueRequest(nameField.getText().trim(), descriptionField.getText().trim(), keywordsField.getText().trim());
    }

    private ComponentRequest promptComponent(Component existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.name(), 24);
        JTextField descriptionField = new JTextField(existing == null ? "" : existing.description(), 24);
        JTextField keywordsField = new JTextField(existing == null ? "" : existing.keywords(), 24);
        JTextField catalogueIdsField = new JTextField(existing == null
                ? ""
                : existing.catalogueIds().stream().map(String::valueOf).collect(Collectors.joining(",")), 24);
        JComboBox<ComponentType> typeBox = new JComboBox<>(new DefaultComboBoxModel<>(ComponentType.values()));
        typeBox.setSelectedItem(existing == null ? ComponentType.CODE : existing.type());

        JPanel panel = formPanel(
                fieldRow("Name", nameField),
                fieldRow("Description", descriptionField),
                fieldRow("Keywords", keywordsField),
                fieldRow("Type", typeBox),
                fieldRow("Catalogue IDs", catalogueIdsField)
        );
        int result = JOptionPane.showConfirmDialog(this, panel,
                existing == null ? "Create Component" : "Update Component",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new ComponentRequest(
                nameField.getText().trim(),
                descriptionField.getText().trim(),
                keywordsField.getText().trim(),
                (ComponentType) typeBox.getSelectedItem(),
                parseIds(catalogueIdsField.getText())
        );
    }

    private List<Long> parseIds(String rawIds) {
        List<Long> ids = new ArrayList<>();
        if (rawIds == null || rawIds.isBlank()) {
            return ids;
        }
        for (String token : rawIds.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                try {
                    ids.add(Long.parseLong(trimmed));
                } catch (NumberFormatException exception) {
                    throw new ApiException("Catalogue IDs must be comma-separated numbers.");
                }
            }
        }
        return ids;
    }

    private static JPanel formPanel(JComponent... rows) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JComponent row : rows) {
            row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            panel.add(row);
            panel.add(Box.createVerticalStrut(8));
        }
        return panel;
    }

    private static JPanel fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 8));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private final class AuthPanel extends JPanel {
        private final JTextField loginEmailField = new JTextField("admin@example.com", 24);
        private final JPasswordField loginPasswordField = new JPasswordField("StrongPass1!", 24);

        private final JTextField registerNameField = new JTextField("Admin User", 24);
        private final JTextField registerEmailField = new JTextField("admin@example.com", 24);
        private final JPasswordField registerPasswordField = new JPasswordField("StrongPass1!", 24);
        private final JPasswordField registerConfirmField = new JPasswordField("StrongPass1!", 24);

        private AuthPanel() {
            setLayout(new GridLayout(1, 2, 16, 16));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            add(buildLoginPanel());
            add(buildRegisterPanel());
        }

        private JPanel buildLoginPanel() {
            JButton loginButton = new JButton("Login");
            loginButton.addActionListener(event -> runAction("Login", () -> {
                String baseUrl = currentBaseUrl();
                var auth = apiClient.login(baseUrl, new LoginRequest(
                        loginEmailField.getText().trim(),
                        new String(loginPasswordField.getPassword())
                ));
                UserProfile profile = apiClient.getCurrentUser(baseUrl, auth.token());
                return new Session(baseUrl, auth.token(), profile);
            }, AppFrame.this::updateSession));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Existing User"));
            panel.add(formPanel(
                    fieldRow("Email", loginEmailField),
                    fieldRow("Password", loginPasswordField),
                    fieldRow("", loginButton)
            ), BorderLayout.NORTH);
            return panel;
        }

        private JPanel buildRegisterPanel() {
            JButton registerButton = new JButton("Register");
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

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder("New User"));
            panel.add(formPanel(
                    fieldRow("Username", registerNameField),
                    fieldRow("Email", registerEmailField),
                    fieldRow("Password", registerPasswordField),
                    fieldRow("Confirm", registerConfirmField),
                    fieldRow("", registerButton)
            ), BorderLayout.NORTH);
            return panel;
        }

        private void refreshState() {
        }
    }

    private final class CataloguePanel extends JPanel {
        private final CatalogueTableModel tableModel = new CatalogueTableModel();
        private final JTable table = new JTable(tableModel);
        private final JTextArea detailsArea = new JTextArea();

        private CataloguePanel() {
            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton refreshAllButton = new JButton("Refresh All");
            JButton refreshMineButton = new JButton("Refresh Mine");
            JButton createButton = new JButton("Create");
            JButton updateButton = new JButton("Update");
            JButton deleteButton = new JButton("Delete");

            refreshAllButton.addActionListener(event -> runAction("Load catalogues", () -> {
                Session current = requireSession();
                return apiClient.getCatalogues(current.baseUrl(), current.token());
            }, result -> tableModel.setItems(result)));
            refreshMineButton.addActionListener(event -> runAction("Load my catalogues", () -> {
                Session current = requireSession();
                return apiClient.getMyCatalogues(current.baseUrl(), current.token());
            }, result -> tableModel.setItems(result)));
            createButton.addActionListener(event -> {
                CatalogueRequest request = promptCatalogue(null);
                if (request == null) {
                    return;
                }
                runAction("Create catalogue", () -> {
                    Session current = requireSession();
                    apiClient.createCatalogue(current.baseUrl(), current.token(), request);
                    return apiClient.getMyCatalogues(current.baseUrl(), current.token());
                }, result -> {
                    tableModel.setItems(result);
                    statusLabel.setText("Catalogue created");
                });
            });
            updateButton.addActionListener(event -> {
                Catalogue selected = tableModel.getSelected(table.getSelectedRow());
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
                }, tableModel::setItems);
            });
            deleteButton.addActionListener(event -> {
                Catalogue selected = tableModel.getSelected(table.getSelectedRow());
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Select a catalogue first.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete catalogue " + selected.name() + "?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                runAction("Delete catalogue", () -> {
                    Session current = requireSession();
                    apiClient.deleteCatalogue(current.baseUrl(), current.token(), selected.id());
                    return apiClient.getMyCatalogues(current.baseUrl(), current.token());
                }, tableModel::setItems);
            });

            actions.add(refreshAllButton);
            actions.add(refreshMineButton);
            actions.add(createButton);
            actions.add(updateButton);
            actions.add(deleteButton);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(event -> showCatalogueDetails());
            JScrollPane tableScroll = new JScrollPane(table);

            detailsArea.setEditable(false);
            detailsArea.setLineWrap(true);
            detailsArea.setWrapStyleWord(true);
            JScrollPane detailsScroll = new JScrollPane(detailsArea);
            detailsScroll.setPreferredSize(new Dimension(320, 400));

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailsScroll);
            splitPane.setResizeWeight(0.72);

            add(actions, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
        }

        private void showCatalogueDetails() {
            Catalogue selected = tableModel.getSelected(table.getSelectedRow());
            if (selected == null) {
                detailsArea.setText("No catalogue selected.");
                return;
            }
            String componentNames = selected.components().isEmpty()
                    ? "None"
                    : selected.components().stream()
                    .map(component -> component.id() + " - " + component.name() + " (" + component.type() + ")")
                    .collect(Collectors.joining("\n"));
            detailsArea.setText("""
                    Catalogue ID: %d
                    Name: %s
                    Owner: %s (#%d)

                    Description:
                    %s

                    Keywords:
                    %s

                    Components:
                    %s
                    """.formatted(
                    selected.id(),
                    valueOrEmpty(selected.name()),
                    valueOrEmpty(selected.ownerUsername()),
                    selected.ownerId(),
                    valueOrEmpty(selected.description()),
                    valueOrEmpty(selected.keywords()),
                    componentNames
            ));
        }

        private void refreshState() {
            tableModel.setItems(List.of());
            detailsArea.setText(session == null ? "Sign in to load catalogues." : "Use Refresh All or Refresh Mine.");
        }
    }

    private final class ComponentPanel extends JPanel {
        private final ComponentTableModel tableModel = new ComponentTableModel();
        private final JTable table = new JTable(tableModel);
        private final JTextArea detailsArea = new JTextArea();
        private final JTextField searchField = new JTextField(20);
        private final JTextField idLookupField = new JTextField(8);

        private ComponentPanel() {
            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton refreshButton = new JButton("Refresh All");
            JButton searchButton = new JButton("Search");
            JButton fetchByIdButton = new JButton("Fetch By ID");
            JButton useButton = new JButton("Record Use");
            JButton createButton = new JButton("Create");
            JButton updateButton = new JButton("Update");
            JButton deleteButton = new JButton("Delete");

            refreshButton.addActionListener(event -> runAction("Load components", () -> {
                Session current = requireSession();
                return apiClient.getComponents(current.baseUrl(), current.token());
            }, tableModel::setItems));
            searchButton.addActionListener(event -> runAction("Search components", () -> {
                Session current = requireSession();
                return apiClient.searchComponents(current.baseUrl(), current.token(), searchField.getText().trim());
            }, tableModel::setItems));
            fetchByIdButton.addActionListener(event -> runAction("Fetch component", () -> {
                Session current = requireSession();
                Component component = apiClient.getComponent(current.baseUrl(), current.token(), Long.parseLong(idLookupField.getText().trim()));
                return List.of(component);
            }, tableModel::setItems));
            useButton.addActionListener(event -> {
                Component selected = tableModel.getSelected(table.getSelectedRow());
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Select a component first.");
                    return;
                }
                runAction("Record component usage", () -> {
                    Session current = requireSession();
                    Component updated = apiClient.useComponent(current.baseUrl(), current.token(), selected.id());
                    return List.of(updated);
                }, tableModel::setItems);
            });
            createButton.addActionListener(event -> {
                if (!ensureAdmin()) {
                    return;
                }
                ComponentRequest request = promptComponent(null);
                if (request == null) {
                    return;
                }
                runAction("Create component", () -> {
                    Session current = requireSession();
                    apiClient.createComponent(current.baseUrl(), current.token(), request);
                    return apiClient.getComponents(current.baseUrl(), current.token());
                }, tableModel::setItems);
            });
            updateButton.addActionListener(event -> {
                if (!ensureAdmin()) {
                    return;
                }
                Component selected = tableModel.getSelected(table.getSelectedRow());
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Select a component first.");
                    return;
                }
                ComponentRequest request = promptComponent(selected);
                if (request == null) {
                    return;
                }
                runAction("Update component", () -> {
                    Session current = requireSession();
                    apiClient.updateComponent(current.baseUrl(), current.token(), selected.id(), request);
                    return apiClient.getComponents(current.baseUrl(), current.token());
                }, tableModel::setItems);
            });
            deleteButton.addActionListener(event -> {
                if (!ensureAdmin()) {
                    return;
                }
                Component selected = tableModel.getSelected(table.getSelectedRow());
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Select a component first.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete component " + selected.name() + "?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                runAction("Delete component", () -> {
                    Session current = requireSession();
                    apiClient.deleteComponent(current.baseUrl(), current.token(), selected.id());
                    return apiClient.getComponents(current.baseUrl(), current.token());
                }, tableModel::setItems);
            });

            actions.add(refreshButton);
            actions.add(new JLabel("Keywords"));
            actions.add(searchField);
            actions.add(searchButton);
            actions.add(new JLabel("Component ID"));
            actions.add(idLookupField);
            actions.add(fetchByIdButton);
            actions.add(useButton);
            actions.add(createButton);
            actions.add(updateButton);
            actions.add(deleteButton);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(event -> showComponentDetails());
            JScrollPane tableScroll = new JScrollPane(table);

            detailsArea.setEditable(false);
            detailsArea.setLineWrap(true);
            detailsArea.setWrapStyleWord(true);
            JScrollPane detailsScroll = new JScrollPane(detailsArea);
            detailsScroll.setPreferredSize(new Dimension(360, 400));

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailsScroll);
            splitPane.setResizeWeight(0.72);

            add(actions, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
        }

        private boolean ensureAdmin() {
            if (session == null) {
                JOptionPane.showMessageDialog(this, "Sign in first.");
                return false;
            }
            if (!session.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Only ADMIN users can manage components.");
                return false;
            }
            return true;
        }

        private void showComponentDetails() {
            Component selected = tableModel.getSelected(table.getSelectedRow());
            if (selected == null) {
                detailsArea.setText("No component selected.");
                return;
            }
            String catalogues = selected.catalogueIds().isEmpty()
                    ? "None"
                    : selected.catalogueIds().stream().map(String::valueOf).collect(Collectors.joining(", "));
            detailsArea.setText("""
                    Component ID: %d
                    Name: %s
                    Type: %s

                    Description:
                    %s

                    Keywords:
                    %s

                    Usage Count: %d
                    Search Hit Count: %d
                    Searched But Not Used: %d
                    Catalogue IDs: %s
                    """.formatted(
                    selected.id(),
                    valueOrEmpty(selected.name()),
                    selected.type(),
                    valueOrEmpty(selected.description()),
                    valueOrEmpty(selected.keywords()),
                    selected.usageCount(),
                    selected.searchHitCount(),
                    selected.searchedButNotUsedCount(),
                    catalogues
            ));
        }

        private void refreshState() {
            tableModel.setItems(List.of());
            detailsArea.setText(session == null ? "Sign in to load components." : "Use Refresh All, Search, or Fetch By ID.");
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

    @FunctionalInterface
    private interface ResultConsumer<T> {
        void accept(T value);
    }
}
