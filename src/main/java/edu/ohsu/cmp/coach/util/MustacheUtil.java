package edu.ohsu.cmp.coach.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import edu.ohsu.cmp.coach.model.Audience;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MustacheUtil {
    public static String compileMustache(Audience audience, String s) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(audience.getTag(), true);
        return compileMustache(s, map);
    }

    public static String compileMustache(String s, Map<String, Object> map) throws IOException {
        if (s == null) return null;
        if (s.trim().isEmpty()) return "";

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile(new StringReader(s), "template" );
        StringWriter writer = new StringWriter();

        m.execute(writer, map).flush();

        return writer.toString();
    }
}
