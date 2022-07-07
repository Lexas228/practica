package ru.vsu.main;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Objects;

public class FileUtil {


    public static byte[] readData(String path) throws IOException {
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        byte[] bytes = new byte[Objects.requireNonNull(inputStream).available()];
        inputStream.read(bytes);
        return bytes;
    }


    public static void writeData(String path, byte[] bytes) throws URISyntaxException, IOException {
        File outputFile = new File("./" + path);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
