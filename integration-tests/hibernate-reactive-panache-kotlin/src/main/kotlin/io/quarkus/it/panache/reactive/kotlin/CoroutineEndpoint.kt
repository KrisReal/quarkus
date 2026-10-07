package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.arc.Arc
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.core.Vertx
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail

/** Kotlin suspend functions annotated with @WithTransaction and @WithSession */
@Path("coroutines")
class CoroutineEndpoint {

    @Inject lateinit var transactionService: CoroutineTransactionService

    @Inject lateinit var sessionService: CoroutineSessionService

    @GET
    @Path("transaction/{title}")
    suspend fun transaction(@PathParam("title") title: String): String {
        val vertxContext = Vertx.currentContext()
        val book = transactionService.persist(title)
        // The calling coroutine is resumed on the Vert.x context of the current request
        assertSame(vertxContext, Vertx.currentContext())
        assertTrue(Arc.container().requestContext().isActive)
        assertNotNull(book.id)
        assertEquals(1L, sessionService.countByTitle(title))
        assertEquals(book.id, sessionService.findByTitle(title)?.id)
        return "OK"
    }

    @GET
    @Path("rollback/{title}")
    suspend fun rollback(@PathParam("title") title: String): String {
        try {
            transactionService.persistAndFail(title)
            fail<Unit>("An exception should have been thrown")
        } catch (e: IllegalStateException) {
            assertEquals("Rollback for $title", e.message)
        }
        assertEquals(0L, sessionService.countByTitle(title))
        assertNull(sessionService.findByTitle(title))
        return "OK"
    }

    @GET
    @Path("nested/{title}")
    suspend fun nested(@PathParam("title") title: String): String {
        assertEquals(1L, transactionService.persistAndCountInSameTransaction(title))
        assertEquals("echo", transactionService.echo("echo"))
        return "OK"
    }

    @GET
    @Path("unit/{title}")
    suspend fun unit(@PathParam("title") title: String): String {
        transactionService.persistUnit(title)
        // A method returning Uni declared on a class with a class-level binding
        assertEquals(1L, sessionService.countByTitleUni(title).awaitSuspending())
        return "OK"
    }
}
