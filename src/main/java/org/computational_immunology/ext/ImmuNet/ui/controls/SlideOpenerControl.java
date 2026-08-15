package org.computational_immunology.ext.ImmuNet.ui.controls;

import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.geometry.Insets;
import java.util.function.BooleanSupplier;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverController;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.SlideLoadWorkflow;
import org.computational_immunology.ext.ImmuNet.ui.tabBoxes.DataListViewerBox;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;

public class SlideOpenerControl extends VBox {

    private final ServerGateway serverGateway;
    private final SelectedDataStore selectedDataStore;
    private final TileHoverController tileHoverController;
    private final DataListViewerBox dataListViewerBox;
    private final BooleanSupplier useTiffComposite;

    private final Button openImgBtn = new Button("Open Slide");
    private final Label statusLabel = new Label();
    private final PauseTransition buttonPause = new PauseTransition(Duration.seconds(2));
    private SlideLoadWorkflow currentWorkflow;

    public SlideOpenerControl(ServerGateway serverGateway, SelectedDataStore selectedDataStore,
                               TileHoverController tileHoverController, DataListViewerBox dataListViewerBox,
                               BooleanSupplier useTiffComposite) {
        this.serverGateway = serverGateway;
        this.selectedDataStore = selectedDataStore;
        this.tileHoverController = tileHoverController;
        this.dataListViewerBox = dataListViewerBox;
        this.useTiffComposite = useTiffComposite;

        openImgBtn.setPrefHeight(30);
        openImgBtn.setPrefWidth(120);

        buttonPause.setOnFinished(event -> {
            openImgBtn.setStyle("-fx-text-fill: black;");
            openImgBtn.setText("Open Slide");
        });

        openImgBtn.setOnAction(e -> handleOpenSlideClicked());

        //set a gap between the button and the status label, and make the status label invisible by default
        statusLabel.setPadding(new Insets(3, 0, 10, 0));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        getChildren().addAll(openImgBtn, statusLabel);
    }

    //WE HAVE to make that if this button is clicked while the workflow is running,
    // it will ONLY cancel the workflow and clear the viewer.
    // Otherwise, if the user clicks this button again, it will start a new workflow while the old one is still running,
    //  which can cause problems.
    private void handleOpenSlideClicked() {
        if (dataListViewerBox.getSelectedDataset() == null || dataListViewerBox.getSelectedSlide() == null) {
            ImmuNetLog.error("No dataset or slide selected for opening.");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            statusLabel.setText("No dataset or slide selected for opening.");
            return;
        }
        try {
            String dsName = dataListViewerBox.getSelectedDataset();
            String tsName = dataListViewerBox.getSelectedSlide();
            if (currentWorkflow != null && !currentWorkflow.isDone()) {
                currentWorkflow.cancel();
                return;
            }
            SlideLoadWorkflow workflow = new SlideLoadWorkflow(dsName, tsName, serverGateway, useTiffComposite.getAsBoolean(), selectedDataStore);
            workflow.build();

            workflow.setOnSlideReady(slide -> {
                QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
                if (viewer != null) {
                    tileHoverController.setSlide(slide, viewer);
                }
            });

            workflow.messageProperty().addListener((obs, oldMsg, newMsg) -> statusLabel.setText(newMsg));
            workflow.stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    openImgBtn.setStyle("-fx-text-fill: green;");
                    openImgBtn.setText("Success");
                } else if (newState == Worker.State.CANCELLED) {
                    openImgBtn.setStyle("-fx-text-fill: red;");
                    openImgBtn.setText("Cancelled");
                    statusLabel.setVisible(false);
                    statusLabel.setManaged(false);
                } else if (newState == Worker.State.FAILED) {
                    openImgBtn.setStyle("-fx-text-fill: red;");
                    openImgBtn.setText("Failed");
                    statusLabel.setVisible(false);
                    statusLabel.setManaged(false);
                } else {
                    return;
                }
                buttonPause.stop();
                buttonPause.playFromStart();
            });

            workflow.start();
            currentWorkflow = workflow;

            openImgBtn.setText("Cancel");
            openImgBtn.setStyle("-fx-text-fill: black;");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            buttonPause.stop();
        } catch (NullPointerException exc) {
            ImmuNetLog.error("No dataset of slide selected for opening.", exc);
        }
    }
}