/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pubsub;

// [START pubsub_subscriber_concurrency_control]

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SubscribeWithConcurrencyControlExample {
  public static void main(String... args) throws Exception {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "your-project-id";
    String subscriptionId = "your-subscription-id";

    subscribeWithConcurrencyControlExample(projectId, subscriptionId);
  }

  public static void subscribeWithConcurrencyControlExample(
      String projectId, String subscriptionId) {
    ProjectSubscriptionName subscriptionName =
        ProjectSubscriptionName.of(projectId, subscriptionId);

    // Instantiate an asynchronous message receiver.
    MessageReceiver receiver =
        (PubsubMessage message, AckReplyConsumer consumer) -> {
          // Handle incoming message, then ack the received message.
          System.out.println("Id: " + message.getMessageId());
          System.out.println("Data: " + message.getData().toStringUtf8());
          consumer.ack();
        };

    Subscriber subscriber = null;
    try {
      // Provides an executor service for processing messages. The default `executorProvider` used
      // by the subscriber has a default thread count of 5.
      ExecutorProvider executorProvider =
          InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(4).build();

      // `setParallelPullCount` determines how many StreamingPull streams the subscriber will open
      // to receive message. It defaults to 1. `setExecutorProvider` configures an executor for the
      // subscriber to process messages. Here, the subscriber is configured to open 2 streams for
      // receiving messages, each stream creates a new executor with 4 threads to help process the
      // message callbacks. In total 2x4=8 threads are used for message processing.
      subscriber =
          Subscriber.newBuilder(subscriptionName, receiver)
              .setParallelPullCount(2)
              .setExecutorProvider(executorProvider)
              .build();

      // Start the subscriber.
      subscriber.startAsync().awaitRunning();
      System.out.printf("Listening for messages on %s:\n", subscriptionName.toString());
      // Allow the subscriber to run for 30s unless an unrecoverable error occurs.
      subscriber.awaitTerminated(30, TimeUnit.SECONDS);
    } catch (TimeoutException timeoutException) {
      // Shut down the subscriber after 30s. Stop receiving messages.
      subscriber.stopAsync();
    }
  }
}
// [END pubsub_subscriber_concurrency_control]
