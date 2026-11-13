package edu.ohsu.cmp.coach.security;

import edu.ohsu.cmp.coach.util.FhirUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    private static final String PROPERTY_EXTRA_CONNECT_SRC = "security.csp.extra-connect-src";
    private static final String PROPERTY_EXTRA_FRAME_SRC = "security.csp.extra-frame-src";
    private static final String PROPERTY_EXTRA_FORM_ACTION = "security.csp.extra-form-action";

    private static final String[] CSP_URL_PROPERTY_NAMES = {
            "smart.patient.iss",
            "smart.provider.iss",
            "omron.authorize.url",
            "omron.url"
    };

    private static final String[] ISS_PROPERTY_NAMES = {
            "smart.patient.iss",
            "smart.provider.iss"
    };

    private final String contentSecurityPolicy;

    public SecurityHeadersFilter(Environment env) {
        Set<String> externalOrigins = getExternalOrigins(env);

        Set<String> connectSrc = buildSourceSet(externalOrigins, env.getProperty(PROPERTY_EXTRA_CONNECT_SRC));
        Set<String> frameSrc = buildSourceSet(externalOrigins, env.getProperty(PROPERTY_EXTRA_FRAME_SRC));
        Set<String> formAction = buildSourceSet(externalOrigins, env.getProperty(PROPERTY_EXTRA_FORM_ACTION));

        // add YouTube for resources links
        frameSrc.add("https://www.youtube.com");
        frameSrc.add("https://www.youtube-nocookie.com");

        this.contentSecurityPolicy = String.join("; ",
                "default-src 'self'",
                "script-src 'self' 'unsafe-inline'",
                "style-src 'self' 'unsafe-inline'",
                "img-src 'self' data:",
                "font-src 'self' data:",
                "connect-src " + StringUtils.join(connectSrc, " "),
                "frame-src " + StringUtils.join(frameSrc, " "),
                "object-src 'none'",
                "base-uri 'self'",
                "form-action " + StringUtils.join(formAction, " "),
                "frame-ancestors 'self'"
        );
    }

    private Set<String> buildSourceSet(Set<String> externalOrigins, String extraSources) {
        Set<String> sources = new LinkedHashSet<>();
        sources.add("'self'");
        sources.addAll(externalOrigins);
        sources.addAll(parseExtraSources(extraSources));
        return sources;
    }

    private Set<String> parseExtraSources(String extraSources) {
        Set<String> sources = new LinkedHashSet<>();

        if (StringUtils.isBlank(extraSources)) {
            return sources;
        }

        for (String source : extraSources.trim().split("[,\\s]+")) {
            if (StringUtils.isNotBlank(source)) {
                sources.add(source);
            }
        }

        return sources;
    }

    private Set<String> getExternalOrigins(Environment env) {
        Set<String> origins = new LinkedHashSet<>();

        for (String propertyName : CSP_URL_PROPERTY_NAMES) {
            String value = env.getProperty(propertyName);
            String origin = toOrigin(value);

            if (StringUtils.isNotBlank(origin)) {
                origins.add(origin);
            }
        }

        for (String propertyName : ISS_PROPERTY_NAMES) {
            // may just need the token endpoint, but why not just scour the configs and add
            // everything distinct?
            origins.addAll(FhirUtil.getAuthEndpointOrigins(env.getProperty(propertyName)));
        }

        return origins;
    }

    private String toOrigin(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            URI uri = URI.create(value.trim());

            if (StringUtils.isBlank(uri.getScheme()) || StringUtils.isBlank(uri.getHost())) {
                logger.warn("ignoring URL for Content-Security-Policy because it does not include a scheme and host: " + value);
                return null;
            }

            StringBuilder origin = new StringBuilder()
                    .append(uri.getScheme())
                    .append("://")
                    .append(uri.getHost());

            if (uri.getPort() >= 0) {
                origin.append(":").append(uri.getPort());
            }

            return origin.toString();

        } catch (IllegalArgumentException e) {
            logger.warn("ignoring invalid URL for Content-Security-Policy: " + value);
            logger.debug("caught " + e.getClass().getName() + " parsing CSP URL value - " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}