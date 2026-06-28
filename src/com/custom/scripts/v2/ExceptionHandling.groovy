package com.custom.scripts.v2

import com.custom.scripts.util.v2.HttpUtil
import com.custom.scripts.util.v2.InternalCustomException
import com.custom.scripts.util.v2.JsonUtil
import com.sap.it.script.v2.api.Message
import com.sap.it.script.v2.api.exceptions.HttpOperationFailedException
import org.apache.cxf.binding.soap.SoapFault

static Message processData(Message message) {
    Throwable caughtException = message.getProperty('CamelExceptionCaught') as Throwable
    Throwable handledException = caughtException

    while (handledException != null) {
        switch (handledException.getClass().getName()) {
            case [
                'com.ctc.wstx.exc.WstxEOFException',
                'com.sap.it.xmljson.JsonXmlException',
                'com.ctc.wstx.exc.WstxParsingException',
                'com.ctc.wstx.exc.WstxUnexpectedCharException',
                'com.google.gson.stream.MalformedJsonException',
                'com.sap.it.rt.adapter.jdbc.exceptions.JDBCException',
                'com.sap.it.xml.validator.exception.XSDSchemaValidationException',
                'com.sap.it.rt.csvtoxml.converter.exception.CsvToXmlConversionException',
                'com.sap.it.rt.xmltocsv.converter.exception.XmlToCsvConversionException'
            ]:
                HttpUtil.setBadRequestResponse(message, handledException.getMessage())
                return message
            case SoapFault.getName():
                handleSoapFault(message, handledException as SoapFault)
                return message
            case HttpOperationFailedException.getName():
                handleHttpOperationFailedException(
                    message, handledException as HttpOperationFailedException)
                return message
            case InternalCustomException.getName():
                throw handledException
        }

        handledException = handledException.getCause()
    }

    throw new InternalCustomException(caughtException)
}

static private void handleSoapFault(Message message, SoapFault exception) {
    int exceptionStatusCode = exception.getStatusCode()

    if (exceptionStatusCode != 500)
        throw new InternalCustomException(exception)

    HttpUtil.setResponse(message, exceptionStatusCode, 'text/plain', exception.getMessage())
}

static private void handleHttpOperationFailedException(
    Message message, HttpOperationFailedException exception)
{
    int exceptionStatusCode = exception.getStatusCode()

    if (exceptionStatusCode != 400)
        throw new InternalCustomException(exception)

    String responseBody = null
    String exceptionContentType = message.getHeader('Content-Type', String)
    String exceptionMessage = exception.getMessage()
    String exceptionResponseBody = exception.getResponseBody()

    try {
        Map<String, Object> exceptionMap = JsonUtil.jsonToMap(exceptionResponseBody)
        exceptionMap['SAP_ExceptionMessage'] = exceptionMessage
        responseBody = JsonUtil.mapToJson(exceptionMap)
    }
    catch (ignored) {
        responseBody ?= "HTTP response: ${exceptionResponseBody}\n\n" +
            "Exception message: ${exceptionMessage}"
    }

    HttpUtil.setResponse(message, exceptionStatusCode, exceptionContentType, responseBody)
}