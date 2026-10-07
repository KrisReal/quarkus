package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.hibernate.reactive.panache.common.WithSession;

@WithSession
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class WithSessionInterceptor extends AbstractUniInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Bindings are validated at build time - method-level binding declared on a method that does not return Uni results in a build failure
        // (Kotlin suspend functions are supported if the Hibernate Reactive Panache Kotlin extension is present)
        // However, a class-level binding implies that the other methods that do not return Uni are just a no-op
        if (isUniReturnType(context) || isKotlinSuspendMethod(context)) {
            WithSession withSession = getAnnotation(context);
            String persistenceUnitName = withSession.value();
            if (withSession.stateless()) {
                return fromUni(context, SessionOperations.withStatelessSession(persistenceUnitName, s -> proceedUni(context)));
            } else {
                return fromUni(context, SessionOperations.withSession(persistenceUnitName, s -> proceedUni(context)));
            }
        }
        return context.proceed();
    }

    private WithSession getAnnotation(InvocationContext context) {
        // Check method-level annotation first
        WithSession annotation = context.getMethod().getAnnotation(WithSession.class);
        if (annotation == null) {
            // Check class-level annotation
            annotation = context.getTarget().getClass().getAnnotation(WithSession.class);
        }
        return annotation;
    }

}
