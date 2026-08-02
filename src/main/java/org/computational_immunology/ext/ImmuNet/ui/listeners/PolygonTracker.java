package org.computational_immunology.ext.ImmuNet.ui.listeners;

import java.awt.image.BufferedImage;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyEvent;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyListener;

/* 
Tracks changes to the polygon hierarchy
Specifically, tracks when user ADDS a new polygon, so that we can save it to the server 

*/
public class PolygonTracker implements PathObjectHierarchyListener  {
    PathObjectHierarchy hierarchy;
    private final ObservableList<PathObject> newAnnotations = FXCollections.observableArrayList();

    public PolygonTracker() {
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer == null) {
            return;
        }
        if (viewer.getImageData() != null) {
            this.hierarchy = viewer.getImageData().getHierarchy();
            this.hierarchy.addListener(this);
        }
        //if the viewer changes, we need to update the hierarchy listener
        ReadOnlyObjectProperty<ImageData<BufferedImage>> imageDataProperty = viewer.imageDataProperty();
        imageDataProperty.addListener((observable, oldValue, newValue) -> {
            if (this.hierarchy != null) {
                this.hierarchy.removeListener(this);
            }
            this.hierarchy = newValue == null ? null : newValue.getHierarchy();
            if (this.hierarchy != null) {
                this.hierarchy.addListener(this);
            }
        });
    }

    @Override
    public void hierarchyChanged(PathObjectHierarchyEvent event) {
        //change the newAnnotations list to only contain annotations that are still in the hierarchy, in case the user deleted some of them
        //but that it wasn't registered (such as deleting from hierarchy tab)
        newAnnotations.removeIf(obj -> obj.getParent() == null);
        ImmuNetLog.log("Hierarchy changed: " + event);
        //filter for polygon changes and log them
        if (event.getEventType() == PathObjectHierarchyEvent.HierarchyEventType.ADDED) {
            ImmuNetLog.log("Polygon added: " + event);
        } else if (event.getEventType() == PathObjectHierarchyEvent.HierarchyEventType.REMOVED) {
            ImmuNetLog.log("Polygon removed: " + event);
        } else if (event.getEventType() == PathObjectHierarchyEvent.HierarchyEventType.CHANGE_MEASUREMENTS) {
            ImmuNetLog.log("Polygon changed: " + event);
        }

        if (event.getChangedObjects() != null){ 
            ImmuNetLog.log("Polygon class changed: " + event.getChangedObjects());
        }

        // any newly drawn annotation gets tracked so it can later be sent to the server, except points
        if (event.getEventType() == PathObjectHierarchyEvent.HierarchyEventType.ADDED) {
            for (PathObject addedObject : event.getChangedObjects()) {
                if (!addedObject.getROI().isPoint()) {
                    newAnnotations.add(addedObject);
                }
            }
        }
        //if we removed a polygon, we want to remove it from the newAnnotations list so that we don't send it to the server
        if (event.getEventType() == PathObjectHierarchyEvent.HierarchyEventType.REMOVED) {
            for (PathObject removedObject : event.getChangedObjects()) {
                    newAnnotations.remove(removedObject);
                
            }
        }
    }

    public ObservableList<PathObject> getNewAnnotations() {
        return newAnnotations;
    }
}
    


    