package com.example.auradesktop.utils;

import com.google.gson.*;
import java.lang.reflect.Type;

public class MongoObjectIdDeserializer implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Case 1: ObjectId is an object like { "$oid": "60d5ecb..." }
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("$oid")) {
                return jsonObject.get("$oid").getAsString();
            }
        }
        // Case 2: ObjectId is just a plain string "60d5ecb..."
        return json.getAsString();
    }
}
