package edu.ohsu.cmp.coach.model;

import edu.ohsu.cmp.coach.entity.CounselingPage;

public class CounselingPageModel {
    private String key;
    private String title;
    private String body;

    public CounselingPageModel(CounselingPage counselingPage) {
        this.key = counselingPage.getPageKey();
        this.title = counselingPage.getTitle();
        this.body = counselingPage.getBody();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
