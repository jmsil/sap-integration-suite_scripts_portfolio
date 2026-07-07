package src.main.resources.script.v2

import com.sap.it.script.v2.api.Message

static Message processData(Message message) {
    Throwable caughtException = Property.CAMEL_EXCEPTION_CAUGHT.get(message)
    Throwable handledException = caughtException

    while (handledException != null) {
        if (handledException.getClass().getName() == InternalCustomException.getName()) {
            Http.setInternalServerErrorResponse(
                message, handledException.getCause().getClass().getName())
            return message
        }

        handledException = handledException.getCause()
    }

    throw caughtException
}