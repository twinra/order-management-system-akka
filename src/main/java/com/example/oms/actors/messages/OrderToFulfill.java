package com.example.oms.actors.messages;

import akka.actor.typed.ActorRef;
import com.example.oms.domain.Order;
import lombok.Data;

@Data
public class OrderToFulfill {
    private final long orderId;
    private final Order order;
    private final ActorRef<OrderFulfillmentResult> replyTo;
}
