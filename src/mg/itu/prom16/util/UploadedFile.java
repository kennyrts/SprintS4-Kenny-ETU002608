package mg.itu.prom16.util;

import java.io.InputStream;

public class UploadedFile {
    private String fileName;
    private String contentType;
    private long size;
    private InputStream inputStream;

    public UploadedFile(String fileName, String contentType, long size, InputStream inputStream) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.inputStream = inputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
