package com.example.oms.actors.messages;

import lombok.Data;

public interface OrderFulfillmentResult {

    enum Success implements OrderFulfillmentResult{
        INSTANCE
    }

    @Data
    class Failure implements OrderFulfillmentResult {
        private final String reason;

        public static Failure of(String template, Object... args) {
            return new Failure(String.format(template, args));
        }
    }
}
