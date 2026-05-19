package me.ronygomes.ecommerce.core.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import me.ronygomes.ecommerce.core.application.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RabbitMQCommandBusTest {

    private record SampleCommand(String name, int qty) implements Command<Void> {
    }

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private RabbitMQCommandBus bus;

    @BeforeEach
    void setUp() throws Exception {
        factory = mock(ConnectionFactory.class);
        connection = mock(Connection.class);
        channel = mock(Channel.class);
        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);

        bus = new RabbitMQCommandBus("test_queue", factory, new ObjectMapper());
    }

    @Test
    void send_declaresDurableQueueAndPublishesSerializedCommand() throws Exception {
        SampleCommand command = new SampleCommand("widget", 3);

        bus.send(command).get();

        verify(channel).queueDeclare(eq("test_queue"), eq(true), eq(false), eq(false), any());

        ArgumentCaptor<AMQP.BasicProperties> propsCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(channel).basicPublish(eq(""), eq("test_queue"), propsCaptor.capture(), bodyCaptor.capture());

        assertThat(propsCaptor.getValue().getHeaders())
                .containsEntry(Command.HEADER_MESSAGE_TYPE, "SampleCommand");
        assertThat(new String(bodyCaptor.getValue()))
                .contains("\"name\":\"widget\"")
                .contains("\"qty\":3");
    }

    @Test
    void send_closesConnectionAfterPublish() throws Exception {
        bus.send(new SampleCommand("a", 1)).get();

        verify(channel).close();
        verify(connection).close();
    }

    @Test
    void send_whenChannelThrows_completesExceptionally() throws Exception {
        when(channel.queueDeclare(any(), anyBoolean(), anyBoolean(), anyBoolean(), any()))
                .thenThrow(new IOException("rabbit down"));

        assertThatThrownBy(() -> bus.send(new SampleCommand("a", 1)).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to send command to RabbitMQ");
    }

    @Test
    void send_returnsTheSameCommandIdItStampsAsXCommandIdHeader() throws Exception {
        UUID returned = bus.send(new SampleCommand("a", 1)).get();

        ArgumentCaptor<AMQP.BasicProperties> propsCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(""), eq("test_queue"), propsCaptor.capture(), any(byte[].class));

        Object headerId = propsCaptor.getValue().getHeaders().get(Command.HEADER_COMMAND_ID);
        assertThat(returned).isNotNull();
        assertThat(headerId).asString().isEqualTo(returned.toString());
    }

    @Test
    void send_stampsEachMessageWithAFreshXCommandIdHeader() throws Exception {
        bus.send(new SampleCommand("a", 1)).get();
        bus.send(new SampleCommand("b", 2)).get();

        ArgumentCaptor<AMQP.BasicProperties> propsCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel, org.mockito.Mockito.times(2))
                .basicPublish(eq(""), eq("test_queue"), propsCaptor.capture(), any(byte[].class));

        Object firstId = propsCaptor.getAllValues().get(0).getHeaders().get(Command.HEADER_COMMAND_ID);
        Object secondId = propsCaptor.getAllValues().get(1).getHeaders().get(Command.HEADER_COMMAND_ID);
        assertThat(firstId).asString().isNotBlank();
        assertThat(secondId).asString().isNotBlank();
        assertThat(firstId).isNotEqualTo(secondId);
    }
}
