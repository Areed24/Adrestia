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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
* Rest Controller defining the Object API.
* Responsible for handling and responding to all Object API Requests.
*/
@RestController
@RequestMapping(path = "/v1/scene/{scn_name}/object")
public class ObjectController {

  // DAO Object allowing access to object data
  @Autowired
  ObjectDao objData;

  // Utility Provider, providing us with basic utility methods
  @Autowired
  UtilityProviderInterface utils;

  // Object Controller Logger
  private static final Logger logger =
      LogManager.getLogger("adrestia.ObjectController");

  // Save an Object to Clyman
  private ObjectList saveObject(ObjectDocument inpDoc, boolean docExists) {
    if (docExists) {
      return objData.update(inpDoc);
    }
    return objData.create(inpDoc);
  }

  // Query Clyman
  private ObjectList objectQuery(String sceneName, String objName) {
    // Execute a query against Clyman
    ObjectDocument queryObj = new ObjectDocument();
    queryObj.setName(objName);
    queryObj.setScene(sceneName);
    return objData.query(queryObj);
  }

  // Determine if a response from Clyman is a response or failure
  private boolean isSuccessResponse(ObjectList clymanResponse) {
    if (clymanResponse.getNumRecords() > 0
        && clymanResponse.getErrorCode() == 100) {
      return true;
    }
    return false;
  }

  /**
  * Object Retrieval.
  * Object name & object name input as path variables, no Request Parameters accepted.
  */
  @RequestMapping(path = "/{obj_name}", method = RequestMethod.GET)
  public ResponseEntity<ObjectDocument> getObject(@PathVariable("scn_name") String sceneName,
      @PathVariable("obj_name") String objName) {
    logger.info("Responding to Object Get Request");
    // Set up our response objects
    ObjectDocument returnObj = new ObjectDocument();
    HttpStatus returnCode = HttpStatus.OK;

    // Retrieve the object requested
    ObjectList clymanResponse = objectQuery(sceneName, objName);

    // If we have a successful response, then we pull the first value and
    // the error code
    if (isSuccessResponse(clymanResponse)) {
      returnObj = clymanResponse.getDocuments()[0];
      returnCode = utils.translateDvsError(clymanResponse.getErrorCode());
    } else {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("Failure Registered.  Clyman Response Error Code and Length:");
      logger.debug(clymanResponse.getNumRecords());
      logger.debug(clymanResponse.getErrorCode());
    }

    // Set up a response header to return a valid HTTP Response
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    // Create and return the new HTTP Response
    return new ResponseEntity<ObjectDocument>(returnObj, responseHeaders, returnCode);
  }

  /**
  * Object Create/Update.
  * Object Name & Object name input as path variable, no Request Parameters accepted.
  * POST Data read in with Object data.
  */
  @RequestMapping(path = "/{obj_name}",
      headers = "Content-Type=application/json",
      method = RequestMethod.POST)
  public ResponseEntity<ObjectDocument> updateObject(
      @PathVariable("scn_name") String sceneName,
      @PathVariable("obj_name") String objName,
      @RequestBody ObjectDocument inpObject) {
    logger.info("Responding to Object Save Request");
    ObjectDocument returnObj = new ObjectDocument();
    HttpStatus returnCode = HttpStatus.OK;

    // See if we can find the Object requested
    ObjectList clymanResponse = objectQuery(sceneName, objName);

    // If we have a successful response, then the Object exists
    boolean objectExists = false;
    if (isSuccessResponse(clymanResponse)) {
      objectExists = true;
      logger.debug("Existing Object found in Clyman");
      // Set the key on the input Object to the key from the response
      String clymanRespKey = clymanResponse.getDocuments()[0].getKey();
      if (clymanRespKey != null && !clymanRespKey.isEmpty()) {
        inpObject.setKey(clymanRespKey);
        logger.debug("Clyman Response Key: " + clymanRespKey);
      }
    }

    // Update the Object
    inpObject.setName(objName);
    inpObject.setScene(sceneName);
    ObjectList updateResponse = saveObject(inpObject, objectExists);

    // If we have a successful response, then we pull the first value
    if (isSuccessResponse(updateResponse)) {
      returnObj = updateResponse.getDocuments()[0];
      returnCode = utils.translateDvsError(updateResponse.getErrorCode());
    } else {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("Failure Registered.  Clyman Response Error Code and Length:");
      logger.debug(updateResponse.getNumRecords());
      logger.debug(updateResponse.getErrorCode());
    }

    // Set up a response header to return a valid HTTP Response
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    // Create and return the new HTTP Response
    return new ResponseEntity<ObjectDocument>(returnObj, responseHeaders, returnCode);
  }

  /**
  * Object Delete.
  * Object Name & Object name input as path variable, no Request Parameters accepted.
  */
  @RequestMapping(path = "/{obj_name}",
      headers = "Content-Type=application/json",
      method = RequestMethod.DELETE)
  public ResponseEntity<ObjectDocument> deleteObject(
      @PathVariable("scn_name") String sceneName,
      @PathVariable("obj_name") String objName) {
    logger.info("Responding to Object Delete Request");
    ObjectDocument returnObj = new ObjectDocument();
    HttpStatus returnCode = HttpStatus.OK;

    // See if we can find the Object requested
    ObjectList clymanResponse = objectQuery(sceneName, objName);

    // If we have a successful response, then the Object exists
    boolean objectExists = false;
    if (isSuccessResponse(clymanResponse)) {
      objectExists = true;
      logger.debug("Existing Object found in Clyman");
      // Set the key on the input Object to the key from the response
      String clymanRespKey = clymanResponse.getDocuments()[0].getKey();
      if (clymanRespKey != null && !clymanRespKey.isEmpty()) {
        logger.debug("Clyman Response Key: " + clymanRespKey);
        ObjectList deleteResponse = objData.destroy(clymanRespKey);
        // If we have a successful response, then set a success code
        if (isSuccessResponse(deleteResponse)) {
          returnObj = deleteResponse.getDocuments()[0];
          returnCode = utils.translateDvsError(deleteResponse.getErrorCode());
        } else {
          returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
          logger.debug("Failure Registered.  Clyman Response Error Code and Length:");
          logger.debug(deleteResponse.getNumRecords());
          logger.debug(deleteResponse.getErrorCode());
        }
      }
    } else {
      // Delete request for non-existing object
      returnCode = HttpStatus.INTERNAL_SERVER_ERROR;
      logger.debug("Key not found in Clyman response");
      logger.debug(clymanResponse.getNumRecords());
      logger.debug(clymanResponse.getErrorCode());
    }

    // Set up a response header to return a valid HTTP Response
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    // Create and return the new HTTP Response
    return new ResponseEntity<ObjectDocument>(returnObj, responseHeaders, returnCode);
  }

  /**
  * Object Query.
  * Object Name & Scene name input as path variable, Request Parameters accepted.
  */
  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<ObjectDocument> queryObject(
      @PathVariable("scn_name") String sceneName,
      @RequestParam(value = "type", defaultValue = "") String type,
      @RequestParam(value = "subtype", defaultValue = "") String subtype,
      @RequestParam(value = "owner", defaultValue = "") String owner) {
    logger.info("Responding to Object Query");
    ObjectDocument returnObj = new ObjectDocument();
    HttpStatus returnCode = HttpStatus.OK;

    // Execute a query against Clyman
    ObjectDocument queryObj = new ObjectDocument();
    queryObj.setScene(sceneName);
    if (!(type.isEmpty())) {
      queryObj.setType(type);
    }
    if (!(subtype.isEmpty())) {
      queryObj.setSubtype(subtype);
    }
    if (!(owner.isEmpty())) {
      queryObj.setOwner(owner);
    }
    ObjectList clymanResponse = objData.query(queryObj);

    // Update our HTTP Response based on the Clyman response
    if (isSuccessResponse(clymanResponse)) {
      returnObj = clymanResponse.getDocuments()[0];
      returnCode = utils.translateDvsError(clymanResponse.getErrorCode());
    } else {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("Failure Registered.  Clyman Response Error Code and Length:");
      logger.debug(clymanResponse.getNumRecords());
      logger.debug(clymanResponse.getErrorCode());
    }

    // Set up a response header to return a valid HTTP Response
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    // Create and return the new HTTP Response
    return new ResponseEntity<ObjectDocument>(returnObj, responseHeaders, returnCode);
  }

  private ResponseEntity<ObjectDocument> lockTransaction(String sceneName,
      String objName, String owner, boolean isLocking) {
    logger.info("Object Lock Transaction");
    ObjectDocument returnObj = new ObjectDocument();
    HttpStatus returnCode = HttpStatus.OK;

    // Execute a query against Clyman
    ObjectDocument queryObj = new ObjectDocument();
    queryObj.setScene(sceneName);
    queryObj.setName(objName);
    ObjectList clymanResponse = objData.query(queryObj);

    if (isSuccessResponse(clymanResponse)) {
      // Execute the lock transaction
      ObjectList lockResponse = null;
      if (isLocking) {
        lockResponse = objData.lock(clymanResponse.getDocuments()[0].getKey(), owner);
      } else {
        lockResponse = objData.unlock(clymanResponse.getDocuments()[0].getKey(), owner);
      }
      if (isSuccessResponse(lockResponse)) {
        returnObj = lockResponse.getDocuments()[0];
        returnCode = utils.translateDvsError(lockResponse.getErrorCode());
      } else {
        returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
        logger.debug("Failure Registered.  Clyman Response Error Code and Length:");
        logger.debug(clymanResponse.getNumRecords());
        logger.debug(clymanResponse.getErrorCode());
      }
    } else {
      returnCode = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      logger.debug("Failure Registered.  Clyman Response Error Code and Length:");
      logger.debug(clymanResponse.getNumRecords());
      logger.debug(clymanResponse.getErrorCode());
    }

    // Set up a response header to return a valid HTTP Response
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    // Create and return the new HTTP Response
    return new ResponseEntity<ObjectDocument>(returnObj, responseHeaders, returnCode);
  }

  /**
  * Object Lock.
  * Object Name & Scene name input as path variable, Request Parameters accepted.
  */
  @RequestMapping(path = "/{obj_name}/lock",
      method = RequestMethod.GET)
  public ResponseEntity<ObjectDocument> lockObject(
      @PathVariable("scn_name") String sceneName,
      @PathVariable("obj_name") String objName,
      @RequestParam(value = "owner", defaultValue = "") String owner) {
    return lockTransaction(sceneName, objName, owner, true);
  }

  /**
  * Object Unlock.
  * Object Name & Scene name input as path variable, Request Parameters accepted.
  */
  @RequestMapping(path = "/{obj_name}/lock",
      method = RequestMethod.DELETE)
  public ResponseEntity<ObjectDocument> unlockObject(
      @PathVariable("scn_name") String sceneName,
      @PathVariable("obj_name") String objName,
      @RequestParam(value = "owner", defaultValue = "") String owner) {
    return lockTransaction(sceneName, objName, owner, false);
  }
}
