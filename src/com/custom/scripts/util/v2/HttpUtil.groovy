package com.custom.scripts.util.v2

import com.sap.it.script.v2.api.Message

import java.nio.charset.Charset

class HttpUtil {
    static Map<String, String> parseQueryToMap(String query) {
        List<String> list = parseQueryToList(query)
        return list.collectEntries {
            String it -> it.tokenize('=')
        }
    }

    static List<String> parseQueryToList(String query) {
        query ?= ''
        query = urlDecode(query)
        query = query.replace('\$', '')
        return query.tokenize('&')
    }

    static void setBadRequestResponse(Message message, String body) {
        setResponse(message, 400, 'text/plain', body)
    }

    static void setNotFoundResponse(Message message) {
        setResponse(message, 404, null, null)
    }

    static void setMethodNotAllowedResponse(Message message, List<String> allow) {
        message.setHeader('Allow', allow.join(', '))
        setResponse(message, 405, null, null)
    }

    static void setInternalServerErrorResponse(Message message, String error) {
        setResponse(message, 500, 'text/plain', "An internal server error occured: ${error}.")
    }

    static void setResponse(Message message, int code, String contentType, String body) {
        message.setHeader('CamelHttpResponseCode', code)
        message.setHeader('Content-Type', contentType)
        message.setBody(body)
    }

    static void setAuthorizationHeader(Message message, Map<String, Object> token) {
        setAuthorizationHeader(
            message,
            token['token_type'] as String,
            token['access_token'] as String
        )
    }

    static void setAuthorizationHeader(Message message, String type, String token) {
        message.setHeader(
            'Authorization',
            type && token ? "${type} ${token}" : null
        )
    }

    static void setJsonRequest(Message message, Map<String, Object> body) {
        message.setHeader('Content-Type', 'application/json')
        message.setBody(JsonUtil.mapToJson(body))
    }

    static void setFormUrlEncodedRequest(Message message, Map<String, String> body) {
        List<String> list = []
        body.each { String key, String value -> {
            key = urlEncode(key)
            value = urlEncode(value)
            list.add("${key}=${value}")
        }}
        message.setHeader('Content-Type', 'application/x-www-form-urlencoded')
        message.setBody(list.join('&'))
    }

    static private String urlEncode(String value) {
        return URLEncoder.encode(value, Charset.defaultCharset())
    }

    static private String urlDecode(String value) {
        return URLDecoder.decode(value, Charset.defaultCharset())
    }
}