package io.javalin.pathbuilder;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.security.RouteRole;
import org.jetbrains.annotations.NotNull;

public class PathType {

    public static PathBuilder path(@NotNull String path) {
        return PathBuilder.of(path);
    }

    public static PathBuilder path(@NotNull String path, RouteRole... roles) {
        return PathBuilder.of(path, roles);
    }

    private final HandlerType handlerType;
    private Handler handler;
    private RouteRole[] roles = new RouteRole[0];

    private PathType(@NotNull HandlerType handlerType) {
        this.handlerType = handlerType;
    }

    public static PathType get() {
        return new PathType(HandlerType.GET);
    }

    public static PathType post() {
        return new PathType(HandlerType.POST);
    }

    public static PathType put() {
        return new PathType(HandlerType.PUT);
    }

    public static PathType patch() {
        return new PathType(HandlerType.PATCH);
    }

    public static PathType delete() {
        return new PathType(HandlerType.DELETE);
    }

    public static PathType head() {
        return new PathType(HandlerType.HEAD);
    }

    public static PathType query() {
        return new PathType(HandlerType.QUERY);
    }

    public PathType handler(@NotNull Handler handler) {
        this.handler = handler;
        return this;
    }

    public PathType roles(@NotNull RouteRole... roles) {
        this.roles = roles;
        return this;
    }

    void registerOn(@NotNull PathBuilder builder) {
        if (this.handler == null) {
            throw new IllegalStateException("A handler must be specified before registering a route.");
        }
        String path = "";
        builder.addRoute(this.handlerType, path, this.handler, this.roles);
    }
}
