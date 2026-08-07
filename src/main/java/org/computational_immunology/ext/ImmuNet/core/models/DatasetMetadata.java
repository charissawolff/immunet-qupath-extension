package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class DatasetMetadata {
    private final String id;
    private final String name;
    private final String panelName;
    private final String vectraVersion;
    private final AntibodyPanel antibodyPanel;
    private final List<Integer> channelReordering;

    public DatasetMetadata(JSONObject json) {
        this.id = json.getString("_id");
        this.name = json.getString("name");
        this.panelName = json.getString("panelname");
        this.vectraVersion = json.getString("vectra_version");
        this.antibodyPanel = new AntibodyPanel(json.getJSONObject("antibodypanel"));
        this.channelReordering = toIntList(json.getJSONArray("channelreordering"));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPanelName() {
        return panelName;
    }

    public String getVectraVersion() {
        return vectraVersion;
    }

    public AntibodyPanel getAntibodyPanel() {
        return antibodyPanel;
    }

    public List<Integer> getChannelReordering() {
        return channelReordering;
    }

    public static class AntibodyPanel {
        private final String id;
        private final String name;
        private final List<String> channels;
        private final Map<String, int[]> defaultColors;
        private final int phenoChannelsNum;
        private final String displayName;
        private final String defaultModel;

        public AntibodyPanel(JSONObject json) {
            this.id = json.getString("_id");
            this.name = json.getString("name");
            this.channels = toStringList(json.getJSONArray("channels"));
            this.defaultColors = parseDefaultColors(json.getJSONArray("default_colors"));
            this.phenoChannelsNum = json.getInt("pheno_channels_num");
            this.displayName = json.getString("display_name");
            this.defaultModel = json.getString("default_model");
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<String> getChannels() {
            return channels;
        }

        public int getChannelCount() {
            return channels.size();
        }

        public Map<String, int[]> getDefaultColors() {
            return defaultColors;
        }

        public int getPhenoChannelsNum() {
            return phenoChannelsNum;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        private static Map<String, int[]> parseDefaultColors(JSONArray array) {
            Map<String, int[]> colors = new LinkedHashMap<>();
            for (int i = 0; i < array.length(); i++) {
                JSONArray pair = array.getJSONArray(i);
                String channel = pair.getString(0);
                JSONArray rgb = pair.getJSONArray(1);
                colors.put(channel, new int[] { rgb.getInt(0), rgb.getInt(1), rgb.getInt(2) });
            }
            return colors;
        }
    }

    private static List<String> toStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    private static List<Integer> toIntList(JSONArray array) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getInt(i));
        }
        return list;
    }
}
