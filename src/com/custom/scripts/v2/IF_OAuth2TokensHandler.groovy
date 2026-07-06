package com.custom.scripts.v2

import com.custom.scripts.util.v2.HttpUtil
import com.custom.scripts.util.v2.JsonUtil
import com.custom.scripts.util.v2.SecurityUtil
import com.sap.it.script.v2.api.Message
import groovy.transform.Field

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime

import static com.custom.scripts.util.v2.MessageUtil.MessageHeader
import static com.custom.scripts.util.v2.SecurityUtil.OAuth2ClientCredentials

@Field
static final MessageHeader firebaseProjectId = new MessageHeader('_firebase-project-id')

static Message setCachedAuthorization(Message message) {
    String cache = message.getBody(String)

    if (!cache)
        return message

    Map<String, Object> token = JsonUtil.jsonToMap(cache)
    int expiresInSeconds = token['expires_in'] as Integer
    expiresInSeconds -= Math.min(expiresInSeconds.intdiv(10) as Integer, 120)
    LocalDateTime createdIn = LocalDateTime.parse(token['created_in'] as String)
    LocalDateTime expiresIn = createdIn.plusSeconds(expiresInSeconds)

    if (LocalDateTime.now() <= expiresIn)
        HttpUtil.setAuthorizationHeader(message, token)

    firebaseProjectId.set(message, token['project_id'])
    return message
}

static Message setNewAuthorization(Message message) {
    Map<String, Object> token = JsonUtil.jsonToMap(message.getBody(String))
    String projectId = firebaseProjectId.get(message)

    if (projectId)
        token['project_id'] = projectId

    token['created_in'] = LocalDateTime.now().toString()
    message.setBody(JsonUtil.mapToJson(token))
    HttpUtil.setAuthorizationHeader(message, token)

    return message
}

static Message setRequest(Message message) {
    switch (message.getProperty('_route')) {
        case 'cc-mtls' -> setRequestFromClientCredentials(message)
        case 'fb-skey' -> setRequestFromFirebaseServiceKey(message)
    }
    return message
}

static private void setRequestFromClientCredentials(Message message) {
    OAuth2ClientCredentials credentials = SecurityUtil.getOAuth2ClientCredentials(
        getArtifactAlias(message))
    setConnectionAddress(message, credentials.serviceUrl)
    Map<String, String> requestBody = ['scope': credentials.scope]

    switch (credentials.sendAuthenticationAs) {
        case 'header' -> {
            String authentication = credentials.clientId + ':' + credentials.clientSecret
            authentication = encodeBase64(authentication)
            HttpUtil.setAuthorizationHeader(message, 'Basic', authentication)
        }
        case 'body' -> {
            requestBody['client_id'] = credentials.clientId
            requestBody['client_secret'] = credentials.clientSecret
        }
    }

    switch (credentials.sendGrantTypeAs) {
        case 'partOfBody'-> requestBody['grant_type'] = 'client_credentials'
        case 'partOfURL' -> setConnectionQuery(message, 'grant_type=client_credentials')
    }

    switch (credentials.contentType) {
        case 'json' -> HttpUtil.setJsonRequest(message, requestBody)
        case 'urlencoded' -> HttpUtil.setFormUrlEncodedRequest(message, requestBody)
    }
}

static private void setRequestFromFirebaseServiceKey(Message message) {
    long nowSeconds = System.currentTimeMillis().intdiv(1000)
    String secureParameter = SecurityUtil.getSecureParameter(getArtifactAlias(message))
    Map<String, String> key = JsonUtil.jsonToMap(secureParameter) as Map<String, String>

    Map<String, String> header = [
        'alg': 'RS256',
        'typ': 'JWT',
        'kid': key['private_key_id']
    ]

    Map<String, Object> payload = [
        'iss': key['client_email'],
        'scope': 'https://www.googleapis.com/auth/identitytoolkit',
        'aud': 'https://oauth2.googleapis.com/token',
        'iat': nowSeconds,
        'exp': nowSeconds + 3600
    ] as Map<String, Object>

    Map<String, String> requestBody = [
        'grant_type': 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion': getSignedJwt(header, payload, key['private_key'])
    ]

    setConnectionAddress(message, key['token_uri'])
    firebaseProjectId.set(message, key['project_id'])
    HttpUtil.setFormUrlEncodedRequest(message, requestBody)
}

static private String getSignedJwt(
    Map<String, Object> header, Map<String, Object> payload, String key)
{
    String jwtHeader = encodeBase64(JsonUtil.mapToJson(header))
    String jwtPayload = encodeBase64(JsonUtil.mapToJson(payload))
    String jwt = jwtHeader + '.' + jwtPayload

    KeyFactory keyFactory = KeyFactory.getInstance('RSA')
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key.getBytes())
    PrivateKey privateKey = keyFactory.generatePrivate(keySpec)

    Signature signature = Signature.getInstance('SHA256withRSA')
    signature.initSign(privateKey)
    signature.update(jwt.getBytes())
    jwt += '.' + signature.sign().encodeBase64().toString()

    return jwt
}

static private String encodeBase64(String text) {
    return text.getBytes().encodeBase64().toString()
}

static private void setConnectionAddress(Message message, String address) {
    message.setProperty('_connection-address', address)
}

static private void setConnectionQuery(Message message, String query) {
    message.setProperty('_connection-query', query)
}

static private String getArtifactAlias(Message message) {
    return message.getHeader('_oauth2-tokens-handler-artifact-alias', String)
}