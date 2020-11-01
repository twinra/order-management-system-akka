package com.example.oms.service;

import com.example.oms.domain.Order;
import com.example.oms.domain.OrderState;
import com.example.oms.domain.OrderStateMachine;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Repository {

    @RequiredArgsConstructor
    private static class StateMachineRecord {
        public final OrderState state;
        public final long version;

        public OrderStateMachine map() {
            return OrderStateMachine.load(state, version);
        }

        public static StateMachineRecord map(OrderStateMachine osm) {
            return new StateMachineRecord(osm.getCurrentState(), osm.getVersion());
        }
    }

    //TODO: use database
    private long nextId = 1;
    private final Map<Long, Order> orders = new HashMap<>();
    private final Map<Long, StateMachineRecord> stateMachines = new HashMap<>();

    public long createOrder(Order order) {
        //returns new orderId
        long id = nextId++;
        orders.put(id, order);
        stateMachines.put(id, StateMachineRecord.map(OrderStateMachine.create()));
        return id;
    }

    public Optional<Order> getOrderById(long orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Optional<OrderStateMachine> getStateMachineById(long orderId) {
        return Optional.ofNullable(stateMachines.get(orderId)).map(StateMachineRecord::map);
    }

    public void updateStateMachine(long orderId, OrderStateMachine stateMachine) {
        getStateMachineById(orderId)
                .filter(sm -> sm.getVersion() == stateMachine.getVersion() - 1)
                .ifPresent(sm -> stateMachines.put(orderId, StateMachineRecord.map(stateMachine)));
    }
}
