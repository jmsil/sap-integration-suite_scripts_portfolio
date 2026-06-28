package com.custom.scripts.v2

import com.custom.scripts.util.v2.JsonUtil
import com.custom.scripts.util.v2.SecurityUtil
import com.custom.scripts.util.v2.SecurityUtil.OAuth2ClientCredentials
import com.sap.it.script.v2.api.Message

import java.time.LocalDateTime

static Message setCachedAuthorization(Message message) {
    String cache = message.getBody(String)

    if (!cache) {
        setAuthHeader(message, null)
        return message
    }

    Map<String, Object> token = JsonUtil.jsonToMap(cache)
    int expiresInSeconds = token['expires_in'] as Integer
    expiresInSeconds -= Math.min(expiresInSeconds.intdiv(10) as Integer, 120)
    LocalDateTime createdIn = LocalDateTime.parse(token['created_in'].toString())
    LocalDateTime expiresIn = createdIn.plusSeconds(expiresInSeconds)

    if (LocalDateTime.now() <= expiresIn)
        setAuthHeader(message, token)

    return message
}

static Message setNewAuthorization(Message message) {
    Map<String, Object> token = JsonUtil.jsonToMap(message.getBody(String))
    token['created_in'] = LocalDateTime.now().toString()
    message.setBody(JsonUtil.mapToJson(token))
    setAuthHeader(message, token)
    return message
}

static Message setNewRequest(Message message) {
    String alias = message.getHeader('_oauth2-client-credentials-alias', String)
    OAuth2ClientCredentials credentials = SecurityUtil.getOAuth2ClientCredentials(alias)

    Map<String, String> body = ['scope': credentials.scope]
    message.setProperty('_connection-address', credentials.serviceUrl)

    if (credentials.sendAuthenticationAs == 'header') {
        String authentication = "${credentials.clientId}:${credentials.clientSecret}}"
        authentication = authentication.bytes.encodeBase64().toString()
        setAuthHeader(message, 'Basic', authentication)
    }
    else {
        body['client_id'] = credentials.clientId
        body['client_secret'] = credentials.clientSecret
    }

    if (credentials.sendGrantTypeAs == 'partOfBody')
        body['grant_type'] = 'client_credentials'
    else
        message.setProperty('_connection-query', 'grant_type=client_credentials')

    if (credentials.contentType == 'urlencoded') {
        message.setHeader('Content-Type', 'application/x-www-form-urlencoded')
        message.setBody(mapToFormUrlEncoded(body))
    }
    else {
        message.setHeader('Content-Type', 'application/json')
        message.setBody(JsonUtil.mapToJson(body))
    }

    return message
}

static private void setAuthHeader(Message message, Map<String, Object> token) {
    setAuthHeader(message, token?['token_type'] as String, token?['access_token'] as String)
}

static private void setAuthHeader(Message message, String type, String token) {
    message.setHeader(
        'Authorization',
        type && token ? "${type} ${token}" : null
    )
}

static private String mapToFormUrlEncoded(Map<String, String> map) {
    List<String> list = []
    map.each { String key, String value -> {
        key = URLEncoder.encode(key, 'UTF-8')
        value = URLEncoder.encode(value, 'UTF-8')
        list.add("${key}=${value}")
    }}
    return list.join('&')
}