package org.computational_immunology;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class Tile {
    private int id;
    private String fullImagePath;
    private String type;
    private String code;

    private BufferedImage bi = null;
    // Cache of pre-shrunk copies keyed by downsample factor (pyramid levels).
    private final Map<Integer, BufferedImage> downsampledCache = new HashMap<>();
    //real-world tile xy pos
    private double tileX;
    private double tileY;

    //real-world tile wh size
    public double tileW;
    public double tileH;

    public enum ImageType {
        THUMB, COMPOSITE;

        @Override
        public String toString(){
            return this.name().toLowerCase();
        }
    }


    /**
     * Tile constructor
     * @param id - identifier of tile order
     * @param path - path to tile on server. Format: dataset/slide
     * @param imageType - what type of image the tile should be. Options: [thumb, composite].
     * @param code - code of tile order. Identifies the tile.
     * @param x - x coordinate of tile
     * @param y - y coordinate of tile
     * @param w - width of image
     * @param h - height of image
     * @throws IOException
     */
    public Tile(int id, String path, ImageType imageType, String code, double x, double y, double w, double h) {
        if (id < 0) {
            throw new IllegalArgumentException("id cannot be negative");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("width and height cannot be negative or zero");
        }
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }
        this.id = id;
        this.code = code;
        this.fullImagePath = createImagePath(path, imageType.toString());
        this.type = imageType.toString();
        this.tileX = x;
        this.tileY = y;
        this.tileW = w;
        this.tileH = h;
    }

    public String getCode(){
        return this.code;
    }
    
    private String createImagePath(String path, String imageType) {
        if (path.isEmpty() || !path.substring(path.length() - 1).equals("/")) {
            return (path + "/" + this.code + "/" + imageType + ".jpg");
        }
        return (path + this.code + "/" + imageType + ".jpg");
    }
    // Get full path name of tile
    public String getPath(){
        return this.fullImagePath;
    }

    public String getType(){
        return this.type;
    }

    public int getId(){
        return this.id;
    }

    public double getTileX(){
        return this.tileX;
    }
    
    public double getTileY(){
        return this.tileY;
    }

    static BufferedImage resizeImage(BufferedImage img, int targetWidth, int targetHeight, boolean qualityOverSpeed) {
        BufferedImage bufferedImg = new BufferedImage(targetWidth, targetHeight, img.getType());
        Graphics2D g2d = bufferedImg.createGraphics();
        
        if (qualityOverSpeed) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        
        g2d.drawImage(img, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return bufferedImg;
    }

    public BufferedImage getImage() throws IOException, IllegalArgumentException {
        return getImage(1);
    }

    /**
     * Returns the tile image, optionally pre-shrunk by the given downsample factor.
     * Full-resolution pixels are fetched from the server on first access; downsampled
     * copies are derived from them and cached per downsample so a pyramid level is only
     * computed once.
     *
     * @param downsample factor to shrink by ({@code <= 1} returns full resolution)
     */
    public synchronized BufferedImage getImage(int downsample) throws IOException, IllegalArgumentException {
        BufferedImage full = getFullImage();
        if (downsample <= 1) {
            return full;
        }
        BufferedImage cached = downsampledCache.get(downsample);
        if (cached == null) {
            int width = Math.max(1, (int)this.tileW / downsample);
            int height = Math.max(1, (int)this.tileH / downsample);
            cached = resizeImage(full, width, height, false);
            downsampledCache.put(downsample, cached);
        }
        return cached;
    }

    private BufferedImage getFullImage() throws IOException, IllegalArgumentException {
        if (bi == null) {
            InputStream webpage = ServerConnectionHandler.getInstance().fetchPage(getPath()).body();
            ImmuNetLog.log("Fetched URL is " + getPath());
            bi = ImageIO.read(webpage);
            if (bi == null) {
                ImmuNetLog.error("No valid image received");
                throw new IOException("No valid image received");
            } else {
                ImmuNetLog.log("image read.");
            }

            /*
             * For now we only support thumbnail images, where quality does not matter too
             * much.
             * TODO: There's a good chance QuPAth has a way of scaling this image when
             * TODO: rendering. Rescaling in-memory is needlessly resource intensive!
             */
            boolean qualityOverSpeed = false;
            bi = resizeImage(bi, (int)this.tileW, (int)this.tileH, qualityOverSpeed);
        }
        return bi;
    }
}