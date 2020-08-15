package org.mafr.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class MoodlePreferences {
    private Preferences preferences;
    public MoodlePreferences(){
        this.preferences = Preferences.userRoot().node(this.getClass().getName());
    }

    public void rememberServerCredentials(String serverURL, String serverPassword){
        preferences.put("serverURL", serverURL);
        preferences.put("serverPassword", serverPassword);
    }
    public HashMap<String,String> getServerCredentials(){
        String serverURL = preferences.get("serverURL", "");
        String serverPassword = preferences.get("serverPassword", "");

        HashMap<String,String> map = new HashMap<>();
        map.put("serverURL", serverURL);
        map.put("serverPassword", serverPassword);
        return map;
    }
    public boolean serverCredentialsExist(){
        String check = preferences.get("serverURL", "");
        return !check.equals("");
    }
    public void clearCredentials() throws BackingStoreException {
        preferences.clear();
    }

    public void saveCourse(String courseId, String type, String location, String id) {
        preferences.put("course_" + id, courseId + ";" + type + ";" + location + "" );
    }
    public HashMap<String, String> getSavedCourses(){
        // linked hashmaps keep the order, which will keep the courses in the order the user had before closing.
        LinkedHashMap<String,String> map = new LinkedHashMap<>();
        try {
            for(var key : preferences.keys()){
                if(key.startsWith("course_")){
                    map.put(key, preferences.get(key, ""));
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        return map;
    }
}
