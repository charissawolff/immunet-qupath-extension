package org.computational_immunology.ext.ImmuNet.ui;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;

import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

public class CustomSidePanelTab {
    String name;

    public CustomSidePanelTab(String panelName) {
        this.name = panelName;
    }

    public void addCustomTab(TabPane tabpane){
        Tab tab = new Tab();
        tab.setText(this.name); 
        VBox content = getContent();
        tab.setContent(content); 
        tabpane.getTabs().add(tab);
    }

    /**
     * Get tab content.
     * @return empty box
     */
    public VBox getContent(){
        return new VBox();
    }

    public Button makeButton(String name, Dimensions dim){
        Button btn = new Button();
        btn.setText(name);
        btn.setPrefHeight(dim.getHeight());
        btn.setPrefWidth(dim.getWidth());
        return btn;
    }
    
}
