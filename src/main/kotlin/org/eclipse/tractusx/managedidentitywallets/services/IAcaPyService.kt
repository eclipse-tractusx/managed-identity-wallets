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

package org.eclipse.tractusx.managedidentitywallets.services

import io.ktor.client.*
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord

interface IAcaPyService {

    fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig

    suspend fun getWallets(): WalletList

    suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult

    suspend fun assignDidToPublic(didIdentifier: String, token: String)

    suspend fun deleteSubWallet(walletData: WalletExtendedData)

    suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse

    suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult

    suspend fun registerDidOnLedger(didRegistration: DidRegistration, endorserWalletToken: String): DidRegistrationResult

    suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String

    suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse

    suspend fun resolveDidDoc(did: String, token: String): ResolutionResult

    suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String)

    fun subscribeForWebSocket(subscriberWallet: WalletExtendedData)

    suspend fun getAcapyClient(walletToken: String): AriesClient

    suspend fun connect(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto,
        token: String
    ): ConnectionRecord

    suspend fun issuanceFlowCredentialSend(
        token: String,
        vc: VerifiableCredentialIssuanceFlowRequest
    ): V20CredExRecord

    suspend fun deleteConnection(connectionId: String, token: String)

    companion object {
        fun create(
            walletAndAcaPyConfig: WalletAndAcaPyConfig,
            utilsService: UtilsService,
            client: HttpClient
        ): IAcaPyService {
            return AcaPyService(walletAndAcaPyConfig, utilsService, client)
        }
    }
}
