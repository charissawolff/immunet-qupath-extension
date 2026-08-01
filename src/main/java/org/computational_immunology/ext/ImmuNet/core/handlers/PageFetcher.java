package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

public interface PageFetcher {
    HttpResponse<InputStream> fetchPage(String localPath) throws IOException, InterruptedException;
    HttpResponse<String> fetchStringPage(String localPath) throws IOException, InterruptedException;
}
