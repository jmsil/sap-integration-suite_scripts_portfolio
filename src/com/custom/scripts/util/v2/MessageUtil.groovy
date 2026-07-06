package com.custom.scripts.util.v2

import com.sap.it.script.v2.api.Message

class MessageUtil {
    static final MessageHeader contentType = new MessageHeader('Content-Type')

    static class MessageHeader {
        final String header

        MessageHeader(String header) { this.header = header }

        <T> T get(Message message) {
            return message.getHeader(header) as T
        }

        void set(Message message, Object value) {
            message.setHeader(header, value)
        }

        void clear(Message message) {
            message.setHeader(header, null)
        }
    }

    static class MessageProperty {
        final String property

        MessageProperty(String property) { this.property = property }

        <T> T get(Message message) {
            return message.getProperty(property) as T
        }

        void set(Message message, Object value) {
            message.setProperty(property, value)
        }

        void clear(Message message) {
            message.setProperty(property, null)
        }
    }
}