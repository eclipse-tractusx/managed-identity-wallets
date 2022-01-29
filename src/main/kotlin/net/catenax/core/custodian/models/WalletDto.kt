package net.catenax.core.custodian.models

import java.time.LocalDateTime
import net.catenax.core.custodian.plugins.*

import kotlinx.serialization.Serializable

@Serializable
data class WalletDto(val did: String, @Serializable(with = LocalDateTimeAsStringSerializer::class) val createdAt: LocalDateTime, val publicKey: String, @Serializable(with = StringListSerializer::class) val vcs: List<String>) {

    init {
        require(did.isNotBlank()) { "Field 'did' is required not to be blank, but it was blank" }
    }
}