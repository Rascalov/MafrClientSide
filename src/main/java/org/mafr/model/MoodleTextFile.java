package org.mafr.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MoodleTextFile extends MoodleFile{
    private String text;
    public MoodleTextFile(String downloadLink) {
        super(downloadLink);
    }

    @Override
    public void download(String path) {
        new File(path + "/" + this.getName());
        try {
            Files.write(Paths.get(path + "/" + getName() + ".html"),text.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("IO error .txt: " + e.getMessage());
        }
    }

    public MoodleTextFile(String text, String downloadLink){
        super(downloadLink);
        this.text = text;
    }
    @Override
    public String getName(){
        return super.getName() + ".html";
    }
}
