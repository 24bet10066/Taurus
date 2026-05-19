package com.serviceos.notification.channel;

public record SendResult(boolean success, boolean suppressed, String channel, String error) {

    public static SendResult ok(String channel) {
        return new SendResult(true, false, channel, null);
    }

    public static SendResult fail(String error) {
        return new SendResult(false, false, null, error);
    }

    public static SendResult suppressed(String reason) {
        return new SendResult(false, true, null, reason);
    }
}
