package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.interceptor.InvocationContext;

import io.smallrye.mutiny.Uni;

/**
 * Bridges intercepted Kotlin {@code suspend} functions and the {@link Uni}-based interceptors in this module.
 * <p>
 * An implementation is registered through {@link PanacheHibernateRecorder} by the Hibernate Reactive Panache Kotlin
 * extension, so that this module stays free of a Kotlin dependency.
 */
public interface KotlinSuspendMethodHandler {

    /**
     * @return a lazy {@link Uni} that invokes the intercepted {@code suspend} function when subscribed and emits its result
     */
    Uni<Object> proceedUni(InvocationContext context);

    /**
     * Subscribes to the given {@link Uni} and resumes the calling coroutine with its outcome.
     *
     * @return the {@code COROUTINE_SUSPENDED} marker, or the result if the {@link Uni} completed immediately
     */
    Object awaitUni(InvocationContext context, Uni<Object> uni);

}
