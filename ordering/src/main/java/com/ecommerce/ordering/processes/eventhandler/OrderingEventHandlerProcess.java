package com.ecommerce.ordering.processes.eventhandler;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.ordering.domain.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.rabbitmq.client.*;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class OrderingEventHandlerProcess {
    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        MongoCollection<Document> detailViewCollection = database.getCollection("order_detail_view");
        MongoCollection<Document> adminListViewCollection = database.getCollection("admin_order_list_view");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "ordering_events";
        channel.exchangeDeclare(exchangeName, "fanout", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("OrderingEventHandler waiting for events on " + exchangeName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("OrderCreated".equals(messageType)) {
                    OrderCreated event = objectMapper.readValue(message, OrderCreated.class);
                    String oid = event.orderId().toString();

                    // Detail View
                    Document detailDoc = new Document()
                            .append("_id", oid)
                            .append("orderId", oid)
                            .append("orderNumber", event.orderNumber())
                            .append("guestToken", event.guestToken())
                            .append("customerInfo", new Document("name", event.customerInfo().name())
                                    .append("phone", event.customerInfo().phone())
                                    .append("email", event.customerInfo().email()))
                            .append("shippingAddress", new Document("line1", event.address().line1())
                                    .append("city", event.address().city())
                                    .append("postalCode", event.address().postalCode())
                                    .append("country", event.address().country()))
                            .append("items", event.items().stream()
                                    .map(i -> new Document("productId", i.getProductId().toString())
                                            .append("sku", i.getSkuSnapshot())
                                            .append("name", i.getNameSnapshot())
                                            .append("unitPrice", i.getUnitPriceSnapshot())
                                            .append("qty", i.getQuantity())
                                            .append("lineTotal", i.getLineTotal()))
                                    .collect(Collectors.toList()))
                            .append("totals", new Document("subtotal", event.totals().subtotal())
                                    .append("shippingFee", event.totals().shippingFee())
                                    .append("total", event.totals().total()))
                            .append("createdAt", Instant.ofEpochMilli(event.getTimestamp()).toString());

                    detailViewCollection.replaceOne(eq("_id", oid), detailDoc, new ReplaceOptions().upsert(true));

                    // Admin List View
                    Document listDoc = new Document()
                            .append("_id", oid)
                            .append("orderId", oid)
                            .append("orderNumber", event.orderNumber())
                            .append("date", Instant.ofEpochMilli(event.getTimestamp()).toString())
                            .append("customerName", event.customerInfo().name())
                            .append("phone", event.customerInfo().phone())
                            .append("total", event.totals().total());

                    adminListViewCollection.replaceOne(eq("_id", oid), listDoc, new ReplaceOptions().upsert(true));

                    System.out.println("Handled OrderCreated for " + oid);
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
