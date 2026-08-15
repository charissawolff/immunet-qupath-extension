package org.computational_immunology.ext.ImmuNet.ui.listeners;

import java.awt.image.BufferedImage;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyEvent;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyListener;

/**
* Tracks changes to the polygon hierarchy
* Specifically, tracks when user ADDS a new polygon, so that we can save it to the server 
* Note: when I change the figure of a polygon currently there, the events change other fire, and then it's reigistered as "added"
* Sometimes when I create the polygon it also first fired change "other" and then "added". I have to check if the polygon is already in the newAnnotations list before adding it, to avoid duplicates.
*/
public class PolygonTracker implements PathObjectHierarchyListener  {
    PathObjectHierarchy hierarchy;
    private final ObservableList<PathObject> newAnnotations = FXCollections.observableArrayList();
    private final ObservableBooleanValue enabled;

    public PolygonTracker(ObservableBooleanValue enabled) {
        this.enabled = enabled;
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer == null) {
            return;
        }
        if (viewer.getImageData() != null) {
            this.hierarchy = viewer.getImageData().getHierarchy();
            this.hierarchy.addListener(this);
        }
        //if the viewer changes, we need to update the hierarchy listener AND the newAnnotations list 
        ReadOnlyObjectProperty<ImageData<BufferedImage>> imageDataProperty = viewer.imageDataProperty();
        imageDataProperty.addListener((observable, oldValue, newValue) -> {
            newAnnotations.clear(); //clear the newAnnotations list when the viewer changes,
                //  since we don't want to keep track of polygons from a different image
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
        if (!enabled.get()) {
            return;
        }
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
                    ImmuNetLog.log("Tracking new polygon annotation: " + addedObject);
                    ImmuNetLog.log("Tracking new polygon annotation: " + addedObject.getROI().getClass().getSimpleName());
                    ImmuNetLog.log("It had the type of " + event.getEventType());
                    newAnnotations.remove(addedObject); //remove it first in case it was already there, to avoid duplicates
                    if (!newAnnotations.contains(addedObject)) {
                        newAnnotations.add(addedObject);
                    }
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
    


    