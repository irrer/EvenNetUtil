/*
 * Copyright 2021 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.umro.EventNetClientTest;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class Recv implements DeliverCallback {

    private final static String QUEUE_NAME = "hello";

    static long start = System.currentTimeMillis();

    @Override
    public void handle(String consumerTag, Delivery message) {
        String text = new String(message.getBody(), StandardCharsets.UTF_8);
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