package org.computational_immunology.ext.ImmuNet.ui.tabs;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerUploadGateway;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPolygon;
import org.computational_immunology.ext.ImmuNet.core.models.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.AddPolygonCommand;
import org.json.JSONObject;

import java.awt.image.BufferedImage;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import qupath.lib.images.ImageData;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.tools.PathObjectImageViewers;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;

/*
Viewer box for viewing USER added new polygons, not the ones that are fetched from the server
Here user can see the polygons they added, and can choose to upload them to the server or remove them.
*/
public class NewPolygonViewerBox extends TableView<PathObject> {
    private final ServerUploadGateway dataUploadHandler;
    private final SelectedDataStore selectedDataStore;

    public NewPolygonViewerBox(ObservableList<PathObject> items, ServerUploadGateway dataUploadHandler, SelectedDataStore selectedDataStore) {
        super(items);
        this.dataUploadHandler = dataUploadHandler;
        this.selectedDataStore = selectedDataStore;
        setEditable(true);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().add(buildThumbnailColumn());
        getColumns().add(buildNameColumn());
        getColumns().add(buildDatasetColumn());
        getColumns().add(buildSlideColumn());
        getColumns().add(buildAddColumn());
    }

    private TableColumn<PathObject, String> buildDatasetColumn() {
        TableColumn<PathObject, String> col = new TableColumn<>("Dataset");
        col.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getMetadata().get("dataset")));
        return col;
    }
    private TableColumn<PathObject, String> buildSlideColumn() {
        TableColumn<PathObject, String> col = new TableColumn<>("Slide");
        col.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getMetadata().get("slide")));
        return col;
    }

    private TableColumn<PathObject, String> buildNameColumn() {
        TableColumn<PathObject, String> col = new TableColumn<>("Name");
        col.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event -> event.getRowValue().setName(event.getNewValue()));
        return col;
    }

    private TableColumn<PathObject, Void> buildAddColumn() {
        TableColumn<PathObject, Void> col = new TableColumn<>("Add");
        col.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Add");
            {
                button.setOnAction(e -> handleAddClicked(getTableView().getItems().get(getIndex()), button));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                PathObject row = getTableView().getItems().get(getIndex());
                button.setText(row.isLocked() ? "Added" : "Add");
                button.setDisable(row.isLocked());
                setGraphic(button);
            }
        });
        return col;
    }

    private TableColumn<PathObject, PathObject> buildThumbnailColumn() {
        TableColumn<PathObject, PathObject> col = new TableColumn<>("Thumbnail");
        col.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        col.setCellFactory(c -> {
            ImageData<BufferedImage> imageData = viewer.getImageData();
            if (imageData == null) {
                return new TableCell<>();
            }
            return PathObjectImageViewers.createTableCell(viewer, imageData.getServer(), true, 5);
        });
        return col;
    }



    private void handleAddClicked(PathObject polygon, Button button) {
        button.setDisable(true);
        ImmuNetLog.log("Adding polygon: " + polygon.getName() + " which is:" + polygon);
        AnnotationPolygon polygonData = PolygonConverter.fromPathObject(polygon, selectedDataStore.getDx(), selectedDataStore.getDy());
        ImmuNetLog.log("Polygon from PathObject is " + polygonData);
        JSONObject polygonJson = PolygonConverter.toJSONObject(polygonData);
        ImmuNetLog.log("Polygon JSON is " + polygonJson);
        AddPolygonCommand command = new AddPolygonCommand(polygonJson, dataUploadHandler);
        command.build();
        //visible on screen and not editable anymore
        command.setOnDone(() -> { polygon.setLocked(true); refresh(); });
        command.setOnFailed(this::refresh);
        command.start();
    }
}
