package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.`when`
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

/** Kotlin suspend functions annotated with @WithTransaction and @WithSession */
@QuarkusTest
open class CoroutineTest {

    @Test
    fun testTransaction() {
        `when`()["/coroutines/transaction/transaction"].then().statusCode(200).body(`is`("OK"))
    }

    @Test
    fun testRollback() {
        `when`()["/coroutines/rollback/rollback"].then().statusCode(200).body(`is`("OK"))
    }

    @Test
    fun testNested() {
        `when`()["/coroutines/nested/nested"].then().statusCode(200).body(`is`("OK"))
    }

    @Test
    fun testUnit() {
        `when`()["/coroutines/unit/unit"].then().statusCode(200).body(`is`("OK"))
    }
}
