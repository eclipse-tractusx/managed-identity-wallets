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
import io.ktor.features.*
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository

import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import org.eclipse.tractusx.managedidentitywallets.services.*
import org.jetbrains.exposed.sql.transactions.transaction

import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object Services {
    lateinit var businessPartnerDataService: IBusinessPartnerDataService
    lateinit var walletService: IWalletService
    lateinit var utilsService: UtilsService
    lateinit var revocationService: IRevocationService
    lateinit var webhookService: IWebhookService
}

fun Application.module(testing: Boolean = false) {

    val log = LoggerFactory.getLogger(this::class.java)
    if (testing) {
        log.info("Starting in testing mode...")
    }

    configureSockets()
    configureSerialization()

    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
    }
    
    // for debugging
    install(CallLogging)

    // Installs the Kompendium Plugin and sets up baseline server metadata
    configureOpenAPI()

    configureStatusPages()

    configureSecurity()

    val walletRepository = WalletRepository()
    val credRepository = CredentialRepository()
    val connectionRepository = ConnectionRepository()
    val webhookRepository = WebhookRepository()

    val baseWalletBpn = environment.config.property("wallet.baseWalletBpn").getString()
    val acaPyConfig = WalletAndAcaPyConfig(
        apiAdminUrl = environment.config.property("acapy.apiAdminUrl").getString(),
        networkIdentifier = environment.config.property("acapy.networkIdentifier").getString(),
        adminApiKey = environment.config.property("acapy.adminApiKey").getString(),
        baseWalletBpn = baseWalletBpn
    )
    val utilsService = UtilsService(networkIdentifier = acaPyConfig.networkIdentifier)
    val revocationUrl = environment.config.property("revocation.baseUrl").getString()
    val revocationService = IRevocationService.createRevocationService(revocationUrl)
    val webhookService = IWebhookService.createWebhookService(webhookRepository)
    val walletService = IWalletService.createWithAcaPyService(
        acaPyConfig,
        walletRepository,
        credRepository,
        utilsService,
        revocationService,
        webhookService,
        connectionRepository
    )
    val bpdmConfig = BPDMConfig(
        url = environment.config.property("bpdm.datapoolUrl").getString(),
        tokenUrl = environment.config.property("bpdm.authUrl").getString(),
        clientId = environment.config.property("bpdm.clientId").getString(),
        clientSecret = environment.config.property("bpdm.clientSecret").getString(),
        scope = environment.config.property("bpdm.scope").getString(),
        grantType = environment.config.property("bpdm.grantType").getString()
    )
    val businessPartnerDataService = IBusinessPartnerDataService.createBusinessPartnerDataService(walletService,
        bpdmConfig)
    Services.businessPartnerDataService = businessPartnerDataService
    Services.walletService = walletService
    Services.utilsService = utilsService
    Services.revocationService = revocationService
    Services.webhookService = webhookService

    configureRouting(walletService)

    appRoutes(walletService, businessPartnerDataService, revocationService, utilsService)
    configurePersistence()

    configureJobs()

    val wallets = transaction {
        walletService.getAll()
    }

    if (wallets.isNotEmpty() && wallets.stream().anyMatch{ wallet -> wallet.bpn == baseWalletBpn }) {
        walletService.subscribeForAriesWS()
    }
}
