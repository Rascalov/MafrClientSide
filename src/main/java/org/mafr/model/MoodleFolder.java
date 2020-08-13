package org.mafr.model;

import java.util.ArrayList;
import java.util.List;

public class MoodleFolder {
    private String name;
    private int sectionId;
    private String path; // decided to throw it in where necessary, it can be set whenever desired
    private boolean snapshotable =true;

    public void setSnapshotable(boolean snapshottable) {
        this.snapshotable = snapshottable;
    }

    public boolean isSnapshotable() {
        return snapshotable;
    }

    private List<MoodleFile> moodleFiles;
    private List<MoodleFolder> moodleFolders;


    public MoodleFolder(String foldername) { // Constructor for Course Folders
        this.name = foldername.replaceAll("[^a-zA-Z0-9\\.\\-]", "-"); // prevents use of chars that break folder making
        moodleFiles = new ArrayList<>();
        moodleFolders = new ArrayList<>();
    }
    public MoodleFolder(String foldername, int sectionId) {
        this(foldername);
        this.sectionId = sectionId; // section id determines the url required to force the request to show appropriate content
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSectionId() {
        return sectionId;
    }

    public void setSectionId(int sectionId) {
        this.sectionId = sectionId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<MoodleFile> getMoodleFiles() {
        return moodleFiles;
    }

    public List<MoodleFolder> getMoodleFolders() {
        return moodleFolders;
    }
}