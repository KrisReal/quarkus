package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.interceptor.InvocationContext;

import io.smallrye.mutiny.Uni;

abstract class AbstractUniInterceptor {

    // Set by PanacheHibernateRecorder if the Hibernate Reactive Panache Kotlin extension is present
    static volatile KotlinSuspendMethodHandler kotlinSuspendMethodHandler;

    @SuppressWarnings("unchecked")
    protected <T> Uni<T> proceedUni(InvocationContext context) {
        try {
            if (isKotlinSuspendMethod(context)) {
                return (Uni<T>) kotlinSuspendMethodHandler.proceedUni(context);
            }
            return ((Uni<T>) context.proceed());
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
    }

    protected boolean isUniReturnType(InvocationContext context) {
        return context.getMethod().getReturnType().equals(Uni.class);
    }

    /**
     * Returns {@code true} if the intercepted method is a Kotlin {@code suspend} function, i.e. its last parameter is a
     * {@code kotlin.coroutines.Continuation}, and Kotlin {@code suspend} functions are supported.
     */
    protected boolean isKotlinSuspendMethod(InvocationContext context) {
        if (kotlinSuspendMethodHandler == null) {
            return false;
        }
        Class<?>[] parameterTypes = context.getMethod().getParameterTypes();
        return parameterTypes.length > 0
                && parameterTypes[parameterTypes.length - 1].getName().equals("kotlin.coroutines.Continuation");
    }

    /**
     * Returns the given {@link Uni} as the result of the intercepted method; a Kotlin {@code suspend} function is suspended
     * until the {@link Uni} completes.
     */
    @SuppressWarnings("unchecked")
    protected Object fromUni(InvocationContext context, Uni<?> uni) {
        if (isKotlinSuspendMethod(context)) {
            return kotlinSuspendMethodHandler.awaitUni(context, (Uni<Object>) uni);
        }
        return uni;
    }

}
