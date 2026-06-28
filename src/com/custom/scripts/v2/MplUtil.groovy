package com.custom.scripts.v2

import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.msglog.MessageLogFactory
import com.sap.it.script.v2.api.Message

Message logQuery(Message message) {
    String query = message.getHeader('CamelHttpQuery', String)
    logData(message, 'Query', query)
    return message
}

Message logBody(Message message) {
    String body = message.getBody(String)
    logData(message, 'Body', body)
    return message
}

private void logData(Message message, String logName, String data) {
    MessageLogFactory messageLogFactory =
        binding.getVariable('messageLogFactory') as MessageLogFactory
    MessageLog messageLog = messageLogFactory.getMessageLog(message)
    messageLog?.addAttachmentAsString(logName, data, 'text/plain')
}