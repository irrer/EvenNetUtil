package edu.umro.EventNetClient;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import java.io.IOException;

public class DeliverCallbackWrapper implements DeliverCallback {

    public void handleWrapper(String consumerTag, Delivery message) throws IOException {
        System.out.println("handleWrapper message: " + (new String(message.getBody())));
    }

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        handleWrapper(consumerTag, message);
    }
}

