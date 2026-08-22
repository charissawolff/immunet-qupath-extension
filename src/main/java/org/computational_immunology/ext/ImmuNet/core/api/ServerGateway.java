package org.computational_immunology.ext.ImmuNet.core.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.converters.AnnotationPointConverter;
import org.computational_immunology.ext.ImmuNet.core.converters.ImageConverter;
import org.computational_immunology.ext.ImmuNet.core.converters.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.converters.TiffConverter;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPolygon;
import org.computational_immunology.ext.ImmuNet.core.models.DatasetMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadataConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerGateway extends DataRequestHandler {
    private final PageFetcher pageFetcher;
    private final String DATASET_PATH = "v/datasets/";
    private final String SLIDE_PATH = "v/datasets/%s/";
    private final String DATASET_METADATA_PATH = "v/datasets/%s";
    private static final String SLIDE_POLYGONS = "v/datasets/%s/%s/polygons.json"; // datasetName, slideName
    private static final String SLIDE_ANNOTATIONS = "/v/annotations/%s/%s/"; // datasetName, slideName
    private static final String TILE_ANNOTATIONS = "v/datasets/%s/%s/%s/annotations.json"; // datasetName, slideName, tileCode
    private static final String TILE_IMAGE_PATH_FORMAT = "v/datasets/%s/%s/%s/%s.jpg"; // datasetName, slideName, tileCode, imageType
    private static final String TILEMETADATAPATH_FORMAT = "v/datasets/%s/%s/"; // datasetName, slideName
    private static final String TIFF_COMPONENTS_TILE_PATH_FORMAT = "v/datasets/%s/%s/%s/components.tiff?downsample=%d"; //dataset, slide and tile
    private static final String SLIDE_CHANNELS_PATH_FORMAT = "v/datasets/%s/%s/channels"; // datasetName, slideName

    public ServerGateway(PageFetcher pageFetcher) {
        super(pageFetcher);
        this.pageFetcher = pageFetcher;
    }

    public List<String> getAllDatasets() throws IOException, InterruptedException {
        JSONArray datasetData = getWebpageAsJsonArray(DATASET_PATH);
        List<String> datasetList = new ArrayList<>();
        for (int i = 0; i < datasetData.length(); i++) {
            datasetList.add(datasetData.getString(i));
        }
        return datasetList;
    }

    public List<String> getAllSlides(String datasetName) throws IOException, InterruptedException {
        String path = String.format(SLIDE_PATH, datasetName);
        JSONArray slideData = getWebpageAsJsonArray(path);
        List<String> slideList = new ArrayList<>();
        for (int i = 0; i < slideData.length(); i++) {
            slideList.add(slideData.getString(i));
        }
        return slideList;
    }

    public DatasetMetadata getDatasetMetadata(String datasetName) throws IOException, InterruptedException {
        String path = String.format(DATASET_METADATA_PATH, datasetName);
        JSONObject response = getWebpageAsJsonObject(path);
        return new DatasetMetadata(response);
    }

    public List<String> getSlideChannels(String datasetName, String slideName) throws IOException, InterruptedException {
        //map so that i know which channel is which
        String path = String.format(SLIDE_CHANNELS_PATH_FORMAT, datasetName, slideName);
        JSONArray channelData = getWebpageAsJsonArray(path);
        List<String> channelList = new ArrayList<>();
        for (int i = 0; i < channelData.length(); i++) {
            channelList.add(channelData.getString(i));
        }
        return channelList;
    }

    public List<TileMetadata> getTileMetadatas(String datasetName, String slideName) throws IOException, InterruptedException {
        String path = String.format(TILEMETADATAPATH_FORMAT, datasetName, slideName);
        String allTilesJson = pageFetcher.fetchStringPage(path).body();
        JSONArray parsedOutput = new JSONArray(allTilesJson);
        return TileMetadataConverter.jsonToTileMetadatas(parsedOutput, ImageType.THUMB);
    }

    public Tile fetchTileImage(String datasetName, String slideName, TileMetadata tileMetadata) throws IOException, InterruptedException {
        String path = String.format(TILE_IMAGE_PATH_FORMAT, datasetName, slideName, tileMetadata.getCode(), tileMetadata.getType().toString());
        try {
            byte[] bytes = fetchBytes(path);
            BufferedImage image = ImageConverter.imageFromBytes(bytes, tileMetadata.getType());
            return new Tile(tileMetadata, image);
        } catch (IOException e) {
            ImmuNetLog.error("Error fetching tile image for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e; // Rethrow the exception to be handled by the caller
        } catch (InterruptedException e) {
            ImmuNetLog.error("Thread interrupted while fetching tile image for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e; // Rethrow the exception to be handled by the caller
        }
    }

    public Tile fetchComponentsTiffImage(TileMetadata tileMetadata, String datasetName, String slideName, int downsampleFactor)
            throws IOException, InterruptedException {
        //round up the downsample factor to the nearest integer so that the server can actually process it, because it treats non int numbers as 1 which is NOT what we want.
        String path = String.format(TIFF_COMPONENTS_TILE_PATH_FORMAT, datasetName, slideName, tileMetadata.getCode(), downsampleFactor);
        try {
            ImmuNetLog.log("Fetching components.tiff for downsample {} at path: {}", downsampleFactor, path);
            byte[] bytes = fetchBytes(path);
            BufferedImage image = TiffConverter.imageFromBytes(bytes);
            return new Tile(tileMetadata, image);
        } catch (IOException e) {
            ImmuNetLog.error("Error decoding components.tiff for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e;
        }
    }

    public List<String> fetchSlideAnnotations(String dataset, String slide) throws IOException, JSONException, InterruptedException {
        String path = String.format(SLIDE_ANNOTATIONS, dataset, slide);
        List<String> tileCodes = new ArrayList<>();

        HttpResponse<String> response = pageFetcher.fetchStringPage(path);

        int status = response.statusCode();
        if (status == 404) {
            return tileCodes; // no annotations for this dataset/slide
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Could not fetch annotations for dataset: " + dataset
                    + " with slide: " + slide + " (status " + status + ")");
        }

        String body = response.body().trim();
        if (body.isEmpty()) {
            return tileCodes; // empty body means there are no annotations
        }

        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            tileCodes.add(array.getString(i));
        }
        return tileCodes;
    }

    public List<AnnotationPoint> fetchAnnotations(String dataset, String slide, String tile) throws IOException, JSONException, InterruptedException {
        String path = String.format(TILE_ANNOTATIONS, dataset, slide, tile);
        List<AnnotationPoint> annotations = new ArrayList<>();

        HttpResponse<String> response = pageFetcher.fetchStringPage(path);

        int status = response.statusCode();
        if (status == 404) {
            return annotations; // no annotations for this dataset/slide
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Could not fetch annotations for dataset: " + dataset
                    + " with slide: " + slide + " (status " + status + ")");
        }

        String body = response.body().trim();
        if (body.isEmpty()) {
            return annotations; // empty body means there are no annotations
        }

        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            annotations.add(AnnotationPointConverter.fromJson(array.getJSONObject(i)));
        }
        return annotations;
    }

    public List<AnnotationPolygon> fetchPolygons(String dataset, String slide) throws IOException, JSONException, InterruptedException {
        String path = String.format(SLIDE_POLYGONS, dataset, slide);
        List<AnnotationPolygon> polygons = new ArrayList<>();

        HttpResponse<String> response = pageFetcher.fetchStringPage(path);

        int status = response.statusCode();
        if (status == 404) {
            return polygons; // no polygons for this dataset/slide
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Could not fetch polygons for dataset: " + dataset
                    + " with slide: " + slide + " (status " + status + ")");
        }

        String body = response.body().trim();
        if (body.isEmpty()) {
            return polygons; // empty body means there are no polygons
        }

        JSONArray array = new JSONArray(body);
        return PolygonConverter.fromJsonArray(array);
    }
}
