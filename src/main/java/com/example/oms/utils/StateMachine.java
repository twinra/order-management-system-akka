package com.example.oms.utils;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Builder
@Getter
@Slf4j
@AllArgsConstructor
public class StateMachine<T> {

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class Transition<T> {
        private final T from;
        private final T to;
    }

    private T current;

    @Singular
    private final Set<Transition<T>> transitions;

    public boolean canBeSet(T next) {
        return transitions.contains(new Transition<T>(current, next));
    }

    public boolean set(T next) {
        boolean validTransition = canBeSet(next);
        if(validTransition) {
            current = next;
        }
        return validTransition;
    }
}
