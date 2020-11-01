package com.example.oms.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.oms.service.Repository;
import lombok.Data;

public class MainActor extends AbstractBehavior<MainActor.Command> {

    public interface Command {
    }

    @Data
    public static class GetState implements Command {
        public final ActorRef<State> replyTo;
    }

    public enum Stop implements Command {INSTANCE}

    @Data
    public static class State {
        public final ActorRef<OrderManagerActor.Request> orderManagerActor;
    }


    private final ActorRef<OrderManagerActor.Request> orderManagerActor;

    private MainActor(ActorContext<Command> context) {
        super(context);
        var fulfillmentActor = context.spawn(FulfillmentProviderActor.create(), "fulfillment-provider");
        orderManagerActor = context.spawn(OrderManagerActor.create(fulfillmentActor, new Repository()), "order-manager");

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(MainActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetState.class, this::onGetState)
                .onMessage(Stop.class, this::onStop)
                .build();
    }

    private Behavior<Command> onGetState(GetState cmd) {
        cmd.replyTo.tell(new State(orderManagerActor));
        return this;
    }

    private Behavior<Command> onStop(Stop cmd) {
        return Behaviors.stopped();
    }
}
