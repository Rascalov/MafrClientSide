package org.mafr.model;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MoodleFile {
    private String name;
    private String downloadLink;
    private int size;
    private long LastModified;

    public MoodleFile(String downloadLink){
        this.downloadLink = downloadLink.replaceAll(" ", "%20");
    }
    public void setSize(int sizeInBytes){
        this.size = sizeInBytes;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setLastModified(String httpHeaderDateTime){
        this.LastModified = LocalDateTime.parse(httpHeaderDateTime, DateTimeFormatter.RFC_1123_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public long getLastModified() {
        return LastModified;
    }
    public static long httpDateTimeHeaderToMs(String httpHeaderDateTime){
        return LocalDateTime.parse(httpHeaderDateTime, DateTimeFormatter.RFC_1123_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    public void download(String path){

    }
}