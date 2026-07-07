package src.main.resources.script.v2

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

static Map<String, Object> jsonToMap(String text) {
    JsonSlurper parser = new JsonSlurper()
    Object parsed = parser.parseText(text)

    if (parsed instanceof List)
        parsed = ['': parsed]

    return parsed as Map<String, Object>
}

static String mapToJson(Map<String, Object> map) {
    return JsonOutput.toJson(map)
}