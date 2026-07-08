package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message
import groovy.transform.Field

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime

@Field
static final private Header FIREBASE_PROJECT_ID = new Header('_firebase-project-id')
@Field
static final private Header ARTIFACT_ALIAS = new Header('_oauth2-tokens-handler-artifact-alias')
@Field
static final private Property ROUTE = new Property('_route')
@Field
static final private Property CONNECTION_ADDRESS = new Property('_connection-address')
@Field
static final private Property CONNECTION_QUERY = new Property('_connection-query')

static Message setCachedAuthorization(Message message) {
    String cache = message.getBody(String)

    if (!cache)
        return message

    Map<String, Object> token = Json.jsonToMap(cache)
    int expiresInSeconds = token['expires_in'] as Integer
    expiresInSeconds -= Math.min(expiresInSeconds.intdiv(10) as Integer, 120)
    LocalDateTime createdIn = LocalDateTime.parse(token['created_in'] as String)
    LocalDateTime expiresIn = createdIn.plusSeconds(expiresInSeconds)

    if (LocalDateTime.now() <= expiresIn)
        Http.setAuthorizationHeader(message, token)

    FIREBASE_PROJECT_ID.set(message, token['project_id'])
    return message
}

static Message setNewAuthorization(Message message) {
    Map<String, Object> token = Json.jsonToMap(message.getBody(String))
    String projectId = FIREBASE_PROJECT_ID.get(message)

    if (projectId)
        token['project_id'] = projectId

    token['created_in'] = LocalDateTime.now().toString()
    Http.setAuthorizationHeader(message, token)
    message.setBody(Json.mapToJson(token))

    return message
}

static Message setRequest(Message message) {
    switch (ROUTE.get(message)) {
        case 'cc-mtls' ->
            setRequestFromClientCredentials(message)
        case 'fb-skey' ->
            setRequestFromFirebaseServiceKey(message)
    }
    return message
}

static private void setRequestFromClientCredentials(Message message) {
    OAuth2ClientCredentials credentials = Security.getOAuth2ClientCredentials(
        ARTIFACT_ALIAS.get(message))
    CONNECTION_ADDRESS.set(message, credentials.serviceUrl)
    Map<String, String> requestBody = ['scope': credentials.scope]

    switch (credentials.sendAuthenticationAs) {
        case 'header' -> {
            String authentication = credentials.clientId + ':' + credentials.clientSecret
            authentication = encodeBase64(authentication)
            Http.setAuthorizationHeader(message, 'Basic', authentication)
        }
        case 'body' -> {
            requestBody['client_id'] = credentials.clientId
            requestBody['client_secret'] = credentials.clientSecret
        }
    }

    switch (credentials.sendGrantTypeAs) {
        case 'partOfBody'->
            requestBody['grant_type'] = 'client_credentials'
        case 'partOfURL' ->
            CONNECTION_QUERY.set(message, 'grant_type=client_credentials')
    }

    switch (credentials.contentType) {
        case 'json' ->
            Http.setJsonRequest(message, requestBody)
        case 'urlencoded' ->
            Http.setFormUrlEncodedRequest(message, requestBody)
    }
}

static private void setRequestFromFirebaseServiceKey(Message message) {
    long nowSeconds = System.currentTimeMillis().intdiv(1000)
    String secureParameter = Security.getSecureParameter(ARTIFACT_ALIAS.get(message))
    Map<String, String> key = Json.jsonToMap(secureParameter) as Map<String, String>

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

    CONNECTION_ADDRESS.set(message, key['token_uri'])
    FIREBASE_PROJECT_ID.set(message, key['project_id'])
    Http.setFormUrlEncodedRequest(message, requestBody)
}

static private String getSignedJwt(
    Map<String, Object> header, Map<String, Object> payload, String key)
{
    String jwt = encodeBase64(Json.mapToJson(header)) + '.' + encodeBase64(Json.mapToJson(payload))

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