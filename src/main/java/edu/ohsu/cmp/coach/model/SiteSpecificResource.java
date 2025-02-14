package edu.ohsu.cmp.coach.model;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;

public class SiteSpecificResource {
    private File file;
    private String filename;
    private String name;
    private String hash;

    public SiteSpecificResource(File file) {
        this.file = file;
        filename = file.getName();
        int index = filename.lastIndexOf(".");
        name = index > 0 ?
                filename.substring(0, index) :
                filename;
        hash = DigestUtils.sha1Hex(filename);
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

    public String getHash() {
        return hash;
    }
}
