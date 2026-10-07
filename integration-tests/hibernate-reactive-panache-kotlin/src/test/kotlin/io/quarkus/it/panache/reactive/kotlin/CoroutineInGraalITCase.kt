package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.test.junit.QuarkusIntegrationTest

/** Test Kotlin suspend functions annotated with @WithTransaction and @WithSession in native mode */
@QuarkusIntegrationTest class CoroutineInGraalITCase : CoroutineTest()
