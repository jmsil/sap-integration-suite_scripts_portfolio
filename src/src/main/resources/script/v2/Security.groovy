package src.main.resources.script.v2

import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential

static String getSecureParameter(String alias) {
    UserCredential credential = getUserCredential(alias, 'secure_param', null, 'Secure Parameter')
    return credential.getPassword().toString()
}

static UserCredentials getUserCredentials(String alias) {
    UserCredential credential = getUserCredential(alias, 'default', null, 'User Credentials')
    return new UserCredentials(
        credential.getUsername(),
        credential.getPassword().toString()
    )
}

static OAuth2ClientCredentials getOAuth2ClientCredentials(String alias) {
    UserCredential credential = getUserCredential(
        alias, 'oauth2:default', 'client_credentials', 'OAuth2 Client Credentials')
    Map<String, String> properties = credential.getCredentialProperties()
    return new OAuth2ClientCredentials(
        credential.getUsername(),
        credential.getPassword().toString(),
        properties['sec:server.url'],
        properties['contentType'],
        properties['sendOauth2CcGrantTypeAs'],
        properties['clientAuthentication'],
        properties['sec:scope']
    )
}

static private UserCredential getUserCredential(
    String alias, String credentialKind, String grantType, String credentialType)
{
    SecureStoreService service = ITApiFactory.getService(SecureStoreService.class, null)
    UserCredential credential = service.getUserCredential(alias)
    Map<String, String> properties = credential.getCredentialProperties()

    if (
        properties['sec:credential.kind'] != credentialKind ||
        properties['sec:grant.type'] != grantType
    )
        throw new Exception("Unsupported credential type. Required: ${credentialType}.")

    return credential
}