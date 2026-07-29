package org.computational_immunology.ext.ImmuNet.ui;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ListViewerBox {
    VBox box;
    ListView<String> listView;

    public ListViewerBox(double height, double width){
        this.box = createBoxOutline(height, width);
        this.listView = new ListView<>(); 
        this.box.getChildren().add(listView); 
    }

    public void setItems(List<String> items){
        listView.setItems(FXCollections.observableArrayList(items));
    }

    public ListView<String> getListView(){
        return listView;
    }

    public VBox getBox(){
        return box;
    }

    // Create box with black outline
    private VBox createBoxOutline(double height, double width) {
        VBox newBox = new VBox();
        // Create black outline
        newBox.setBorder(new Border(new BorderStroke(Color.BLACK, 
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        // Set dimensions
        newBox.setMaxWidth(width);
        newBox.setMaxHeight(height);
        return newBox;
    }
}