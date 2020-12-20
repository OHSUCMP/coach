package edu.ohsu.cmp.htnu18app.layout;

import com.samskivert.mustache.Mustache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// source: https://spring.io/blog/2016/11/21/the-joy-of-mustache-server-side-templates-for-the-jvm
@ControllerAdvice
public class LayoutAdvice {
    private final Mustache.Compiler compiler;

    @Autowired
    public LayoutAdvice(Mustache.Compiler compiler) {
        this.compiler = compiler;
    }

    @ModelAttribute("layout")
    public Mustache.Lambda layout() {
        return new Layout(compiler);
    }

    @ModelAttribute("title")
    public Mustache.Lambda title(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            String title = frag.execute();
            layout.setTitle(title);
        };
    }

    @ModelAttribute("headers")
    public Mustache.Lambda headers(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            String headers = frag.execute();
            layout.setHeaders(headers);
        };
    }

    @ModelAttribute("menuItems")
    public Mustache.Lambda menuItems(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            String menuItems = frag.execute();
            layout.setMenuItems(menuItems);
        };
    }

    @ModelAttribute("content")
    public Mustache.Lambda content(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            String content = frag.execute();
            layout.setContent(content);
        };
    }
}
