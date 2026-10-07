package io.quarkus.hibernate.reactive.panache.common.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item that indicates that Kotlin {@code suspend} functions are supported by the {@code @WithSession} and
 * {@code @WithTransaction} interceptors.
 * <p>
 * Produced by the Hibernate Reactive Panache Kotlin extension, which registers the
 * {@link io.quarkus.hibernate.reactive.panache.common.runtime.KotlinSuspendMethodHandler}.
 */
public final class KotlinSuspendMethodSupportBuildItem extends SimpleBuildItem {
}
