package com.example.oms.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.oms.actors.messages.OrderToFulfill;
import com.example.oms.domain.OrderState;
import com.example.oms.domain.OrderStateMachine;
import com.example.oms.service.Repository;

import java.util.Optional;
import java.util.Random;

public class OrderFulfillmentActor extends AbstractBehavior<OrderToFulfill> {

    private final Repository repository;
    private final Random random;

    private OrderFulfillmentActor(ActorContext<OrderToFulfill> context) {
        super(context);
        repository = new Repository();
        random = new Random();
    }

    public static Behavior<OrderToFulfill> create() {
        return Behaviors.setup(OrderFulfillmentActor::new);
    }

    @Override
    public Receive<OrderToFulfill> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderToFulfill.class, this::onEvent)
                .build();
    }

    private Behavior<OrderToFulfill> onEvent(OrderToFulfill event) {
        long orderId = event.getOrderId();
        Optional<OrderStateMachine> stateMachineOptional = repository.getStateMachineById(orderId);
        if(stateMachineOptional.isEmpty())
            getContext().getLog().warn("Order {} is not found", orderId);
        else {
            OrderStateMachine stateMachine = stateMachineOptional.get();
            if(stateMachine.getCurrentState() != OrderState.IN_FULFILLMENT) {
                getContext().getLog().warn("Order {} is not  in appropriate state: {}", orderId, stateMachine.getCurrentState());
            } else {
                boolean fulfilled = random.nextBoolean();
                stateMachine.set(fulfilled ? OrderState.CLOSED : OrderState.FAILURE);
                repository.updateStateMachine(orderId, stateMachine);
            }
        }
        return this;
    }
}
