package org.computational_immunology.ext.ImmuNet.ui.tabBoxes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPolygon;
import org.computational_immunology.ext.ImmuNet.ui.PathObjectFinder;
import org.computational_immunology.ext.ImmuNet.ui.commands.SelectPathObjectCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.SetPolygonVisibilityCommand;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import qupath.lib.objects.PathObject;

public class PolygonListBox extends VBox {

    private final ObservableList<AnnotationPolygon> polygons = FXCollections.observableArrayList();
    private final Map<String, BooleanProperty> checkedMap = new HashMap<>();
    private final ListView<AnnotationPolygon> listView;
    private final CheckBox showAllCheckBox;

    public PolygonListBox() {
        Label title = new Label("Polygon list");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        VBox.setMargin(title, new javafx.geometry.Insets(0, 2, 5, 2));
        listView = new ListView<>(polygons);
        listView.setPrefHeight(200);
        VBox.setMargin(listView, new Insets(1, 2, 5, 2));
        listView.setCellFactory(CheckBoxListCell.forListView(
                p -> checkedMap.computeIfAbsent(p.getId(), id -> {
                    BooleanProperty visible = new SimpleBooleanProperty(true);
                    visible.addListener((obs, was, isVisible) -> new SetPolygonVisibilityCommand(id, isVisible).execute());
                    return visible;
                }),
                new StringConverter<AnnotationPolygon>() {
                    @Override
                    public String toString(AnnotationPolygon p) {
                        return p.getName();
                    }

                    @Override
                    public AnnotationPolygon fromString(String s) {
                        return null;
                    }
                }
        ));

        showAllCheckBox = new CheckBox("Show polygons");
        VBox.setMargin(showAllCheckBox, new javafx.geometry.Insets(0, 2, 5, 2));
        showAllCheckBox.setSelected(true);
        showAllCheckBox.setOnAction(e -> {
            ImmuNetLog.log("Show polygons checkbox clicked");
            checkedMap.forEach((id, visible) -> {
                visible.set(showAllCheckBox.isSelected());
                new SetPolygonVisibilityCommand(id, showAllCheckBox.isSelected()).execute();
            });
        });

        //when the user selects a polygon in the list, select it in the viewer
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null || newSel.getId() == null) {
                return;
            }
            BooleanProperty visibility = checkedMap.get(newSel.getId());
            boolean isVisible = visibility.get();
            if (!isVisible) {
                return;
            }
            ImmuNetLog.log("Polygon selected in list: " + newSel.getName());
            PathObject selectedPathObject = PathObjectFinder.execute(newSel.getId());
            new SelectPathObjectCommand(selectedPathObject).execute();
        });

        getChildren().addAll(title, showAllCheckBox, listView);
    }


    /** Replaces the displayed polygons with the provided list */
    public void setPolygons(List<AnnotationPolygon> newPolygons) {
        checkedMap.clear();
        polygons.setAll(newPolygons);
    }

    /** Empties the list */
    public void clear() {
        polygons.clear();
        checkedMap.clear();
    }
}