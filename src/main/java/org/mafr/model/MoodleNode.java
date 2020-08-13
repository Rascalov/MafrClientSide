package org.mafr.model;

import javafx.scene.Node;

public class MoodleNode {
    private String id;
    private Node node;

    public MoodleNode(String id, Node node) {
        this.id = id;
        this.node = node;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
