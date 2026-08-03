package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.handlers.MiscDataRequestHandler;
import org.json.JSONArray;

public class LoadSlideDataCommand extends AbstractAsyncCommand<List<String>> {
    private final MiscDataRequestHandler dataRequestHandler;
    private final String datasetName;

    public LoadSlideDataCommand(MiscDataRequestHandler miscDataRequestHandler, String datasetName) {
        this.dataRequestHandler = miscDataRequestHandler;
        this.datasetName = datasetName;

    }

    @Override
    protected List<String> execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Loading slide data for dataset: " + datasetName);
        JSONArray slideData = dataRequestHandler.getAllSlides(datasetName);
        List<String> slideList = new ArrayList<>();
        //convert to string
        for (int i = 0; i < slideData.length(); i++) {
            slideList.add(slideData.getString(i));
        }
        return slideList;
    }
}
