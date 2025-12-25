package com.example.akkajr.core.observability;

public final class MessageEvent {
    private final String type;
    private final long timestamp;
    private final String path;
    private final String messageId;
    private final String traceId;
    private final String detail;

    public MessageEvent(String type, long timestamp, String path, String messageId, String traceId, String detail) {
        this.type = type;
        this.timestamp = timestamp;
        this.path = path;
        this.messageId = messageId;
        this.traceId = traceId;
        this.detail = detail;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getDetail() {
        return detail;
    }
}
