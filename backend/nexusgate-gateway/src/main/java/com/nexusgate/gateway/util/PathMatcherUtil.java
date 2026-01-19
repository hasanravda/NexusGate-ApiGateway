package com.nexusgate.gateway.util;

import org.springframework.util.AntPathMatcher;

public class PathMatcherUtil {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static boolean matches(String pattern, String path) {
        return pathMatcher.match(pattern, path);
    }
}