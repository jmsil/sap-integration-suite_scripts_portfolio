package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message
import groovy.transform.Field
import groovy.transform.Immutable

@Field
static final private Property PUBLIC_PATH = new Property('_public-path')
@Field
static final private Property RESOURCE = new Property('_resource')
@Field
static final private Map<String, RouteSettings> ROUTES = [
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
    'google-firebase-auth-users': new RouteSettings(
        methods: ['POST', 'PATCH', 'DELETE'],
        rejectPublicCall: true
    ),
    'google-firebase-auth-users-async': new RouteSettings(
        methods: ['POST', 'PATCH', 'DELETE'],
        rejectPublicCall: true
    ),
    'google-firebase-auth-users-async-response': new RouteSettings(
        methods: ['GET'],
        rejectPublicCall: true
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

static Message processData(Message message) {
    String resource
    String publicPath = PUBLIC_PATH.get(message)
    String camelHttpPath = Header.CAMEL_HTTP_PATH.get(message)
    boolean isPublicCall = camelHttpPath.startsWith(publicPath)
    camelHttpPath = camelHttpPath.replace(publicPath, '')
    int slashPos = camelHttpPath.indexOf('/')

    if (slashPos == -1) {
        resource = camelHttpPath
        camelHttpPath = null
    }
    else {
        resource = camelHttpPath.substring(0, slashPos)
        camelHttpPath = camelHttpPath.substring(slashPos + 1)
    }

    RESOURCE.set(message, resource)
    Header.CAMEL_HTTP_PATH.set(message, camelHttpPath)

    validateSettings(message, isPublicCall)

    return message
}

static private void validateSettings(Message message, boolean isPublicCall) {
    RouteSettings settings = ROUTES[RESOURCE.get(message)]

    if (
        settings == null ||
        (isPublicCall && settings.rejectPublicCall) ||
        !isValid(message, Header.CAMEL_HTTP_PATH, settings.paths))
    {
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
class RouteSettings {
    final List<String> methods
    final List<String> paths
    final List<String> query
    final boolean rejectPublicCall
}