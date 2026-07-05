package com.custom.scripts.v2

import com.custom.scripts.util.v2.HttpUtil
import com.custom.scripts.util.v2.InternalCustomException
import com.sap.it.script.v2.api.Message
import groovy.transform.Immutable

static Message handleRequestSettings(Message message) {
    new ProxyRequestSettings(message).processData()
    return message
}

static Message handleException(Message message) {
    new ProxyExceptionHandling(message).processData()
    return message
}

class ProxyRequestSettings {
    private static final String RESOURCE_PROPERTY = '_resource'
    private static final Map<String, RouteSettings> ROUTES = [
        'calculator': new RouteSettings(
            methods: ['GET'],
            paths: ['add', 'sub', 'mul', 'div'],
            query: [
                /paramA=[0-9]+/,
                /paramB=[0-9]+/
            ]
        ),
        'conversions-ftp': new RouteSettings(
            methods: ['POST']
        ),
        'firebase-users-management': new RouteSettings(
            methods: ['POST', 'PATCH', 'DELETE']
        ),
        'inter-statement-oauth2-mtls': new RouteSettings(
            methods: ['GET'],
            query: [
                /start-date=[0-9]{4}-[0-9]{2}-[0-9]{2}/,
                /end-date=[0-9]{4}-[0-9]{2}-[0-9]{2}/
            ]
        ),
        'sql-server-xslt': new RouteSettings(
            methods: ['GET', 'POST', 'PATCH']
        )
    ]

    private final Message message

    ProxyRequestSettings(Message message) {this.message = message}

    void processData() {
        setResourceAndPath()
        validateSettings()
    }

    private void setResourceAndPath() {
        String resource
        String path = message.getHeader('CamelHttpPath', String)
        int slashPos = path.indexOf('/')

        if (slashPos == -1) {
            resource = path
            path = null
        }
        else {
            resource = path.substring(0, slashPos)
            path = path.substring(slashPos + 1)
        }

        message.setProperty(RESOURCE_PROPERTY, resource)
        message.setHeader('CamelHttpPath', path)
    }

    private void validateSettings() {
        String resource = message.getProperty(RESOURCE_PROPERTY)
        RouteSettings settings = ROUTES[resource]

        if (settings == null || !isValid('CamelHttpPath', settings.paths)) {
            HttpUtil.setNotFoundResponse(message)
            return
        }

        if (!isValid('CamelHttpMethod', settings.methods)) {
            HttpUtil.setMethodNotAllowedResponse(message, settings.methods)
            return
        }

        String query = message.getHeader('CamelHttpQuery', String)
        List<String> querySettings = settings.query ?: []
        List<String> queryRequest = HttpUtil.parseQueryToList(query)

        if (querySettings.size() != queryRequest.size()) {
            HttpUtil.setBadRequestResponse(message, 'Wrong number of query parameters.')
            return
        }

        for (String querySettingsIt in querySettings) {
            boolean found = false

            for (String queryRequestIt in queryRequest) {
                if (queryRequestIt ==~ querySettingsIt) {
                    found = true
                    break
                }
            }

            if (!found) {
                int equalPos = querySettingsIt.indexOf('=')
                String paramName = querySettingsIt.substring(0, equalPos)
                HttpUtil.setBadRequestResponse(
                    message, "Missing or incorrect query parameter: ${paramName}.")
                return
            }
        }
    }

    private boolean isValid(String header, List<String> settings) {
        String headerValue = message.getHeader(header, String)
        return (!headerValue && !settings) || settings?.contains(headerValue)
    }

    @Immutable
    static private class RouteSettings {
        final List<String> methods
        final List<String> paths
        final List<String> query
    }
}

class ProxyExceptionHandling {
    private final Message message

    ProxyExceptionHandling(Message message) { this.message = message }

    void processData() {
        Throwable caughtException = message.getProperty('CamelExceptionCaught') as Throwable
        Throwable handledException = caughtException

        while (handledException != null) {
            if (handledException.getClass().getName() == InternalCustomException.getName()) {
                HttpUtil.setInternalServerErrorResponse(
                    message, handledException.getCause().class.name)
                return
            }

            handledException = handledException.getCause()
        }

        throw caughtException
    }
}