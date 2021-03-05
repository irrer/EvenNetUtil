package edu.umro.EventNetClient;

import com.rabbitmq.client.CancelCallback;

import java.io.IOException;

public class CancelCallbackWrapper implements CancelCallback {
    @Override
    public void handle(String consumerTag) throws IOException {
        System.out.println("CancelCallbackWrapper consumerTag: " + consumerTag);
    }
}


