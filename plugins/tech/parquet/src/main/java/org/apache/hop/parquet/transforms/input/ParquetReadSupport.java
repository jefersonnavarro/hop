/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.parquet.transforms.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hop.core.RowMetaAndData;
import org.apache.parquet.hadoop.api.InitContext;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

public class ParquetReadSupport extends ReadSupport<RowMetaAndData> {

  private List<ParquetField> fields;

  public ParquetReadSupport(List<ParquetField> fields) {
    this.fields = fields;
  }

  private MessageType messageType;

  @Override
  public ReadContext init(InitContext context) {
    this.messageType = context.getFileSchema();
    return new ReadContext(messageType, new HashMap<>());
  }

  @Override
  public RecordMaterializer<RowMetaAndData> prepareForRead(
      Configuration configuration,
      Map<String, String> keyValueMetaData,
      MessageType messageType,
      ReadContext readContext) {
    return new ParquetRecordMaterializer(messageType, fields);
  }

  /**
   * Gets messageType
   *
   * @return value of messageType
   */
  public MessageType getMessageType() {
    return messageType;
  }
}
