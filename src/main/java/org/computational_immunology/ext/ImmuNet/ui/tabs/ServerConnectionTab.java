package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.Dimensions;
import org.computational_immunology.ext.ImmuNet.ui.commands.connectServer.ConnectToServerCommand;
import org.computational_immunology.ext.ImmuNet.ui.tabBoxes.CachedLoginBox;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import qupath.lib.gui.prefs.PathPrefs;

public class ServerConnectionTab extends CustomSidePanelTab{

    Path credentialsConfigPath;

    CachedLoginBox hostnameField;
    CachedLoginBox usernameField;
    CachedLoginBox passwordField;

    CachedLoginBox dbUserField;
    CachedLoginBox dbPassField;

    CachedLoginBox dbLocalPortField;
    CachedLoginBox dbRemotePortField;

    CachedLoginBox[] allLoginBoxes;

    Label statusLabel = new Label();

    public ServerConnectionTab() {
        super("Server Connection");
        String userPath = PathPrefs.userPathProperty().get();
        if (userPath == null) {
            userPath = System.getProperty("user.home");
        }
        credentialsConfigPath = Path.of(userPath, "login.json");
    }

    /**
     * Creates a panel with three input text fields and a connect 
     * button underneath eachother.
     * @return The full panel with input fields and button components.
     */
    @Override
    public VBox getContent(){
        VBox sidePanelTab = new VBox();

        
        sidePanelTab.setPadding(new Insets(10, 10, 10, 10)); // Box margins
        sidePanelTab.setSpacing(5); // Space between buttons and boxes
        
        // Text fields
        hostnameField = new CachedLoginBox("hostname", "Hostname", CachedLoginBox.BoxType.NORMAL);
        usernameField = new CachedLoginBox("username", "User", CachedLoginBox.BoxType.NORMAL);
        passwordField = new CachedLoginBox("password", "Password", CachedLoginBox.BoxType.PASS);
        
        dbUserField  = new CachedLoginBox("dbuser", "Database User", CachedLoginBox.BoxType.NORMAL);
        dbPassField  = new CachedLoginBox("dbpass", "Database Pass", CachedLoginBox.BoxType.PASS);
        
        dbLocalPortField  = new CachedLoginBox("dblocalport", "Local Port", CachedLoginBox.BoxType.PORT);
        dbRemotePortField  = new CachedLoginBox("dbremoteport", "Remote Port", CachedLoginBox.BoxType.PORT);
        
        // Set defaults before loadConfig() so saved values (if any) still override these
        dbLocalPortField.field.setText("8082");
        dbRemotePortField.field.setText("80");

        allLoginBoxes = new CachedLoginBox[]{hostnameField, usernameField, passwordField, dbUserField, dbPassField, dbLocalPortField, dbRemotePortField};
        
        loadConfig();
        
        Button connectBtn = makeButton("Connect", new Dimensions(40, 100));
        EventHandler<ActionEvent> connectEvent = sendCredentials();

        //add listener to make sure all fields are filled before enabling the connect button
        connectBtn.disableProperty().bind(Bindings.createBooleanBinding(() ->
                Arrays.stream(allLoginBoxes).anyMatch(s-> s.field.getText().isEmpty()),
                hostnameField.field.textProperty(),
                usernameField.field.textProperty(),
                passwordField.field.textProperty(),
                dbUserField.field.textProperty(),
                dbPassField.field.textProperty(),
                dbLocalPortField.field.textProperty(),
                dbRemotePortField.field.textProperty()
        ));
        
        connectBtn.setOnAction(connectEvent);

        
        sidePanelTab.getChildren().addAll(hostnameField, usernameField, passwordField, dbUserField, dbPassField, dbLocalPortField, dbRemotePortField, connectBtn, statusLabel);
        return sidePanelTab;
    }

    /**
     * Tries populating the host, username and password boxes via the config file "config/login.json"
     */
    private void loadConfig() {

        String content = null;
        try {
            content = Files.readString(credentialsConfigPath);
            ImmuNetLog.log("ImmuNet login config file found.");
            JSONObject config = new JSONObject(content);

            for (var field : allLoginBoxes)
                field.load(config);

        } catch (IOException | JSONException e) {
            regenerateCredentialsFile(e);
        }
    }

    /**
     * Called whenever the original file read for the config file failed.
     * @param readFailException The exception that we got when trying to read the file.
     *                          If IOException, the file is not there. if JSONException, the file is there
     *                          but the syntax is wrong.
     */
    private void regenerateCredentialsFile(Exception readFailException) {
        ImmuNetLog.log("ImmuNet login config file not present or invalid. Creating");
        try {
            if (readFailException instanceof JSONException) //When the syntax is wrong, if the user tinkered with the file.
                Files.deleteIfExists(credentialsConfigPath);

            Path newFile = Files.createFile(credentialsConfigPath);
            JSONObject json = new JSONObject(newFile);

            for (var field : allLoginBoxes)
                field.save(json); //field defaults empty so this populates empty fields

            Files.writeString(newFile, json.toString(2));
            ImmuNetLog.log("Login config file recreated successfully.");
        } catch (IOException ex) {
            ImmuNetLog.error("Could not recreate the login configuration file.", ex);
        }
    }

    public void saveConfig() throws IOException {

        JSONObject json = new JSONObject();
        for (var field : allLoginBoxes)
            field.save(json);

        Files.writeString(credentialsConfigPath, json.toString());
    }

    /**
     * Connect to server using filled in credentials in input fields
     * @return Event Handler upon an action happening (i.e. button press)
     */
    private EventHandler<ActionEvent> sendCredentials(){
        return (ActionEvent e) -> {
            String hostname     = hostnameField.field.getText();
            String username     = usernameField.field.getText();
            String password     = passwordField.field.getText();
            String dbUsername   = dbUserField.field.getText();
            String dbPass       = dbPassField.field.getText();
            int dbLocalPort = Integer.parseInt(dbLocalPortField.field.getText());
            int dbRemotePort = Integer.parseInt(dbRemotePortField.field.getText());

            // Credentials entered
            if (Arrays.stream(allLoginBoxes).noneMatch(s-> s.field.getText().isEmpty()) ){
                ConnectToServerCommand connectCommand = new ConnectToServerCommand(username, hostname, password, dbUsername, dbPass, dbLocalPort, dbRemotePort);
                statusLabel.setStyle("-fx-text-fill: black;");
                connectCommand.build();
                connectCommand.getTask().messageProperty().addListener((obs, oldMsg, newMsg) -> statusLabel.setText(newMsg));
                connectCommand.start();
                connectCommand.setOnDone(() -> {
                    try {
                        //get result of the command to check if the connection was successful
                        Boolean result = connectCommand.getTask().getValue();
                        if (result) {
                            saveConfig(); //if we didn't catch an exception the credentials were right
                            ImmuNetLog.log("Valid SSH connection. Credentials saved.");
                            statusLabel.setText("● Connected");
                            statusLabel.setStyle("-fx-text-fill: green;");
                        } else {
                            ImmuNetLog.log("Invalid SSH connection. Credentials not saved.");
                            String errorText = statusLabel.getText();
                            statusLabel.setText("● " + (errorText.isEmpty() ? "" : ": " + errorText));
                            statusLabel.setStyle("-fx-text-fill: red;");
                        }
                    } catch (IOException ex) {
                        ImmuNetLog.error("Failed to save credentials after successful connection.", ex);
                    }
                });

                connectCommand.setOnFailed(() -> {
                    ImmuNetLog.error("Connection failed:");
                    statusLabel.setText("Error while connecting to server. Check logs for details.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        };
    }
}
