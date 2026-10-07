package io.quarkus.hibernate.reactive.panache.kotlin.runtime

import io.quarkus.hibernate.reactive.panache.common.runtime.KotlinSuspendMethodHandler
import io.smallrye.mutiny.Uni
import jakarta.interceptor.InvocationContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Allows [io.quarkus.hibernate.reactive.panache.common.WithSession] and
 * [io.quarkus.hibernate.reactive.panache.common.WithTransaction] on Kotlin `suspend` functions, so
 * that the reactive session or transaction spans the whole execution of the `suspend` function.
 */
class PanacheKotlinSuspendMethodHandler : KotlinSuspendMethodHandler {

    override fun proceedUni(invocation: InvocationContext): Uni<Any?> {
        return Uni.createFrom().emitter { emitter ->
            val parameters = invocation.parameters.clone()
            @Suppress("UNCHECKED_CAST")
            val callerContinuation = parameters[parameters.size - 1] as Continuation<Any?>
            // The completion of the suspend function is emitted instead of resuming the calling
            // coroutine directly
            parameters[parameters.size - 1] =
                object : Continuation<Any?> {
                    override val context: CoroutineContext
                        get() = callerContinuation.context

                    override fun resumeWith(result: Result<Any?>) {
                        result.fold({ emitter.complete(it) }, { emitter.fail(it) })
                    }
                }
            invocation.parameters = parameters
            try {
                val result = invocation.proceed()
                if (result !== COROUTINE_SUSPENDED) {
                    emitter.complete(result)
                }
            } catch (e: Throwable) {
                emitter.fail(e)
            }
        }
    }

    override fun awaitUni(invocation: InvocationContext, uni: Uni<Any?>): Any? {
        val parameters = invocation.parameters
        @Suppress("UNCHECKED_CAST")
        val callerContinuation = parameters[parameters.size - 1] as Continuation<Any?>
        // Either null, the Result of the Uni, or COROUTINE_SUSPENDED once this method returned
        val state = AtomicReference<Any?>()
        uni.subscribe()
            .with(
                { item ->
                    if (!state.compareAndSet(null, Result.success(item))) {
                        // The calling coroutine is resumed through its dispatcher, e.g. on the
                        // Vert.x context of the current request
                        callerContinuation.intercepted().resume(item)
                    }
                },
                { failure ->
                    if (!state.compareAndSet(null, Result.failure<Any?>(failure))) {
                        callerContinuation.intercepted().resumeWithException(failure)
                    }
                },
            )
        if (state.compareAndSet(null, COROUTINE_SUSPENDED)) {
            return COROUTINE_SUSPENDED
        }
        // The Uni completed before this method returned - the calling coroutine must not be resumed
        // in the same stack frame, so the result is returned directly instead
        @Suppress("UNCHECKED_CAST")
        return (state.get() as Result<Any?>).getOrThrow()
    }
}
