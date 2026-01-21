package de.butler_kassensysteme.twa.Message;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class CaptureStripePaymentIntentMessage extends Message {
    String id;

    public CaptureStripePaymentIntentMessage(String id) {
        super("captureStripePaymentIntent");
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", type);
            jsonObject.put("id", id);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonObject.toString();
    }
}
