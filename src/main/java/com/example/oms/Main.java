package com.example.oms;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.StringUnmarshallers;
import com.example.oms.actors.MainActor;
import com.example.oms.actors.OrderManagerActor;
import com.example.oms.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static akka.actor.typed.javadsl.AskPattern.ask;
import static akka.http.javadsl.model.ContentTypes.TEXT_HTML_UTF8;

@Slf4j
@RequiredArgsConstructor
public class Main extends AllDirectives {
    private static final int PORT = 8080;
    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ActorSystem<?> system;
    private final ActorRef<OrderManagerActor.Request> orderManager;

    private Route createRoute() {
        return concat(
                path("hello", () ->
                        get(() -> complete(HttpResponse.create().withEntity(TEXT_HTML_UTF8, "<html><body><h2>Hello there!</h2></body></html>")))
                ),
                path("orders", () ->
                        post(() -> entity(Jackson.unmarshaller(Order.class), order -> {
                            CompletionStage<OrderManagerActor.OrderCreated> future = ask(orderManager, ref -> new OrderManagerActor.OrderToCreate(order, ref), TIMEOUT, system.scheduler());
                            return completeWithFuture(future.thenApply(event ->
                                            HttpResponse.create().withEntity(TEXT_HTML_UTF8, confirmationPage(event.getOrderId(), order.getTotalValue()))
                                    )
                            );
                        }))
                ),
                path("payments", () ->
                        get(() ->
                                parameter(StringUnmarshallers.LONG, "id", orderId ->
                                        parameter("value", totalValue -> {
                                                    orderManager.tell(new OrderManagerActor.OrderPaid(orderId));
                                                    return complete(String.format("Order %s is paid: %s", orderId, totalValue));
                                                }
                                        )
                                )
                        )
                )
        );
    }

    private String confirmationPage(long orderId, BigDecimal totalValue) {
        return String.format("<html><body>" +
                "<p>Order %d with total value of %s has been placed! <a href=\"localhost:%d/payments?id=%d&value=%s\">click here to pay</a></p>" +
                "</body></html>", orderId, totalValue, PORT, orderId, totalValue);
    }

    public static void main(String[] args) throws IOException {
        log.info("starting...");
        ActorSystem<MainActor.Command> system = ActorSystem.create(MainActor.create(), "main");
        CompletionStage<MainActor.State> futureState = ask(system, MainActor.GetState::new, TIMEOUT, system.scheduler());

        Main main = new Main(system, futureState.toCompletableFuture().join().orderManagerActor);

        Http http = Http.get(system);

        CompletionStage<ServerBinding> binding = http.newServerAt("localhost", PORT)
                .bind(main.createRoute());

        log.info("Server online at http://localhost:{}/\nPress RETURN to stop...", PORT);
        System.in.read();

        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }
}
