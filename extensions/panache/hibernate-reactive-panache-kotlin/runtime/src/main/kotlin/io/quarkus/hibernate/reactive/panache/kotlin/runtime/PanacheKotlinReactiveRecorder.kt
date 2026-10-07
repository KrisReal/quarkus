package io.quarkus.hibernate.reactive.panache.kotlin.runtime

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations
import io.quarkus.hibernate.reactive.panache.common.runtime.KotlinSuspendMethodHandler
import io.quarkus.runtime.annotations.Recorder

@Recorder
open class PanacheKotlinReactiveRecorder {
    open fun addEntityTypesToPersistenceUnit(
        entityToPersistenceUnit: Map<String?, String?>?,
        incomplete: Boolean,
    ) {
        AbstractJpaOperations.addEntityTypesToPersistenceUnit(entityToPersistenceUnit)
    }

    open fun createKotlinSuspendMethodHandler(): KotlinSuspendMethodHandler {
        return PanacheKotlinSuspendMethodHandler()
    }
}
