package com.example.auradesktop.utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MongoDateDeserializer implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Case 1: Date is { "$date": "2023-11-20T10:00:00Z" } or { "$date": 169... }
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("$date")) {
                JsonElement dateElem = jsonObject.get("$date");
                if (dateElem.isJsonPrimitive() && dateElem.getAsJsonPrimitive().isNumber()) {
                   // Optional: Convert timestamp to readable string if needed, 
                   // or just return the string value of the number if the model expects string
                   long millis = dateElem.getAsLong();
                   // Convert to ISO-8601 string for consistency
                   return Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
                }
                return dateElem.getAsString();
            }
        }
        // Case 2: Date is already a string
        return json.getAsString();
    }
}
