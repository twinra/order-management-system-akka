package com.example.oms.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.oms.domain.Order;
import lombok.Data;

import java.util.Random;

public class FulfillmentProviderActor extends AbstractBehavior<FulfillmentProviderActor.FulfillmentRequest> {

    @Data
    public static class FulfillmentRequest {
        public final long orderId;
        public final Order order;
        public final ActorRef<OrderManagerActor.Request> replyTo;
    }

    public interface FulfillmentResponse extends OrderManagerActor.Request {};

    @Data
    public static class FulfillmentFinished implements FulfillmentResponse {
        public final long orderId;
    };

    @Data
    public static class FulfillmentFailed implements FulfillmentResponse {
        public final long orderId;
        public final String reason;
    };


    private FulfillmentProviderActor(ActorContext<FulfillmentRequest> context) {
        super(context);
    }

    public static Behavior<FulfillmentRequest> create() {
        return Behaviors.setup(FulfillmentProviderActor::new);
    }

    @Override
    public Receive<FulfillmentRequest> createReceive() {
        return newReceiveBuilder()
                .onMessage(FulfillmentRequest.class, this::onEvent)
                .build();
    }

    private Behavior<FulfillmentRequest> onEvent(FulfillmentRequest event) {
        long orderId = event.getOrderId();
        boolean fulfilled = new Random().nextBoolean();
        event.replyTo.tell(fulfilled ? new FulfillmentFinished(orderId) : new FulfillmentFailed(orderId, "bad luck"));
        return this;
    }
}
