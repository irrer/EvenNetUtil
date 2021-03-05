package edu.umro.EventNetClientTest;

import com.rabbitmq.client.*;

import java.io.IOException;

public class Recv implements DeliverCallback {

    private final static String QUEUE_NAME = "hello";

    static long start = System.currentTimeMillis();

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        String text = new String(message.getBody(), "UTF-8");
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(elapsed + " Received '" + text + "'");
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        /*
        channel.basicConsume(QUEUE_NAME, true, new Recv(), new Send());
         */

    }
}