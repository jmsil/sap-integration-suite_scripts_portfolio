package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message

class Property {
    static final Property CAMEL_EXCEPTION_CAUGHT = new Property('CamelExceptionCaught')

    final String property

    Property(String property) { this.property = property }

    <T> T get(Message message) {
        return message.getProperty(property) as T
    }

    void set(Message message, Object value) {
        message.setProperty(property, value)
    }
}