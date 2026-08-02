package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

public interface PagePoster<T> {
    HttpResponse<String> postObject(String localPath, T payload) throws IOException, InterruptedException;
}
