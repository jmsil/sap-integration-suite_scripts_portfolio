package com.custom.scripts.util.v2

import com.sap.it.script.v2.api.Message

import java.nio.charset.Charset

class HttpUtil {
    static Map<String, String> parseQueryMap(String query) {
        List<String> list = parseQueryList(query)
        return list.collectEntries {
            String it -> it.tokenize('=')
        }
    }

    static List<String> parseQueryList(String query) {
        query ?= ''
        query = URLDecoder.decode(query, Charset.defaultCharset())
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
}