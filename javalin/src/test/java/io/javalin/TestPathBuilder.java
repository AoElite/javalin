/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.router.Endpoint;
import io.javalin.security.Roles;
import io.javalin.security.RouteRole;
import io.javalin.testing.TestUtil;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static io.javalin.pathbuilder.PathType.get;
import static io.javalin.pathbuilder.PathType.path;
import static io.javalin.pathbuilder.PathType.post;
import static org.assertj.core.api.Assertions.assertThat;

public class TestPathBuilder {

    enum Role implements RouteRole {
        User, Admin, TopAdmin
    }

    @Test
    public void pathBuilderSupportsNestedBuilderPattern() {
        TestUtil.test(Javalin.create(config -> config.routes.pathBuilder(builder -> builder
                .route(post().handler(ctx -> ctx.result("Hello from level 0")))
                .route(get().handler(ctx -> ctx.result("query-0")))
                .path(path("/level-1")
                    .route(get().handler(ctx -> ctx.result("query-1")))
                    .route(post().handler(ctx -> ctx.result("Hello from level 1")))
                    .path(path("/level-2")
                        .route(get().handler(ctx -> ctx.result("query-2")))
                        .route(post().handler(ctx -> ctx.result("Hello from level 2")))
                    )
                )
                .path(
                    path("/admin", Role.Admin)
                        .route(get().handler(ctx -> ctx.result("admin")))
                        .route(post().handler(ctx -> ctx.result("Hello from admin")))
                        .path(path("/test"))
                        .path(
                            path("/top", Role.TopAdmin)
                                .route(get().handler(ctx -> ctx.result("top admin")))
                                .route(post().handler(ctx -> ctx.result("Hello from top admin")))
                        )
                )
                .path(
                    path("/user", Role.User)
                        .route(get().handler(ctx -> ctx.result("user")))
                        .route(post().handler(ctx -> ctx.result("Hello from user")))
                ))),
            (app, http) -> {
                assertThat(http.getBody("/")).isEqualTo("query-0");
                assertThat(Unirest.post(http.origin + "/").asString().getBody()).isEqualTo("Hello from level 0");
                assertThat(http.getBody("/level-1")).isEqualTo("query-1");
                assertThat(Unirest.post(http.origin + "/level-1").asString().getBody()).isEqualTo("Hello from level 1");
                assertThat(http.getBody("/level-1/level-2")).isEqualTo("query-2");
                assertThat(Unirest.post(http.origin + "/level-1/level-2").asString().getBody()).isEqualTo("Hello from level 2");
                assertThat(http.getBody("/admin")).isEqualTo("admin");
                assertThat(http.getBody("/admin/top")).isEqualTo("top admin");
                assertThat(http.getBody("/user")).isEqualTo("user");
            });
    }

    @Test
    public void pathBuilderInheritsRolesFromNestedPaths() {
        Javalin app = Javalin.create(config -> config.routes.pathBuilder(builder -> {
            builder.path(
                path("/admin", Role.Admin)
                    .route(get())
                    .path(
                        path("/top", Role.TopAdmin)
                            .route(get().handler(ctx -> ctx.result("ok")))
                    )
            );
            builder.path(path("/open")
                .route(get().handler(ctx -> {
                })));
        }));

        assertThat(routeRoles(findEndpoint(app, "/admin"))).containsExactly(Role.Admin);
        assertThat(Objects.requireNonNull(findEndpoint(app, "/admin/top").metadata(Roles.class)).getRoles())
            .containsExactlyInAnyOrder(Role.Admin, Role.TopAdmin);
        assertThat(routeRoles(findEndpoint(app, "/open"))).isEqualTo(Set.of());
    }

    private Endpoint findEndpoint(Javalin app, String path) {
        return app.unsafe.internalRouter.allHttpHandlers().stream()
            .map(meta -> meta.endpoint)
            .filter(endpoint -> endpoint.path.equals(path))
            .findFirst()
            .orElseThrow();
    }

    private Set<RouteRole> routeRoles(Endpoint endpoint) {
        Roles roles = endpoint.metadata(Roles.class);
        return roles == null ? Set.of() : roles.getRoles();
    }
}
