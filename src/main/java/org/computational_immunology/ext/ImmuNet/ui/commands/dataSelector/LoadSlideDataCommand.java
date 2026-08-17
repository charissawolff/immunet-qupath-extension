package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import java.util.List;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.api.ServerGateway;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;

public class LoadSlideDataCommand extends AbstractAsyncCommand<List<String>> {
    private final ServerGateway serverGateway;
    private final String datasetName;

    public LoadSlideDataCommand(ServerGateway serverGateway, String datasetName) {
        this.serverGateway = serverGateway;
        this.datasetName = datasetName;
    }

    @Override
    protected List<String> execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Loading slide data for dataset: " + datasetName);
        return serverGateway.getAllSlides(datasetName);
    }
}
