package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPolygon;
import org.computational_immunology.ext.ImmuNet.core.models.DatasetMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.models.TiffConverter;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadataConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequestHandler extends DataRequestHandler { 
    private final PageFetcher pageFetcher;
    private final String DATASET_PATH = "v/datasets/";
    private final String SLIDE_PATH = "v/datasets/%s/";
    private final String DATASET_METADATA_PATH = "v/datasets/%s";
    private static final String SLIDE_POLYGONS = "v/datasets/%s/%s/polygons.json"; // datasetName, slideName
    private static final String TILE_IMAGE_PATH_FORMAT = "v/datasets/%s/%s/%s/%s.jpg"; // datasetName, slideName, tileCode, imageType
    private static final String TILEMETADATAPATH_FORMAT = "v/datasets/%s/%s/"; // datasetName, slideName
    private static final String TIFF_COMPONENTS_TILE_PATH_FORMAT = "v/datasets/%s/%s/%s/components.tiff"; //dataset, slide and tile


    private static final int MAX_CONCURRENT_THUMB_DECODES = 16;
    private static final int MAX_CONCURRENT_COMPOSITE_DECODES = 4;
    private final Semaphore thumbSemaphore = new Semaphore(MAX_CONCURRENT_THUMB_DECODES);
    private final Semaphore compositeSemaphore = new Semaphore(MAX_CONCURRENT_COMPOSITE_DECODES);
    
    private static final int MAX_CONCURRENT_TIFF_COMPONENT_FETCHES = 10;
    private final Semaphore componentsTiffSemaphore = new Semaphore(MAX_CONCURRENT_TIFF_COMPONENT_FETCHES);


    public ServerRequestHandler(PageFetcher pageFetcher) {
        super(pageFetcher);
        this.pageFetcher = pageFetcher;
    }

    public List<String> getAllDatasets() {
        JSONArray datasetData = getWebpageAsJsonArray(DATASET_PATH);;
        List<String> datasetList = new ArrayList<>();
        //convert to string
        for (int i = 0; i < datasetData.length(); i++) {
            datasetList.add(datasetData.getString(i));
        }
        return datasetList;
    }

    public List<String> getAllSlides(String datasetName) {
        String path = String.format(SLIDE_PATH, datasetName);
        JSONArray slideData = getWebpageAsJsonArray(path);
        List<String> slideList = new ArrayList<>();
        //convert to string
        for (int i = 0; i < slideData.length(); i++) {
            slideList.add(slideData.getString(i));
        }
        return slideList;
    }
    }

    public DatasetMetadata getDatasetMetadata(String datasetName) {
        String path = String.format(DATASET_METADATA_PATH, datasetName);
        JSONObject response = getWebpageAsJsonObject(path);
        return new DatasetMetadata(response);
    }

    public List<TileMetadata> getTileMetadatas(String datasetName, String slideName) throws IOException, InterruptedException {
        String path = String.format(TILEMETADATAPATH_FORMAT, datasetName, slideName);
        List<TileMetadata> tileMetadatas;
        String allTilesJson = pageFetcher.fetchStringPage(path).body();

        //ImmuNetLog.log("Path at getTileMetadatas is " + path);
        //ImmuNetLog.log(allTilesJson);

        JSONArray parsedOutput = new JSONArray(allTilesJson);
        ImmuNetLog.log("Fetched all tiles json");
        tileMetadatas = TileMetadataConverter.jsonToTileMetadatas(parsedOutput, ImageType.THUMB);
        return tileMetadatas;
    }

    public Tile fetchTileImage(String datasetName, String slideName, TileMetadata tileMetadata) throws IOException, InterruptedException {
        // Fetch the image for a specific tile using its metadata and the dataset/slide names. 
        // Check for null image and throw IOException if the image cannot be decoded.
        // The application crashes when I try to load in too fast of the composite images, so I added a semaphore to limit how many images are read
        // at the same time
        
        String path = String.format(TILE_IMAGE_PATH_FORMAT, datasetName, slideName, tileMetadata.getCode(), tileMetadata.getType().toString());
        Semaphore semaphore = tileMetadata.getType() == TileMetadata.ImageType.THUMB ? thumbSemaphore : compositeSemaphore;
        try {
            BufferedImage bImage = fetchImage(path, semaphore);
            return new Tile(tileMetadata, bImage);
        } catch (IOException e) {
            ImmuNetLog.error("Error fetching tile image for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e; // Rethrow the exception to be handled by the caller
        } catch (InterruptedException e) {
            ImmuNetLog.error("Thread interrupted while fetching tile image for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e; // Rethrow the exception to be handled by the caller
        }
    }

    public Tile fetchComponentsTiffImage(TileMetadata tileMetadata, String datasetName, String slideName)
            throws IOException, InterruptedException {
        String path = String.format(TIFF_COMPONENTS_TILE_PATH_FORMAT, datasetName, slideName, tileMetadata.getCode());
        componentsTiffSemaphore.acquire();
        try {
            byte[] bytes = fetchBytes(path);
            BufferedImage image = TiffConverter.imageFromBytes(bytes);
            return new Tile(tileMetadata, image);
        } catch (IOException e) {
            ImmuNetLog.error("Error decoding components.tiff for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e;
        } finally {
            componentsTiffSemaphore.release();
        }
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

