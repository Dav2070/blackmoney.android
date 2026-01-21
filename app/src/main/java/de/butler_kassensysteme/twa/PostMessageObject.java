package de.butler_kassensysteme.twa;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class PostMessageObject {
    String type;
    @Nullable String data;

    PostMessageObject(String type, @Nullable String data) {
        this.type = type;
        this.data = data;
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", type);
            jsonObject.put("data", data);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonObject.toString();
    }
}
