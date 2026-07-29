package org.computational_immunology.ext.ImmuNet.ui;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONObject;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class CachedLoginBox extends HBox {
    String json_key;
    TextField field;
    Label label;

    public enum BoxType {
        NORMAL,
        PASS
    }

    public CachedLoginBox(@NonNull String json_key, String name, BoxType type) {
        label = new Label(name + ":"); // Text field name in front of field
        this.json_key = json_key;
        if (type == BoxType.NORMAL)
            field = new TextField();
        else
            field = new PasswordField();
        setSpacing(10); // Space between label and text field
        getChildren().addAll(label, field);
    }

    public void load(JSONObject json){
        field.setText(json.getString(json_key));
    }

    public void save(JSONObject json){
        json.put(json_key, field.getText());
    }

    public void clear(){field.clear();}
}
