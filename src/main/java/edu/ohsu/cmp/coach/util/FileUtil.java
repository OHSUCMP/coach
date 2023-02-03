package edu.ohsu.cmp.coach.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static List<String> getResourceFilenameList(String resourcePath) throws IOException {
        List<String> list = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String resource;
                while ((resource = reader.readLine()) != null) {
                    list.add(resource);
                }
            } else {
                logger.error("couldn't obtain resource as stream for resourcePath=" + resourcePath);
            }
        }

        return list;
    }

    public static List<String> getFilenameList(String path) throws IOException {
        List<String> list = new ArrayList<>();

        File dir = new File(path);
        File[] fileArr = dir.listFiles();
        if (fileArr != null) {
            for (File file : fileArr) {
                list.add(file.getName());
            }

        } else {
            throw new IOException(path + " is not a directory");
        }

        return list;
    }

    public static String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }
}
