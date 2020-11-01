package com.example.oms.domain;

import com.example.oms.utils.StateMachine;
import lombok.Getter;

import static com.example.oms.domain.OrderState.*;

public class OrderStateMachine {

    private final StateMachine<OrderState> stateMachine;
    @Getter
    private long version;

    private OrderStateMachine(OrderState state, long version) {
        this.stateMachine = StateMachine.<OrderState>builder()
                .current(state)
                .transition(StateMachine.Transition.of(CREATED, PAID))
                .transition(StateMachine.Transition.of(PAID, IN_FULFILLMENT))
                .transition(StateMachine.Transition.of(IN_FULFILLMENT, CLOSED))
                .transition(StateMachine.Transition.of(IN_FULFILLMENT, FAILURE))
                .build();
        this.version = version;
    }

    public static OrderStateMachine create() {
        return new OrderStateMachine(CREATED, 0);
    }

    public static OrderStateMachine load(OrderState state, long version) {
        return new OrderStateMachine(state, version);
    }


    public OrderState getCurrentState() {
        return stateMachine.getCurrent();
    }

    public boolean canBeSet(OrderState state) {
        return stateMachine.canBeSet(state);
    }

    public boolean set(OrderState state) {
        boolean result = stateMachine.set(state);
        if(result)
            version++;
        return result;
    }
}
