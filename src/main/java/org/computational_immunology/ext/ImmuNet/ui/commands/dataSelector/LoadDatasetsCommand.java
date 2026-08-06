package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.handlers.MiscDataRequestHandler;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;
import org.json.JSONArray;

public class LoadDatasetsCommand extends AbstractAsyncCommand<List<String>> {
    private final MiscDataRequestHandler dataRequestHandler;

    public LoadDatasetsCommand(MiscDataRequestHandler miscDataRequestHandler) {
        this.dataRequestHandler = miscDataRequestHandler;
    }

    @Override
    protected List<String> execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Loading dataset data");
        JSONArray datasetData = dataRequestHandler.getAllDatasets();
        List<String> datasetList = new ArrayList<>();
        //convert to string
        for (int i = 0; i < datasetData.length(); i++) {
            datasetList.add(datasetData.getString(i));
        }
        return datasetList;
    }
}
