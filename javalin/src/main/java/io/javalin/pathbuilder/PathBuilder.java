/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.pathbuilder;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.router.JavalinDefaultRoutingApi;
import io.javalin.security.RouteRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PathBuilder {

    private JavalinDefaultRoutingApi routingApi;
    private String pathPrefix;
    private List<RouteRole> inheritedRoles;
    private final List<PathType> routes = new ArrayList<>();
    private final List<PathBuilder> children = new ArrayList<>();

    protected PathBuilder(
        @Nullable JavalinDefaultRoutingApi routingApi,
        @NotNull String pathPrefix,
        @NotNull List<RouteRole> inheritedRoles
    ) {
        this.routingApi = routingApi;
        this.pathPrefix = pathPrefix;
        this.inheritedRoles = inheritedRoles;
    }

    public static PathBuilder root(@NotNull JavalinDefaultRoutingApi routingApi) {
        return new PathBuilder(routingApi, "", List.of());
    }

    static PathBuilder of(@NotNull String path) {
        return new PathBuilder(null, path, List.of());
    }

    static PathBuilder of(@NotNull String path, RouteRole... roles) {
        return new PathBuilder(null, path, List.of(roles));
    }

    public PathBuilder path(@NotNull PathBuilder pathBuilder) {
        this.children.add(pathBuilder);
        if (this.routingApi != null) {
            pathBuilder.routingApi = requireRoutingApi();
            pathBuilder.pathPrefix = normalizePath(this.pathPrefix, pathBuilder.pathPrefix);
            pathBuilder.inheritedRoles = mergeRoles(pathBuilder.inheritedRoles.toArray(RouteRole[]::new));
            pathBuilder.registerPending();
        }
        return this;
    }

    public PathBuilder route(@NotNull PathType routerType) {
        this.routes.add(routerType);
        if (this.routingApi != null) {
            routerType.registerOn(this);
        }
        return this;
    }

    void addRoute(@NotNull HandlerType handlerType, @NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        RouteRole[] mergedRoles = mergeRoles(roles).toArray(RouteRole[]::new);
        requireRoutingApi().addHttpHandler(handlerType, normalizePath(this.pathPrefix, path), handler, mergedRoles);
    }

    private JavalinDefaultRoutingApi requireRoutingApi() {
        if (this.routingApi == null) {
            throw new IllegalStateException("PathBuilder.of(...) instances can only be used as path arguments.");
        }
        return this.routingApi;
    }

    private void registerPending() {
        this.routes.forEach(route -> route.registerOn(this));
        this.children.forEach(child -> {
            child.routingApi = requireRoutingApi();
            child.pathPrefix = normalizePath(this.pathPrefix, child.pathPrefix);
            child.inheritedRoles = mergeRoles(child.inheritedRoles.toArray(RouteRole[]::new));
            child.registerPending();
        });
    }

    private List<RouteRole> mergeRoles(@NotNull RouteRole... roles) {
        Set<RouteRole> mergedRoles = new LinkedHashSet<>(this.inheritedRoles);
        mergedRoles.addAll(Arrays.asList(roles));
        return new ArrayList<>(mergedRoles);
    }

    static String normalizePath(@NotNull String prefix, @NotNull String path) {
        if (path.equals("*")) {
            return prefix.isEmpty() ? "*" : prefix + "*";
        }
        String normalizedPath = path.isEmpty() ? "" : path.startsWith("/") ? path : "/" + path;
        return prefix + normalizedPath;
    }

}
