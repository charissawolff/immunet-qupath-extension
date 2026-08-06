package org.computational_immunology.ext.ImmuNet.ui.commands.annotations;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import javafx.collections.ObservableList;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

public class RegisterNewClassificationsCommand {
    private final List<PathObject> pathObjects;

    public RegisterNewClassificationsCommand(List<PathObject> pathObjects) {
        this.pathObjects = pathObjects;
    }

    public void execute(){
        ObservableList<PathClass> availablePathClasses = QuPathGUI.getInstance().getAvailablePathClasses();
        List<PathClass> newClasses = new ArrayList<>();
        for (PathObject pathObject : pathObjects) {
            PathClass pc = pathObject.getPathClass();
            if (pc == null) continue;
            if (pc == PathClass.NULL_CLASS) continue;
            if (newClasses.contains(pc)) continue;
            newClasses.add(pc);
        }
        ImmuNetLog.log("Found " + newClasses.size() + " new classification(s) in the annotations: " + newClasses);
        if (!newClasses.isEmpty()) {
            newClasses.add(PathClass.StandardPathClasses.IGNORE);
            availablePathClasses.clear();
            availablePathClasses.setAll(newClasses);
            ImmuNetLog.log("Registered " + newClasses.size() + " new classification(s) in the Class list: " + newClasses);
        }
    }
}

    
