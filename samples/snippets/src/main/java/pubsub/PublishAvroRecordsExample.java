/*
 * Copyright 2021 Google LLC
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

// [START pubsub_publish_avro_records]
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SchemaServiceClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.Encoding;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Schema;
import com.google.pubsub.v1.SchemaName;
import com.google.pubsub.v1.TopicName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.avro.Schema.*;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

public class PublishAvroRecordsExample {
  public static void main(String... args) throws Exception {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "your-project-id";
    // Use an existing topic with schema.
    String topicId = "your-topic-id";
    // Use an existing schema.
    String schemaId = "your-schema-id";
    // Use an Avro file that complies to the existing schema.
    String avroFile = "path/to/an/avro/file";

    publishAvroRecordsExample(projectId, topicId, schemaId, avroFile);
  }

  public static void publishAvroRecordsExample(
      String projectId, String topicId, String schemaId, String avroFile)
      throws IOException, ExecutionException, InterruptedException {

    Encoding encoding = null;
    Schema schema = null;

    TopicName topicName = TopicName.of(projectId, topicId);
    SchemaName schemaName = SchemaName.of(projectId, schemaId);

    // Get project schema.
    try (SchemaServiceClient schemaServiceClient = SchemaServiceClient.create()) {
      schema = schemaServiceClient.getSchema(schemaName);
    }

    // Get topic encoding type.
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
      encoding = topicAdminClient.getTopic(topicName).getSchemaSettings().getEncoding();
    }

    Publisher publisher = null;
    try {
      publisher = Publisher.newBuilder(topicName).build();

      // Prepare to read/deserialize Avro records.
      DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
      DataFileReader<GenericRecord> dataFileReader =
          new DataFileReader<>(new File(avroFile), datumReader);
      GenericRecord record = null;

      // Prepare to serialize/write Avro records to output stream.
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      DatumWriter<GenericRecord> datumWriter =
          new GenericDatumWriter<GenericRecord>(new Parser().parse(schema.getDefinition()));

      Encoder encoder = null;

      switch (encoding) {
        case BINARY:
          encoder = EncoderFactory.get().directBinaryEncoder(byteStream, /*reuse=*/ null);
          System.out.println(
              "Prepared to publish BINARY-encoded messages to " + topicName.toString());

          while (dataFileReader.hasNext()) {
            record = dataFileReader.next(record);

            // Encode the Avro record accordingly.
            datumWriter.write(record, encoder);
            byteStream.flush();
            ByteString data = ByteString.copyFrom(byteStream.toByteArray());

            PubsubMessage message = PubsubMessage.newBuilder().setData(data).build();
            ApiFuture<String> future = publisher.publish(message);
            System.out.println("Published message ID: " + future.get());
          }
          break;

        case JSON:
          System.out.println("Prepared to publish JSON-encoded messages to "
              + topicName.toString());

          while (dataFileReader.hasNext()) {
            record = dataFileReader.next(record);

            ByteString data = ByteString.copyFromUtf8(record.toString());
            PubsubMessage message = PubsubMessage.newBuilder().setData(data).build();

            ApiFuture<String> future = publisher.publish(message);
            System.out.println("Published message ID: " + future.get());
          }
          break;
      }


    } finally {
      if (publisher != null) {
        publisher.shutdown();
        publisher.awaitTermination(1, TimeUnit.MINUTES);
      }
    }
  }
}
// [END pubsub_publish_avro_records]