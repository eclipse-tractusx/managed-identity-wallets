## Interaction with External Wallets <a id= "external-wallets"></a>

- Interaction with external wallet involves:
  - Establish connection with external wallet as defined in [ARIES RFC 0023](https://github.com/hyperledger/aries-rfcs/tree/25464a5c8f8a17b14edaa4310393df6094ace7b0/features/0023-did-exchange)
  - Receive a credential from external wallet as defined in [Aries RFC 0453](https://github.com/hyperledger/aries-rfcs/tree/cd27fc64aa2805f756a118043d7c880354353047/features/0453-issue-credential-v2)
  - Send a presentation to external wallet as defined in [Aries RFC 0454](https://github.com/hyperledger/aries-rfcs/tree/eace815c3e8598d4a8dd7881d8c731fdb2bcc0aa/features/0454-present-proof-v2)

- Current limitation:
  - The managed wallets do not send invitation requests
  - The managed wallets accept all invitation and credentials
  - This is not possible for `ledgerType` equal `closed` because the managed wallets have no `Endorser` role and have no service endpoints in their DID Document


### Establish Connection
All managed wallets in AcaPy has a `webhookUrl` as described in the [documentation](https://github.com/hyperledger/aries-cloudagent-python/blob/main/AdminAPI.md#administration-api-webhooks), which is used to notify the MIW to accept connections, credentials and store them.

The following instruction will be executed to establich connection:
  - The managed wallet must have a service endpoint in its DID Document
  - The external wallet send an invitation request using its public DID and the public DID of the managed wallet
  - The MIW get triggered by the Webhook endpoint and it trigger Acapy back to accept the request
  - The external wallet receive the response from the managed wallet and change its state to `completed`
  - The MIW get triggered again by its Webhook and store the connection with state `completed` using the external DIDs of the wallets


### Receive Verifiable Credential from External Wallet
A Credential-Offer is sent from the external wallet using the established connection with the managed wallet. The Credential-Offer is received by the managed wallet, and this triggers the MIW to send a Credential-Request back to the external Wallet. The external wallet issue the credential and send it to the managed wallet and this triggers the MIW again to store the credential which sends `ack` to the external wallet and change the state of the credential exchange to `DONE` 


### Send Verifiable Presentation to external Wallet
  not implemented yet!

### Local Test Steps:
1. Follow the steps in `Steps for initial local deployment and wallet Creation` section in the `README.md` file
1. Import a new postman collection `Test-Acapy-SelfManagedWallet-Or-ExternalWallet.postman_collection.json` from `./dev-asset`
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Get Connections` and make sure there are no connections. If there are any please delete them using `Remove Connection`
1. From `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Send Connection Request` using the public DID of the managed wallet
1. MIW will accpet the connection, send acknowledgement and store the connection
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Get Connections` and copy the `connection_id` e.g. `716e678c-f329-4baa-be4d-3c68f004a0ef`
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Send Credential` after replacing the `connection_id`, `credentialSubject.id` and `credential.id` in the body 
1. The managed wallet triggers the MIW that accepts the offer, sends the request and then stores the credential when it is issued
1. The verifiable credential will be stored in Database as well as in AcaPy using the credential_id if exists, otherwise the credential_exchange_id