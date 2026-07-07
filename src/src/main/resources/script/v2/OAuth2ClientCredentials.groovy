package src.main.resources.script.v2

import groovy.transform.Immutable

@Immutable
class OAuth2ClientCredentials {
    final String clientId
    final String clientSecret
    final String serviceUrl
    final String contentType
    final String sendGrantTypeAs
    final String sendAuthenticationAs
    final String scope
}