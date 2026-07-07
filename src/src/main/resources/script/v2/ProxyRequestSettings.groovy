package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message
import groovy.transform.Field
import groovy.transform.Immutable

@Field
static final private Property RESOURCE = new Property('_resource')
@Field
static final private Map<String, ProxyRouteSettings> ROUTES = [
    'calculator': new ProxyRouteSettings(
        methods: ['GET'],
        paths: ['add', 'sub', 'mul', 'div'],
        query: [
            /paramA=[0-9]+/,
            /paramB=[0-9]+/
        ]
    ),
    'conversions-ftp': new ProxyRouteSettings(
        methods: ['POST']
    ),
    'firebase-users-management-sync': new ProxyRouteSettings(
        methods: ['POST', 'PATCH', 'DELETE']
    ),
    'firebase-users-management-async': new ProxyRouteSettings(
        methods: ['POST', 'PATCH', 'DELETE']
    ),
    'inter-statement-oauth2-mtls': new ProxyRouteSettings(
        methods: ['GET'],
        query: [
            /start-date=[0-9]{4}-[0-9]{2}-[0-9]{2}/,
            /end-date=[0-9]{4}-[0-9]{2}-[0-9]{2}/
        ]
    ),
    'sql-server-xslt': new ProxyRouteSettings(
        methods: ['GET', 'POST', 'PATCH']
    )
]

static Message processData(Message message) {
    setResourceAndPath(message)
    validateSettings(message)
    return message
}

private static void setResourceAndPath(Message message) {
    String resource
    String path = Header.CAMEL_HTTP_PATH.get(message)
    int slashPos = path.indexOf('/')

    if (slashPos == -1) {
        resource = path
        path = null
    }
    else {
        resource = path.substring(0, slashPos)
        path = path.substring(slashPos + 1)
    }

    RESOURCE.set(message, resource)
    Header.CAMEL_HTTP_PATH.set(message, path)
}

static private void validateSettings(Message message) {
    String resource = RESOURCE.get(message)
    ProxyRouteSettings settings = ROUTES[resource]

    if (settings == null || !isValid(message, Header.CAMEL_HTTP_PATH, settings.paths)) {
        Http.setNotFoundResponse(message)
        return
    }

    if (!isValid(message, Header.CAMEL_HTTP_METHOD, settings.methods)) {
        Http.setMethodNotAllowedResponse(message, settings.methods)
        return
    }

    String query = Header.CAMEL_HTTP_QUERY.get(message)
    List<String> querySettings = settings.query ?: []
    List<String> queryRequest = Http.parseQueryToList(query)

    if (querySettings.size() != queryRequest.size()) {
        Http.setBadRequestResponse(message, 'Wrong number of query parameters.')
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
            Http.setBadRequestResponse(
                message, "Missing or incorrect query parameter: ${paramName}.")
            return
        }
    }
}

static private boolean isValid(Message message, Header header, List<String> settings) {
    String headerValue = header.get(message)
    return (!headerValue && !settings) || settings?.contains(headerValue)
}

@Immutable
class ProxyRouteSettings {
    final List<String> methods
    final List<String> paths
    final List<String> query
}