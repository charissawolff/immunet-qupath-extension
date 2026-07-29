package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.InputStream;
import java.net.http.HttpResponse;

public interface PageFetcher {
    HttpResponse<InputStream> fetchPage(String localPath);
    HttpResponse<String> fetchStringPage(String localPath);
}
