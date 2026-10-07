package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame

@ApplicationScoped
open class CoroutineTransactionService {

    @Inject lateinit var sessionService: CoroutineSessionService

    @WithTransaction
    open suspend fun persist(title: String): Book {
        val transaction = Panache.currentTransaction().awaitSuspending()
        assertNotNull(transaction)
        // Suspend outside of Hibernate Reactive - the transaction must still be available
        // afterwards
        delay(10)
        val book = Book(title).persist<Book>().awaitSuspending()
        assertSame(transaction, Panache.currentTransaction().awaitSuspending())
        return book
    }

    @WithTransaction
    open suspend fun persistAndFail(title: String): Book {
        Book(title).persistAndFlush<Book>().awaitSuspending()
        throw IllegalStateException("Rollback for $title")
    }

    @WithTransaction
    open suspend fun persistAndCountInSameTransaction(title: String): Long {
        val transaction = Panache.currentTransaction().awaitSuspending()
        Book(title).persistAndFlush<Book>().awaitSuspending()
        // A nested @WithSession suspend function reuses the session of the current transaction
        val nestedTransaction: Mutiny.Transaction? = sessionService.currentTransaction()
        assertSame(transaction, nestedTransaction)
        return sessionService.countByTitle(title)
    }

    @WithTransaction
    open suspend fun echo(value: String): String {
        // A nested @WithSession suspend function that completes without suspending
        return sessionService.echo(value)
    }

    @WithTransaction
    open suspend fun persistUnit(title: String) {
        Book(title).persist<Book>().awaitSuspending()
    }
}
