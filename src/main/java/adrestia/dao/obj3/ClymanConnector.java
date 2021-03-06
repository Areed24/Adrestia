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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* Dao Implementation for Object Documents using Clyman.
*/
@Component
public class ClymanConnector implements ObjectDao {

  @Autowired
  ZmqConnection zmqConn;

  // How many retries should we attempt prior to reporting a failure
  @Value("${server.zmq.retries}")
  private int requestRetries;
  // How many milliseconds to wait for a reply
  @Value("${server.zmq.timeout}")
  private int requestTimeout;

  // Clyman Connection Logger
  private static final Logger logger =
      LogManager.getLogger("adrestia.ClymanConnector");

  /**
  * Default empty ClymanConnector constructor.
  */
  public ClymanConnector() {
    super();
  }

  // Send a message to Clyman, return the response.
  private ObjectList transaction(ObjectList inpObject) {
    // Set up a default return ObjectDocument List
    ObjectDocument[] baseReturnObjs = new ObjectDocument[0];
    ObjectList returnObjectList = new ObjectList(inpObject.getMsgType(),
        1, baseReturnObjs, 120, "Error Processing Request", "");

    // Send the information to Clyman
    try {
      // Construct our JSON from the ObjectDocument List
      ObjectMapper mapper = new ObjectMapper();
      String clymanMsg = mapper.writeValueAsString(inpObject);
      logger.debug("Clyman Message: " + clymanMsg);

      // Send the message to Clyman
      String replyString =
          zmqConn.send(clymanMsg, requestTimeout, requestRetries, "Clyman");
      logger.debug("Clyman Response: " + replyString);

      // Convert the Response back to a ObjectDocument List
      if (replyString != null) {
        returnObjectList = mapper.readValue(replyString, ObjectList.class);
      }
    } catch (Exception e) {
      logger.error("Error Retrieving Value from Clyman: ", e);
    }
    return returnObjectList;
  }

  // Convenience method to turn a ObjectDocument into a ObjectDocument List
  private ObjectList buildObjectList(ObjectDocument inpObject, int msgType) {
    ObjectDocument[] baseInpScns = {inpObject};
    return new ObjectList(msgType, 1, baseInpScns, 100, "", "");
  }

  // Execute a CRUD Transaction with Clyman
  private ObjectList crudTransaction(ObjectDocument inpObject, int msgType) {
    // Construct a ObjectDocument List, which we will then convert to JSON
    ObjectList inpObjectList = buildObjectList(inpObject, msgType);
    // Send the ObjectDocument List to Clyman and get the response
    return transaction(inpObjectList);
  }

  private ObjectList lockTransaction(String docKey, String ownerKey, int msgType) {
    ObjectDocument msgDocument = new ObjectDocument();
    msgDocument.setKey(docKey);
    msgDocument.setOwner(ownerKey);
    return crudTransaction(msgDocument, msgType);
  }

  /**
  * Create an ObjectDocument.
  */
  @Override
  public ObjectList create(ObjectDocument inpObject) {
    return crudTransaction(inpObject, 0);
  }

  /**
  * Update an ObjectDocument.
  */
  @Override
  public ObjectList update(ObjectDocument inpObject) {
    return crudTransaction(inpObject, 1);
  }

  /**
  * Retrieve an ObjectDocument.
  */
  @Override
  public ObjectList get(String docKey) {
    ObjectDocument obj = new ObjectDocument();
    obj.setKey(docKey);
    return crudTransaction(obj, 2);
  }

  /**
  * Remove an ObjectDocument.
  */
  @Override
  public ObjectList destroy(String docKey) {
    ObjectDocument obj = new ObjectDocument();
    obj.setKey(docKey);
    return crudTransaction(obj, 3);
  }

  /**
  * Query for one or more Objects.
  */
  @Override
  public ObjectList query(ObjectDocument inpObject) {
    return crudTransaction(inpObject, 4);
  }

  /**
  * Lock an ObjectDocument.
  */
  @Override
  public ObjectList lock(String docKey, String ownerKey) {
    return lockTransaction(docKey, ownerKey, 5);
  }

  /**
  * Unlock an ObjectDocument.
  */
  @Override
  public ObjectList unlock(String docKey, String ownerKey) {
    return lockTransaction(docKey, ownerKey, 6);
  }

}
