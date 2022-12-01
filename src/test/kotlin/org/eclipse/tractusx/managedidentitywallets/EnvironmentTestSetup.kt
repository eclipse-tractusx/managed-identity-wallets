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

package org.eclipse.tractusx.managedidentitywallets

import io.ktor.application.*
import io.ktor.config.*
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository

import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository
import org.eclipse.tractusx.managedidentitywallets.routes.AuthorizationHandler
import org.eclipse.tractusx.managedidentitywallets.services.AcaPyWalletServiceImpl
import org.eclipse.tractusx.managedidentitywallets.services.IWebhookService
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService
import java.sql.DriverManager
import java.util.*

object EnvironmentTestSetup {

    const val DEFAULT_BPN = "BPNL00000"
    const val EXTRA_TEST_BPN = "BPNL0Test"
    const val NETWORK_ID = "local:test"
    const val NONE_REVOKED_ENCODED_LIST = "H4sIAAAAAAAAAO3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
    const val ZERO_THIRD_REVOKED_ENCODED_LIST ="H4sIAAAAAAAAAO3BIQEAAAACIKv/DzvDAjQAAAAAAAAAAAAAAAAAAADA2wBHo2oBAEAAAA=="
    private val walletRepository = WalletRepository()
    private val credentialRepository = CredentialRepository()
    val connectionRepository = ConnectionRepository()
    val webhookRepository = WebhookRepository()

    private val acaPyMockedService = AcaPyMockedService(DEFAULT_BPN, NETWORK_ID)
    val revocationMockedService = RevocationMockedService(NETWORK_ID)
    val webhookService = IWebhookService.createWebhookService(webhookRepository)
    val utilsService = UtilsService(NETWORK_ID)

    val walletService = AcaPyWalletServiceImpl(
        acaPyMockedService, walletRepository,
        credentialRepository, utilsService, revocationMockedService,
        webhookService, connectionRepository
    )
    val bpdService = BusinessPartnerDataMockedService()

    val EMPTY_ROLES_TOKEN = JwtConfigTest.makeToken(listOf())
    val CREATE_TOKEN = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_CREATE_WALLETS))
    val VIEW_TOKEN = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_VIEW_WALLETS))
    val UPDATE_TOKEN = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_UPDATE_WALLETS))
    val DELETE_TOKEN = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_DELETE_WALLETS))
    val VIEW_TOKEN_SINGLE = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_VIEW_WALLET), DEFAULT_BPN)
    val VIEW_TOKEN_SINGLE_WITHOUT_BPN = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_VIEW_WALLET))
    val VIEW_TOKEN_SINGLE_EXTRA_BPN =
        JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_VIEW_WALLET), EXTRA_TEST_BPN)
    val UPDATE_TOKEN_SINGLE = JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_UPDATE_WALLET), DEFAULT_BPN)
    val UPDATE_TOKEN_SINGLE_EXTRA_BPN =
        JwtConfigTest.makeToken(listOf(AuthorizationHandler.ROLE_UPDATE_WALLET), EXTRA_TEST_BPN)
    val UPDATE_TOKEN_ALL_AND_SINGLE_EXTRA_BPN = JwtConfigTest.makeToken(
        listOf(
            AuthorizationHandler.ROLE_UPDATE_WALLET,
            AuthorizationHandler.ROLE_UPDATE_WALLETS
        ), EXTRA_TEST_BPN
    )


    fun setupEnvironment(environment: ApplicationEnvironment) {
        val jdbcUrl = System.getenv("CX_DB_JDBC_URL") ?: "jdbc:sqlite:file:test?mode=memory&cache=shared"
        (environment.config as MapApplicationConfig).apply {
            put("app.version", System.getenv("APP_VERSION") ?: "0.0.7")
            put("db.jdbcUrl", jdbcUrl)
            put("db.jdbcDriver", System.getenv("CX_DB_JDBC_DRIVER") ?: "org.sqlite.JDBC")
            put("acapy.apiAdminUrl", System.getenv("ACAPY_API_ADMIN_URL") ?: "http://localhost:11000")
            put("acapy.networkIdentifier", System.getenv("ACAPY_NETWORK_IDENTIFIER") ?: "local:test")
            put("acapy.adminApiKey", System.getenv("ACAPY_ADMIN_API_KEY") ?: "Hj23iQUsstG!dde")
            put("wallet.baseWalletBpn", System.getenv("CX_BPN") ?: DEFAULT_BPN)

            put("auth.jwksUrl", System.getenv("CX_AUTH_JWKS_URL") ?: "http://localhost:18080/jwks")
            put("auth.issuerUrl", System.getenv("CX_AUTH_ISSUER_URL") ?: JwtConfigTest.issuerUrl)
            put("auth.realm", System.getenv("CX_AUTH_REALM") ?: "catenax")
            put("auth.roleMappings",
                System.getenv("CX_AUTH_ROLE_MAPPINGS")
                    ?: "create_wallets:create_wallets,view_wallets:view_wallets,update_wallets:update_wallets,delete_wallets:delete_wallets,view_wallet:view_wallet,update_wallet:update_wallet"
            )
            put("auth.resourceId", System.getenv("CX_AUTH_RESOURCE_ID") ?: JwtConfigTest.resourceId)

            // unused yet, just for completeness
            put("auth.clientId", System.getenv("CX_AUTH_CLIENT_ID") ?: "clientId")
            put("auth.clientSecret", System.getenv("CX_AUTH_CLIENT_SECRET") ?: "clientSecret")
            put("auth.redirectUrl", System.getenv("CX_AUTH_REDIRECT_URL") ?: "http://localhost:8080/callback")

            put("bpdm.pullDataAtHour", System.getenv("BPDM_PULL_DATA_AT_HOUR") ?: "23")
            put("bpdm.datapoolUrl", System.getenv("BPDM_DATAPOOL_URL") ?: "http://0.0.0.0:8080")

            put("revocation.baseUrl", System.getenv("REVOCATION_URL") ?: "http://0.0.0.0:8086")
            put("revocation.createStatusListCredentialAtHour", System.getenv("REVOCATION_CREATE_STATUS_LIST_CREDENTIAL_AT_HOUR") ?: "3")
        }
        // just a keepAliveConnection
        DriverManager.getConnection(jdbcUrl)

        SingletonTestData.baseWalletDID = ""
        SingletonTestData.baseWalletVerKey = ""
        SingletonTestData.signCredentialResponse = ""
        SingletonTestData.isValidVerifiablePresentation = true
        SingletonTestData.isValidVerifiableCredential = true
        SingletonTestData.credentialIndex = 1
        SingletonTestData.revocationListName = UUID.randomUUID().toString()
        SingletonTestData.encodedList = NONE_REVOKED_ENCODED_LIST
    }

    fun setupEnvironmentWithMissingRoleMapping(environment: ApplicationEnvironment) {
        setupEnvironment(environment)
        (environment.config as MapApplicationConfig).apply {
            put(
                "auth.roleMappings", value = System.getenv("CX_AUTH_ROLE_MAPPINGS")
                    ?: ("no_create_wallets:create_wallets,no_view_wallets:view_wallets," +
                            "no_update_wallets:update_wallets,no_delete_wallets:delete_wallets," +
                            "view_wallet:view_wallet,update_wallet:update_wallet")
            )
        }
    }

    fun setupEnvironmentRoleMapping(environment: ApplicationEnvironment, roleMapping: String) {
        setupEnvironment(environment)
        (environment.config as MapApplicationConfig).apply {
            put(
                "auth.roleMappings", value = System.getenv("CX_AUTH_ROLE_MAPPINGS") ?: roleMapping
            )
        }
    }

}

