package com.custom.scripts.v2

import com.custom.scripts.util.v2.HttpUtil as ImportedHttpUtil
import com.sap.it.script.v2.api.Message

static Message parseQuery(Message message) {
    String query = message.getHeader('CamelHttpQuery', String)
    message.setHeader('CamelHttpQuery', null)
    message.setProperties(ImportedHttpUtil.parseQueryToMap(query))
    return message
}