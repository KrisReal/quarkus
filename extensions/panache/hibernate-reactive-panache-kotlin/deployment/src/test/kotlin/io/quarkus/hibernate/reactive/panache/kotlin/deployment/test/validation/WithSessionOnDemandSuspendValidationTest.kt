package io.quarkus.hibernate.reactive.panache.kotlin.deployment.test.validation

import io.quarkus.arc.Unremovable
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand
import io.quarkus.hibernate.reactive.panache.kotlin.deployment.test.MyEntity
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WithSessionOnDemandSuspendValidationTest {
    companion object {
        @RegisterExtension
        val config =
            QuarkusExtensionTest()
                .setExpectedException(IllegalStateException::class.java)
                .withApplicationRoot { jar: JavaArchive ->
                    jar.addClasses(MyEntity::class.java, Bean::class.java)
                }
                .withConfigurationResource("application.properties")
    }

    @Test
    fun testValidationFailed() {
        fail<Unit>()
    }

    @Unremovable
    @ApplicationScoped
    open class Bean {

        // Kotlin suspend functions are only supported by @WithSession and @WithTransaction
        @WithSessionOnDemand open suspend fun ping(): String = "pong"
    }
}
