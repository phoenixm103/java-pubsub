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

// [START pubsub_create_avro_schema]
import com.google.cloud.pubsub.v1.SchemaServiceClient;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.Schema;
import com.google.pubsub.v1.SchemaName;
import java.io.IOException;
import org.apache.avro.Schema.*;

public class CreateAvroSchemaExample {
  public static void main(String... args) throws Exception {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "your-project-id";
    String schemaId = "your-schema-id";
    String avscFile = "path/to/an/avro/schema/file/formatted/in/json";

    createAvroSchemaExample(projectId, schemaId, avscFile);
  }

  public static void createAvroSchemaExample(String projectId, String schemaId, String avscFile)
      throws IOException {

    ProjectName projectName = ProjectName.of(projectId);
    SchemaName schemaName = SchemaName.of(projectId, schemaId);

    // Parse an Avro schema file written in JSON.
    String avscSource = new Parser().parse(avscFile).toString();

    try (SchemaServiceClient schemaServiceClient = SchemaServiceClient.create()) {

      Schema schema =
          schemaServiceClient.createSchema(
              projectName,
              Schema.newBuilder()
                  .setName(schemaName.toString())
                  .setType(com.google.pubsub.v1.Schema.Type.AVRO)
                  .setDefinition(avscSource)
                  .build(),
              schemaId);

      System.out.println("Created a schema:\n" + schema);
    }
  }
}
// [END pubsub_create_avro_schema]
