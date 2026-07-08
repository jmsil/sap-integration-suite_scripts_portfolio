package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message

class Header {
    static final Header ALLOW = new Header('Allow')
    static final Header ACCEPT = new Header('Accept')
    static final Header CONTENT_TYPE = new Header('Content-Type')
    static final Header AUTHORIZATION = new Header('Authorization')
    static final Header CAMEL_HTTP_PATH = new Header('CamelHttpPath')
    static final Header CAMEL_HTTP_QUERY = new Header('CamelHttpQuery')
    static final Header CAMEL_HTTP_METHOD = new Header('CamelHttpMethod')
    static final Header CAMEL_HTTP_RESPONSE_CODE = new Header('CamelHttpResponseCode')

    final String name

    Header(String name) { this.name = name }

    <T> T get(Message message) {
        return message.getHeader(name) as T
    }

    void set(Message message, Object value) {
        message.setHeader(name, value)
    }
}