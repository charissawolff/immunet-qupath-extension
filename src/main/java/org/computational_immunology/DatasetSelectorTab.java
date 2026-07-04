package org.computational_immunology;

import java.util.List;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DatasetSelectorTab extends CustomSidePanelTab {

    public DatasetSelectorTab() {
        super("Image selector");
    }

    /**
     * Creates a panel with a button and two boxes underneath eachother.
     * @return The full panel with button and box components.
     */
    @Override
    public VBox getContent(){
        VBox sidePanelTab = new VBox();
        sidePanelTab.setPadding(new Insets(10, 10, 10, 10)); // Box margins
        sidePanelTab.setSpacing(5); // Space between buttons and boxes

        // Interactive selection boxes
        ListViewerBox dsBox = new ListViewerBox(300, sidePanelTab.getMaxWidth()); // Dataset
        ListViewerBox tsBox = new ListViewerBox(300, sidePanelTab.getMaxWidth()); // Tissue slide

        Button loadDataBtn = makeButton("Load Datasets", new Dimensions(40, 100)); 
        loadDataBtn.setOnAction(e -> MenuActions.updateListViewerBox(dsBox, getDatasets()));

        Button openImgBtn = makeButton("Open Image", new Dimensions(40, 100));
        Label openedImageLabel = new Label("No image opened");
        openImgBtn.setOnAction(e -> {
            String dsName = dsBox.getListView().getSelectionModel().getSelectedItem();
            String tsName = tsBox.getListView().getSelectionModel().getSelectedItem();

            if (dsName == null || tsName == null) {
                ImmuNetLog.error("No dataset of slide selected for opening.",
                        new NullPointerException("No dataset or slide selected."));
                openedImageLabel.setText("No slide selected");
                return;
            }

            // load off the JavaFX Application Thread so the label can update live.
            Task<Void> loadTask = new Task<>() {
                @Override
                protected Void call() {
                    MenuActions.setStreamedServer(dsName, tsName,
                            (processed, total) -> updateMessage("Fetching " + processed + "/" + total + " tiles"));
                    return null;
                }
            };

            openImgBtn.setDisable(true);
            openedImageLabel.textProperty().bind(loadTask.messageProperty());
            loadTask.setOnSucceeded(ev -> {
                openedImageLabel.textProperty().unbind();
                openedImageLabel.setText("Opened: " + tsName);
                openImgBtn.setDisable(false);
            });
            loadTask.setOnFailed(ev -> {
                openedImageLabel.textProperty().unbind();
                openedImageLabel.setText("Failed to open slide");
                openImgBtn.setDisable(false);
                ImmuNetLog.error("Failed to open slide.", loadTask.getException());
            });

            Thread loadThread = new Thread(loadTask, "open-image");
            loadThread.setDaemon(true);
            loadThread.start();
        });

        HBox openImgRow = new HBox(10, openImgBtn, openedImageLabel);
        openImgRow.setAlignment(Pos.CENTER_LEFT);

        updateSlideByDataset(dsBox, tsBox);

        sidePanelTab.getChildren().addAll(loadDataBtn, dsBox.getBox(), tsBox.getBox(), openImgRow);

        return sidePanelTab;
    }

    private List<String> getDatasets(){
        return ServerRequestHandler.getWebpageAsList("datasets/");
    }

    private List<String> getSlides(String dataset){
        return ServerRequestHandler.getWebpageAsList("datasets/" + dataset + "/");
    }

    private void updateSlideByDataset(ListViewerBox datasetBox, ListViewerBox slideBox){
        datasetBox.getListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null){
                    MenuActions.updateListViewerBox(slideBox, getSlides(newValue)); // Update tissue slide box
                    ImmuNetLog.log("Selected: " + newValue);
                }
            }
        );
    }
}
