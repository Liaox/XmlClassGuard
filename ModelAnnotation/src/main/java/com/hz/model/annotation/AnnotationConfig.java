package com.hz.model.annotation;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

public class AnnotationConfig {
    private List<String> subPackagePaths;
    private String channel;

    public AnnotationConfig(Project project) {
        this.subPackagePaths = new ArrayList<>();
    }

    public List<String> getSubPackagePaths() {
        return subPackagePaths;
    }

    public void setSubPackagePaths(List<String> subPackagePaths) {
        this.subPackagePaths = subPackagePaths;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}