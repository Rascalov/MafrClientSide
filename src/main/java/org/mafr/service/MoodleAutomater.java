package org.mafr.service;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mafr.model.MoodleFile;
import org.mafr.model.MoodleFolder;
import org.mafr.model.MoodleTextFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoodleAutomater {
    Logger logger = Logger.getLogger(MoodleAutomater.class.getName());
    private String courseStartUrl = "https://moodle.inholland.nl/course/view.php?id=";
    private int courseId;
    private String serverUrl;
    private String serverPassword;

    public MoodleAutomater(int courseId, String serverUrl, String serverPassword){
        this.courseId = courseId;
        this.serverUrl = serverUrl;
        this.serverPassword = serverPassword;
    }

    public MoodleFolder getCourseStructure() {
        Document doc = null;
        try {
            doc = Jsoup.connect(serverUrl + "/content")
                    .data("courseId", String.valueOf(courseId))
                    .data("type", "structure")
                    .header("moodleServerAuth", serverPassword)
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .get();
        } catch (IOException e) {
            // Todo: log?
            if(e instanceof HttpStatusException){
                HttpStatusException hse = (HttpStatusException) e;
                if(hse.getStatusCode() == 401)
                    throw new RuntimeException("Error: Incorrect password or URL");
                else if (hse.getStatusCode() == 400)
                    throw new RuntimeException("Error: Course does not exist or can not be enrolled in." +
                            " Contact the server owner for more information.");
                else {
                    System.out.println("message: " + hse.getMessage());
                    System.out.println("code: " + hse.getStatusCode());
                    System.out.println(hse.getLocalizedMessage());
                    throw new RuntimeException("Error: Unknown HttpStatusException...");
                }
            }
            else {
                System.out.println(e);
                System.out.println(e.getMessage());
                throw new RuntimeException("Connection timeout");
            }
        }
        Elements heading = doc.select("div.page-header-headings");
        MoodleFolder mainFolder = new MoodleFolder(heading.text());
        if(doc.selectFirst("ul.mt-topmenu") != null){
            for (Element menuOption : doc.selectFirst("ul.mt-topmenu").select("a")){
                String folderName = menuOption.selectFirst("span.mt-btn-text").text();
                String dataPane = menuOption.attr("data-pane");
                Element mtMenu = doc.selectFirst("div[data-pane=" + dataPane + "]");
                MoodleFolder menuFolder = new MoodleFolder(folderName, Integer.parseInt(menuOption.attr("data-section")));
                if(mtMenu.select("div.mt-sidemenu") != null)
                    menuFolder = getLiteralStructure(menuFolder, mtMenu.selectFirst("div.mt-sitemenus"));
                mainFolder.getMoodleFolders().add(menuFolder);
            }
        }
        else {
            Element element = doc.select("div.mt-sitemenus").get(0);
            mainFolder = getLiteralStructure(mainFolder, element);
        }
        return mainFolder;
    }

    public void downloadFile(MoodleFile file, String path){
        // get jsoup response and download file.
        if(file instanceof MoodleTextFile) // but if it's a text/html file, let it get the string bytes
            ((MoodleTextFile)file).download(path);
        else {
            String dl = file.getDownloadLink();
            try {
                dl = URLEncoder.encode(dl, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Connection.Response response = null;
            try {
                response = Jsoup.connect(serverUrl+"/download")
                        .header("moodleServerAuth", serverPassword)
                        .header("downloadLink", dl)
                        .method(Connection.Method.POST)
                        .maxBodySize(0)
                        .execute();
            }catch (IOException e){
                if(e instanceof HttpStatusException){
                    HttpStatusException hse = (HttpStatusException) e;
                    System.out.println(hse.getStatusCode());
                }
                System.out.println("File that caused error: "+file.getDownloadLink());
                throw new RuntimeException(e.getMessage());
            }
            try {
                response.bodyStream().transferTo(new FileOutputStream(path + "/" + file.getName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private MoodleFolder getLiteralStructure(MoodleFolder mainfolder, Element courseSubjects){
        var children = courseSubjects.children();
        for (Element subject : children ){
            var subjectElement = subject.select(".mt-heading-btn").get(0);
            MoodleFolder subjectFolder = new MoodleFolder(subjectElement.text(), Integer.parseInt(subjectElement.attr("data-section")));
            System.out.println(subjectFolder.getName() + " Is the Subject Folder");
            if(subject.children().size() > 1){
                var subjectBody =  subject.child(1).child(0).child(0);
                subjectFolder.getMoodleFolders().addAll(getSubfolders(subjectBody));
            }
            mainfolder.getMoodleFolders().add(subjectFolder);
            System.out.println("-----------------------------------------------------------------------------");
        }

        return mainfolder;
    }
    List<MoodleFolder> getSubfolders(Element sublevelList){
        // Handle additional subfolders and, sometimes, subfolders within subfolders (aka multi-level folders).
        List<MoodleFolder> moodleFolders = new ArrayList<>();
        MoodleFolder folder = null; // Matriarch folder in case of multi-level folders
        for(Element section : sublevelList.children()){
            if(section.is("a")){
                folder = new MoodleFolder(section.text(), Integer.parseInt(section.attr("data-section")));
                moodleFolders.add(folder);
            }
            else if(section.is("div"))
                // Sub folder has a subfolder, so repeat the method.
                moodleFolders.get(moodleFolders.indexOf(folder)).getMoodleFolders().addAll(getSubfolders(section));
        }
        return moodleFolders;
    }
    public MoodleFolder getContentSection(MoodleFolder folder) throws IOException {
        // returns the folder with the (potential) files it holds.
        Document doc=null;
        doc = Jsoup.connect(serverUrl + "/content")
                .data("courseId", String.valueOf(courseId))
                .data("type", "content")
                .data("sectionId", String.valueOf(folder.getSectionId()))
                .header("moodleServerAuth", serverPassword)
                .userAgent("Mozilla")
                .timeout(5000)
                .get();
        List<MoodleFile> files = new ArrayList<>();
        files.addAll(findFilesGenerically(doc.selectFirst("div.section-summary-content")));
        var section = doc.selectFirst("ul.section");
        System.out.println("Items from " + folder.getName() + ":");
        if(folder.isSnapshotable()) {
            MoodleTextFile summaryTextFile = new MoodleTextFile(doc.html(), "");
            summaryTextFile.setName(folder.getName() + "_snapshot");
            files.add(summaryTextFile);
        }
        files.addAll(getExceptionalContent(doc));
        try{
            for (var resource : section.children()){
                // for each li in the ul.
                folder = getPotentialFiles(resource, folder);
            }
        }catch (NullPointerException npe){
            npe.printStackTrace();
            System.out.println("nullpointer at section: " + folder.getName() );
        }
        catch (RuntimeException e){
            throw e;
        }
        folder.getMoodleFiles().addAll(files);
        return folder;
    }

    private List<MoodleFile> getExceptionalContent(Document doc) {
        // It has been proven that images and videos can be embedded in multiple, if not all, mod-types.
        // No doubt there will be more resources that will fit this criteria,
        // therefore the method name is very generalized.
        var moodleFiles = new ArrayList<MoodleFile>();
        for(var video : doc.select("video")){
            String url = video.selectFirst("source").attr("src");
            moodleFiles.add(getFile(peek(url), url));
        }
        for (var image : doc.select("img[src^=https://moodle.inholland.nl/pluginfile.php]")){
            String url = image.attr("src");
            moodleFiles.add(getFile(peek(url), url));
        }
        return moodleFiles;
    }

    private Document visit(String url){
        Document doc=null;
        try {
            doc = Jsoup.connect(serverUrl + "/content")
                    .requestBody("visit;" + url)
                    .header("moodleServerAuth", serverPassword)
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .post();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return doc;
    }
    private Map<String, String> peek(String url){
        Map<String , String > headers = new HashMap<>();
        url = url.replaceAll(" ", "%20");
        Document doc = null;
        try {
            doc = Jsoup.connect(serverUrl + "/content")
                    .header("moodleServerAuth", serverPassword)
                    .requestBody("peek;" + url)
                    .post();
        } catch (IOException e) {
            new RuntimeException(e.getMessage());
        }
        System.out.println("Headers from: " + url);
        System.out.println(doc.text());
        var split = doc.text().split("\\|");
        for (var kv : split){
            var kvSplit = kv.split("~");
            try{headers.put(kvSplit[0], kvSplit[1]);}catch (ArrayIndexOutOfBoundsException e){headers.put(kvSplit[0], "");}
        }
        return headers;
    }

    private MoodleFolder getPotentialFiles(Element li, MoodleFolder folder){
        var modtype = li.className().split("modtype_")[1];
        System.out.println(modtype);
        switch (modtype){
            case "resource": folder = getPotentialResourceFiles(li, folder);
                break;
            case "page": folder = getPageText(li, folder);
                break;
            case "folder": folder = getPotentialFolderFiles(li, folder);
                break;
                /*
                todo: implement folder and label, maybe book too.
            case "book": folder = getPotentialBookFiles(li, folder);
                break;

            case "forum": folder = getForumLink(li, folder);
                break;
            case "assign": folder = getAssignmentLink(li, folder);
                break;
            case "label": folder = getPotentialLabelFiles(li, folder);
                break;
            case "url":  folder = getUrlContent(li, folder);
                break;
            case "groupselect": folder = getGroupSelectContent(li, folder);
                break;

                 */
            case "choice":
                break;
            case "publication":
                break;
            default:
                System.out.println("mod not yet implemented: " + modtype);
                break;
        }
        return folder;
    }

    private MoodleFolder getPotentialFolderFiles(Element li, MoodleFolder folder) {
        List<MoodleFile> folderFiles = new ArrayList<>();
        String folderName;
        Element folderMain;
        // check if either link or embedded, which affect the folder name and element to take files and snapshot from.
        if(li.selectFirst("a[href^=https://moodle.inholland.nl/mod/folder/view]") == null){
            folderName = li.selectFirst("span.fp-filename").text();
            folderMain = li;
        }else{
            folderMain = visit(li.selectFirst("a").attr("href")).selectFirst("div[role=main]");
            folderName = folderMain.selectFirst("h2").text();
        }
        var snapshotFile = new MoodleTextFile(folderMain.html(), "");
        snapshotFile.setName(folderName.replaceAll("[^a-zA-Z0-9\\.\\-]", "-"));
        folderFiles.add(snapshotFile);
        for (var downloadLink : folderMain.selectFirst("div.filemanager").select("a:not([class])")){
            folderFiles.add(getFile(peek(downloadLink.attr("href")), downloadLink.attr("href")));
        }
        MoodleFolder folderMod = new MoodleFolder(folderName);
        // user folderMod.getName instead of folderName, because the prior is sanitized.
        new File(folder.getPath() + "/" + folderMod.getName()).mkdir();
        folderMod.getMoodleFiles().addAll(folderFiles);
        folderMod.setPath(folder.getPath() + "/" + folderMod.getName());
        folderMod.setSnapshotable(false);
        folder.getMoodleFolders().add(folderMod);
        return folder;
    }

    private MoodleFolder getPageText(Element li, MoodleFolder folder) {
        if(li.selectFirst("a[href^=https://moodle.inholland.nl/mod/page/view]") != null){
            Document doc = visit(li.selectFirst("a").attr("href"));
            MoodleTextFile moodleTextFile = new MoodleTextFile(doc.selectFirst("div[role=main]").html(), "");
            moodleTextFile.setName(li.selectFirst("span.instancename").text());
            folder.getMoodleFiles().add(moodleTextFile);
            folder.getMoodleFiles().addAll(findFilesGenerically(doc.selectFirst("div[role=main]")));
        }
        return folder;
    }

    private MoodleFolder getPotentialResourceFiles(Element li, MoodleFolder folder) {
        String link = li.selectFirst("a").attr("href");
        var peekDoc = peek(link);
        MoodleFile resourceFile = null;
        if(peekDoc.get("Content-Disposition")!=null) // if true, it is downloadable
            resourceFile = getFile(peekDoc, link);
        else {
            Document doc = visit(link + "&forceview=1");
            var workaround = doc.selectFirst("div.resourceworkaround");
            var iframeResource = doc.selectFirst("iframe#resourceobject");
            var objectResource = doc.selectFirst("object#resourceobject");
            if(workaround != null)
                resourceFile = getFile(peek(workaround.selectFirst("a").attr("href")),workaround.selectFirst("a").attr("href"));
            else if(iframeResource != null)
                resourceFile = getFile(peek(iframeResource.attr("src")),iframeResource.attr("src"));
            else if(objectResource != null)
                resourceFile = getFile(peek(objectResource.attr("data")),objectResource.attr("data"));
            else {
                // I discovered resource files with no files but rather images and text
                Element main = doc.selectFirst("div[role=main]");
                folder.getMoodleFiles().addAll(getExceptionalContent(Jsoup.parse(main.html())));
                resourceFile = new MoodleTextFile(main.html(),"");
                resourceFile.setName(main.selectFirst("h2").text());
            }
        }
        if(resourceFile != null){
            folder.getMoodleFiles().add(resourceFile);
        }
        else {
            System.out.println("A null file was given, ignoring...");
        }
        return folder;
    }

    private MoodleFile getFile(Map<String, String> cookies, String link){
        String contentHeader = cookies.get("Content-Disposition");
        String name = contentHeader.substring(contentHeader.indexOf("\"")+1, contentHeader.lastIndexOf("\""));
        MoodleFile file = new MoodleFile(link);
        file.setName(name);
        file.setLastModified(cookies.get("Last-Modified"));
        file.setSize(Integer.parseInt(cookies.get("Content-Length")));
        return file;
    }
    private List<MoodleFile> findFilesGenerically(Element elementToSearch){
        // some courses have plugin files in the weirdest places, this method can be used to find them if suspicion arises.
        List<MoodleFile> files = new ArrayList<>();
        for(var element : elementToSearch.select("a[href^=https://moodle.inholland.nl/pluginfile.php]")){
            System.out.println("Found Generically: " + element.attr("href"));
            //System.out.println(element.outerHtml());
            files.add(getFile(peek(element.attr("href")), element.attr("href")));
        }
        return files;
    }
}
