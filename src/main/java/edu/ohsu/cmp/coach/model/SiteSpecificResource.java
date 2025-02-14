package edu.ohsu.cmp.coach.model;

import java.io.File;

public class SiteSpecificResource {
    private File file;
    private String filename;
    private String name;
    private String key;

    public SiteSpecificResource(File file) {
        this.file = file;
        filename = file.getName();

        int index = filename.lastIndexOf(".");
        String n = index > 0 ? filename.substring(0, index) : filename;
        name = n.replaceAll("[^a-zA-Z0-9()_,' -]", "").trim();

        key = name.replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_-]", "")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "")
                .trim();
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }
}
