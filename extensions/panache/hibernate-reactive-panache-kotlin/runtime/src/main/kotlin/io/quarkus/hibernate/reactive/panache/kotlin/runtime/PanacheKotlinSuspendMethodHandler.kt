package io.quarkus.hibernate.reactive.panache.kotlin.runtime

import io.quarkus.hibernate.reactive.panache.common.runtime.KotlinSuspendMethodHandler
import io.smallrye.mutiny.Uni
import jakarta.interceptor.InvocationContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
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
            val parameters = invocation.parameters
            @Suppress("UNCHECKED_CAST")
            val callerContinuation = parameters[parameters.size - 1] as Continuation<Any?>
            // The completion of the suspend function is emitted instead of resuming the calling
            // coroutine directly
            val completion =
                Continuation<Any?>(callerContinuation.context) { result ->
                    result.fold({ emitter.complete(it) }, { emitter.fail(it) })
                }
            val coroutine: suspend () -> Any? = { proceedSuspending(invocation) }
            try {
                val result = coroutine.startCoroutineUninterceptedOrReturn(completion)
                if (result !== COROUTINE_SUSPENDED) {
                    emitter.complete(result)
                }
            } catch (e: Throwable) {
                emitter.fail(e)
            }
        }
    }

    /**
     * Invokes the intercepted `suspend` function with the [Continuation] of the coroutine started
     * in [proceedUni]. A `suspend` function that tail-calls another `suspend` function passes this
     * [Continuation] on as is, and unlike a plain [Continuation], it is then still resumed through
     * the dispatcher of the calling coroutine, i.e. on the Vert.x context of the reactive session.
     */
    private suspend fun proceedSuspending(invocation: InvocationContext): Any? =
        suspendCoroutineUninterceptedOrReturn { continuation ->
            val parameters = invocation.parameters.clone()
            parameters[parameters.size - 1] = continuation
            invocation.parameters = parameters
            invocation.proceed()
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
