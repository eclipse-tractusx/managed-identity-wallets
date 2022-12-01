/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets.persistence.repositories

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.*
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class WalletRepository {

    fun getAll(): List<Wallet> = transaction { Wallet.all().toList() }

    @Throws(NotFoundException::class)
    fun getWallet(identifier: String): Wallet {
        return Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }
            .firstOrNull()
            ?: throw NotFoundException("Wallet with given identifier not found")
    }

    @Throws(ConflictException::class)
    fun checkWalletAlreadyExists(identifier: String) {
         if (!Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }.empty()) {
             throw ConflictException("Wallet with given identifier already exists!")
         }
    }

    @Throws(NotFoundException::class, UnprocessableEntityException::class)
    fun getSelfManagedWalletOrThrow(identifier: String): Wallet {
        return transaction {
            val wallet = Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }.firstOrNull()
            if (wallet == null) {
                throw NotFoundException("Wallet with given identifier not found")
            } else if (!wallet.walletId.isNullOrBlank()) {
                throw UnprocessableEntityException("The Wallet with given identifier is not a self managed wallet")
            } else {
                wallet
            }
        }
    }

    fun addWallet(wallet: WalletExtendedData): Wallet {
        // no VCs are added in this step, they will come in through the business partner data service
        return Wallet.new {
            bpn = wallet.bpn
            name = wallet.name
            did = wallet.did
            walletId = wallet.walletId
            walletKey = wallet.walletKey
            walletToken = wallet.walletToken
            createdAt = LocalDateTime.now()
            revocationListName = wallet.revocationListName
            pendingMembershipIssuance = wallet.pendingMembershipIssuance
        }
    }

    fun deleteWallet(identifier: String): Boolean {
        getWallet(identifier).delete()
        return true
    }

    fun updatePending(did: String,isPending: Boolean) {
        getWallet(did).apply {
            pendingMembershipIssuance = isPending
        }
    }

    fun toObject(entity: Wallet): WalletDto = entity.run {
        WalletDto(name, bpn, did, null, createdAt,
            emptyList<VerifiableCredentialDto>().toMutableList(), revocationListName,
            pendingMembershipIssuance)
    }

    fun toWalletCompleteDataObject(entity: Wallet): WalletExtendedData = entity.run {
        WalletExtendedData(id.value, name, bpn, did, walletId, walletKey,
            walletToken, revocationListName, pendingMembershipIssuance)
    }
}
