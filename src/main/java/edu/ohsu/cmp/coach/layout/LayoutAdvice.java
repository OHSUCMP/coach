package edu.ohsu.cmp.coach.layout;

import com.samskivert.mustache.Mustache;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// source: https://spring.io/blog/2016/11/21/the-joy-of-mustache-server-side-templates-for-the-jvm
@ControllerAdvice
public class LayoutAdvice {
    private final Mustache.Compiler compiler;

    @Value("${app.name}")
	private String appName;

	@Value("${app.version}")
	private String appVersion;

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
            layout.setTitle(frag.execute());
        };
    }

    @ModelAttribute("head")
    public Mustache.Lambda head(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            layout.setHead(frag.execute());
        };
    }

    @ModelAttribute("menuItems")
    public Mustache.Lambda menuItems(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            layout.setMenuItems(frag.execute());
        };
    }

    @ModelAttribute("content")
    public Mustache.Lambda content(@ModelAttribute Layout layout) {
        return (frag, out) -> {
            layout.setContent(frag.execute());
        };
    }

    @ModelAttribute
	public void addDefaultAttributes(HttpServletRequest req, Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("currentYear", Calendar.getInstance().get(Calendar.YEAR));
	}

}
