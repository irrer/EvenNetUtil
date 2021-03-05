package edu.umro.EventNetClient;

import com.rabbitmq.client.Channel;


public class BasicConsumeWrapper {

    static void consumeWrapper(Channel channel, String queueName) { // , DeliverCallbackWrapper deliverCallbackWrapper) {
        boolean autoAck = false;

        CancelCallbackWrapper ccw = new CancelCallbackWrapper();

        DeliverCallbackWrapper dcw = new DeliverCallbackWrapper();

        try {
            channel.basicConsume(queueName, autoAck, dcw, ccw);
        } catch (Exception e) {
            System.out.println("BasicConsumeWrapper exception " + e);
        }

    }

}