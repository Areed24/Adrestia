/*
Apache2 License Notice
Copyright 2017 Alex Barry

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package adrestia;

import adrestia.ObjectDocument;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
* Represents a List of Objects, Required for communication with CLyman.
*/
public class ObjectList {

  @JsonProperty("msg_type")
  private int msgType;
  @JsonProperty("num_records")
  private long numRecords;
  @JsonProperty("objects")
  private ObjectDocument[] documents;
  @JsonProperty("err_code")
  private int errorCode;
  @JsonProperty("err_msg")
  private String errorMessage;
  @JsonProperty("transaction_id")
  private String transactionId;

  /**
  * Default empty ObjectList constructor.
  */
  public ObjectList() {
    super();
  }

  /**
  * Default ObjectList constructor.
  */
  public ObjectList(int msgType, long numRecords, ObjectDocument[] documents,
      int errorCode, String errorMessage, String transactionId) {
    super();
    this.msgType = msgType;
    this.numRecords = numRecords;
    this.documents = documents;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.transactionId = transactionId;
  }

  /**
  * Returns value of msgType.
  * @return The integer message type of the Object List
  */
  @JsonGetter("msg_type")
  public int getMsgType() {
    return this.msgType;
  }

  /**
  * Returns value of numRecords.
  * @return The number of records associated with the message
  */
  @JsonGetter("num_records")
  public long getNumRecords() {
    return this.numRecords;
  }

  /**
  * Returns value of Object Documents.
  * @return The list of object documents contained in the message
  */
  @JsonGetter("objects")
  public ObjectDocument[] getDocuments() {
    return this.documents;
  }

  /**
  * Returns value of errorCode.
  * @return The integer error code of the message
  */
  @JsonGetter("err_code")
  public int getErrorCode() {
    return this.errorCode;
  }

  /**
  * Returns value of errorMessage.
  * @return A Human-Readable error message
  */
  @JsonGetter("err_msg")
  public String getErrorMessage() {
    return this.errorMessage;
  }

  /**
  * Returns value of transactionId.
  * @return A String transaction ID
  */
  @JsonGetter("transaction_id")
  public String getTransactionId() {
    return this.transactionId;
  }

  /**
  * Sets new value of msgType.
  * @param msgType The integer message type
  */
  @JsonSetter("msg_type")
  public void setMsgType(int msgType) {
    this.msgType = msgType;
  }

  /**
  * Sets new value of numRecords.
  * @param numRecords The number of records for the message
  */
  @JsonSetter("num_records")
  public void setNumRecords(long numRecords) {
    this.numRecords = numRecords;
  }

  /**
  * Sets new value of documents.
  * @param documents an array of ObjectDocument objects
  */
  @JsonSetter("objects")
  public void setDocuments(ObjectDocument[] documents) {
    this.documents = documents;
  }

  /**
  * Sets new value of errorCode.
  * @param errorCode The integer error code of the message
  */
  @JsonSetter("err_code")
  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  /**
  * Sets new value of errorMessage.
  * @param errorMessage The Human-Readable error message
  */
  @JsonSetter("err_msg")
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
  * Sets new value of transactionId.
  * @param transactionId The transaction ID for the message
  */
  @JsonSetter("transaction_id")
  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }
}
