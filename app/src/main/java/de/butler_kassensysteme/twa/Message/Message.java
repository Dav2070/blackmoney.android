package de.butler_kassensysteme.twa.Message;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    public String type;

    public Message(String type) {
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", type);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonObject.toString();
    }
}
