package com.custom.scripts.v2

import com.sap.it.script.v2.api.Message

import java.time.LocalDateTime

static Message setFileName(Message message) {
    String fromType = message.getHeader('Content-Type', String)
    String fromFormat = fromType.substring(fromType.indexOf("/") + 1)

    String toType = message.getHeader('Accept', String)
    String toFormat = toType.substring(toType.indexOf("/") + 1)

    String formatedDateTime = LocalDateTime.now().format('yyyy-MM-dd_HH-mm-ss')

    message.setProperty(
        '_file-name',
        "converted-from-${fromFormat}_${formatedDateTime}.${toFormat}"
    )

    return message
}