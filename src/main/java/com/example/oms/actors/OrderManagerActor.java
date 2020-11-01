package com.example.oms.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.oms.domain.Order;
import com.example.oms.domain.OrderState;
import com.example.oms.domain.OrderStateMachine;
import com.example.oms.service.Repository;
import lombok.Data;

import java.util.Optional;

public class OrderManagerActor extends AbstractBehavior<OrderManagerActor.Request> {

    public interface Request {};

    @Data
    public static class OrderToCreate implements Request {
        public final Order order;
        public final ActorRef<OrderCreated> replyTo;
    }

    @Data
    public static class OrderPaid implements Request {
        public final long orderId;
    }

    @Data
    public static class OrderCreated {
        public final long orderId;
    }


    private final Repository repository;
    private final ActorRef<FulfillmentProviderActor.FulfillmentRequest> fulfillmentActor;

    private OrderManagerActor(ActorContext<Request> context,
                              ActorRef<FulfillmentProviderActor.FulfillmentRequest> fulfillmentActor,
                              Repository repository
    ) {
        super(context);
        this.repository = repository;
        this.fulfillmentActor = fulfillmentActor;
    }

    public static Behavior<Request> create(ActorRef<FulfillmentProviderActor.FulfillmentRequest> fulfillmentActor,
                                           Repository repository) {
        return Behaviors.setup(ctx -> new OrderManagerActor(ctx, fulfillmentActor, repository));
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderToCreate.class, this::onSave)
                .onMessage(OrderPaid.class, this::onPayment)
                .onMessage(FulfillmentProviderActor.FulfillmentFinished.class, this::onFulfilled)
                .onMessage(FulfillmentProviderActor.FulfillmentFailed.class, this::onFailed)
                .build();
    }

    private Behavior<Request> onSave(OrderToCreate event) {
        long id = repository.createOrder(event.getOrder());
        getContext().getLog().info("Order {} created", id);
        event.getReplyTo().tell(new OrderCreated(id));
        return this;
    }

    private Behavior<Request> onPayment(OrderPaid event) {
        long orderId = event.orderId;
        getContext().getLog().info("Payment for order {} confirmed", orderId);
        Optional<OrderStateMachine> stateMachineOptional = repository.getStateMachineById(orderId);
        Optional<Order> orderOptional = repository.getOrderById(orderId);
        if(stateMachineOptional.isEmpty() || orderOptional.isEmpty()) {
            getContext().getLog().info("Order {} not found", orderId);
        } else {
            OrderStateMachine stateMachine = stateMachineOptional.get();
            Order order = orderOptional.get();
            if(!stateMachine.canBeSet(OrderState.PAID)) {
                getContext().getLog().info("Order {} is in unexpected state: {}", orderId, stateMachine.getCurrentState());
            } else {
                stateMachine.set(OrderState.PAID);
                repository.updateStateMachine(orderId, stateMachine);

                stateMachine.set(OrderState.IN_FULFILLMENT);
                repository.updateStateMachine(orderId, stateMachine);
                fulfillmentActor.tell(new FulfillmentProviderActor.FulfillmentRequest(orderId, order, getContext().getSelf()));
            }
        }
        return this;
    }

    private Behavior<Request> onFulfilled(FulfillmentProviderActor.FulfillmentFinished event) {
        long orderId = event.orderId;
        getContext().getLog().info("Order {} successfully fulfilled", orderId);
        Optional<OrderStateMachine> stateMachineOptional = repository.getStateMachineById(orderId);
        if(stateMachineOptional.isEmpty()) {
            getContext().getLog().info("Order {} not found", orderId);
        } else {
            OrderStateMachine stateMachine = stateMachineOptional.get();
            if(!stateMachine.canBeSet(OrderState.CLOSED)) {
                getContext().getLog().info("Order {} is in unexpected state: {}", orderId, stateMachine.getCurrentState());
            } else {
                stateMachine.set(OrderState.CLOSED);
                repository.updateStateMachine(orderId, stateMachine);
                getContext().getLog().info("Order {} closed", orderId);
            }
        }
        return this;
    }

    private Behavior<Request> onFailed(FulfillmentProviderActor.FulfillmentFailed event) {
        long orderId = event.orderId;
        getContext().getLog().info("Order {} failed to fulfill", orderId);
        Optional<OrderStateMachine> stateMachineOptional = repository.getStateMachineById(orderId);
        if(stateMachineOptional.isEmpty()) {
            getContext().getLog().info("Order {} not found", orderId);
        } else {
            OrderStateMachine stateMachine = stateMachineOptional.get();
            if(!stateMachine.canBeSet(OrderState.FAILURE)) {
                getContext().getLog().info("Order {} is in unexpected state: {}", orderId, stateMachine.getCurrentState());
            } else {
                stateMachine.set(OrderState.FAILURE);
                repository.updateStateMachine(orderId, stateMachine);
                getContext().getLog().info("Order {} closed", orderId);
            }
        }
        return this;
    }
}
