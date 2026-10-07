package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
class Book : PanacheEntity {
    lateinit var title: String

    companion object : PanacheCompanion<Book>

    constructor(title: String) {
        this.title = title
    }

    constructor()
}
