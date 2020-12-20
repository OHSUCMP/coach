package edu.ohsu.cmp.htnu18app.layout;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.IOException;
import java.io.Writer;

// source: https://spring.io/blog/2016/11/21/the-joy-of-mustache-server-side-templates-for-the-jvm
public class Layout implements Mustache.Lambda {

    private Mustache.Compiler compiler;

    private String title;
    private String headers;
    private String menuItems;
    private String content;

    public Layout(Mustache.Compiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void execute(Template.Fragment frag, Writer out) throws IOException {
        frag.execute();
        compiler.compile("{{>layout}}").execute(frag.context(), out);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(String menuItems) {
        this.menuItems = menuItems;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
