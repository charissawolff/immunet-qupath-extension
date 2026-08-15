package org.computational_immunology.ext.ImmuNet.ui.tabBoxes;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.LoadSlideDataCommand;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class DataListViewerBox extends VBox {

    private final ObservableList<String> datasetItems = FXCollections.observableArrayList();
    private final ListViewerBox datasetBox;
    private final ListViewerBox slideBox;

    public DataListViewerBox(ServerGateway serverGateway, int height, int width) {
        this.datasetBox = new ListViewerBox(height, width);
        this.slideBox = new ListViewerBox(height, width);

        TextField searchField = new TextField();
        searchField.setPromptText("Search for a dataset...");
        FilteredList<String> filteredItems = new FilteredList<>(datasetItems, s -> true);
        datasetBox.getListView().setItems(filteredItems);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredItems.setPredicate(item -> query.isEmpty() || item.toLowerCase().contains(query));
        });

        VBox.setMargin(datasetBox.getBox(), new Insets(5, 2, 2, 2));
        VBox.setMargin(slideBox.getBox(), new Insets(15, 2, 2, 2));
        VBox.setMargin(searchField, new Insets(5, 2, 3, 2));

        datasetBox.getListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            LoadSlideDataCommand loadSlideDataCommand = new LoadSlideDataCommand(serverGateway, newValue);
            loadSlideDataCommand.build();
            loadSlideDataCommand.setOnDone(() -> {
                List<String> slides = loadSlideDataCommand.getTask().getValue();
                slideBox.setItems(slides);
            });
            loadSlideDataCommand.setOnFailed(() ->
                    ImmuNetLog.error("Failed to load slide data for dataset: " + newValue, loadSlideDataCommand.getTask().getException()));
            loadSlideDataCommand.start();
            ImmuNetLog.log("Selected: " + newValue);
        });

        getChildren().addAll(searchField, datasetBox.getBox(), slideBox.getBox());
    }

    /** Replaces the dataset list; the search filter re-applies automatically. */
    public void setDatasets(List<String> datasets) {
        datasetItems.setAll(datasets);
    }

    public String getSelectedDataset() {
        return datasetBox.getListView().getSelectionModel().getSelectedItem();
    }

    public String getSelectedSlide() {
        return slideBox.getListView().getSelectionModel().getSelectedItem();
    }

    public boolean hasCompleteSelection() {
        return getSelectedDataset() != null && getSelectedSlide() != null;
    }
}