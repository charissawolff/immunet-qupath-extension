package org.computational_immunology.ext.ImmuNet.ui.tabBoxes;

import org.computational_immunology.ext.ImmuNet.ImmuNetExtension;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

public class EnableExtensionCheckbox extends VBox {
    private static final BooleanProperty enableExtensionProperty = ImmuNetExtension.enableExtensionProperty();

    private EnableExtensionCheckbox() {
        setPadding(new Insets(15));
        setSpacing(10);

        CheckBox checkBox = new CheckBox("Enable ImmuNet Extension");
        checkBox.selectedProperty().bindBidirectional(enableExtensionProperty);
        getChildren().add(checkBox);
    }

    public static Parent getInstance() {
        return new EnableExtensionCheckbox();
    }
}
