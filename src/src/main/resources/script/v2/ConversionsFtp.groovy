package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message
import groovy.transform.Field

import java.time.LocalDateTime

@Field
static private final Property FILE_NAME = new Property('_file-name')

static Message setFileName(Message message) {
    String fromType = Header.CONTENT_TYPE.get(message)
    String fromFormat = fromType.substring(fromType.indexOf("/") + 1)

    String toType = Header.ACCEPT.get(message)
    String toFormat = toType.substring(toType.indexOf("/") + 1)

    String formatedDateTime = LocalDateTime.now().format('yyyy-MM-dd_HH-mm-ss')
    FILE_NAME.set(message, "converted-from-${fromFormat}_${formatedDateTime}.${toFormat}")

    return message
}