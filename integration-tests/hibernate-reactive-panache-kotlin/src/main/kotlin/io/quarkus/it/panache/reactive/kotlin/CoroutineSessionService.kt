package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import org.hibernate.reactive.mutiny.Mutiny

// Class-level binding - all Kotlin suspend functions and all methods returning Uni are intercepted
@WithSession
@ApplicationScoped
open class CoroutineSessionService {

    open suspend fun countByTitle(title: String): Long {
        return Book.count("title", title).awaitSuspending()
    }

    open suspend fun findByTitle(title: String): Book? {
        return Book.find("title", title).firstResult().awaitSuspending()
    }

    open suspend fun currentTransaction(): Mutiny.Transaction? {
        return Panache.currentTransaction().awaitSuspending()
    }

    open suspend fun echo(value: String): String {
        return value
    }

    open fun countByTitleUni(title: String): Uni<Long> {
        return Book.count("title", title)
    }
}
