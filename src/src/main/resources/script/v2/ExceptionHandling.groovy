package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message
import com.sap.it.script.v2.api.exceptions.HttpOperationFailedException
import org.apache.cxf.binding.soap.SoapFault

static Message processData(Message message) {
    Throwable caughtException = Property.CAMEL_EXCEPTION_CAUGHT.get(message)
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
                Http.setBadRequestResponse(message, handledException.getMessage())
                return message
            case SoapFault.getName():
                handleSoapFault(message, handledException as SoapFault)
                return message
            case HttpOperationFailedException.getName():
                handleHttpOperationFailed(message, handledException as HttpOperationFailedException)
                return message
            case InternalCustomException.getName():
                throw handledException
        }

        handledException = handledException.getCause()
    }

    throw new InternalCustomException(caughtException)
}

static private void handleSoapFault(Message message, SoapFault exception) {
    int statusCode = exception.getStatusCode()

    if (statusCode != 500)
        throw new InternalCustomException(exception)

    Http.setResponse(message, statusCode, Header.CONTENT_TYPE.get(message), exception.getMessage())
}

static private void handleHttpOperationFailed(
    Message message, HttpOperationFailedException exception)
{
    int statusCode = exception.getStatusCode()

    if (statusCode != 400)
        throw new InternalCustomException(exception)

    Http.setResponse(
        message, statusCode, Header.CONTENT_TYPE.get(message), exception.getResponseBody())
}