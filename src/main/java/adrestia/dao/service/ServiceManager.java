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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.zeromq.ZContext;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZPoller;

/**
* Uses the Consul Discovery Client to find Service Instances.
*/
@Component
public class ServiceManager implements ServiceManagerInterface {

  // How long should entries stay in the redlist
  @Value("${server.zmq.redlist.duration}")
  private int redlistDuration;
  // How long should entries stay in the greylist
  @Value("${server.zmq.greylist.duration}")
  private int greylistDuration;
  // How long should entries stay in the blacklist
  @Value("${server.zmq.blacklist.duration}")
  private int blacklistDuration;

  // Consul Client for executing Service Discovery
  @Autowired
  DiscoveryClient consulClient;

  // Utility Provider, providing us with basic utility methods
  @Autowired
  UtilityProviderInterface utils;

  // Service Manager Logger
  private final Logger logger = LogManager.getLogger("adrestia.ServiceManager");

  // cache to hold blacklisted CLyman hosts
  // Keys will expire after 5 minutes, at which point Consul should be able
  // to determine if the service is active or inactive.
  Cache<String, String> blacklist = null;

  // Cache to hold greylisted CLyman hosts
  // Keys will expire after 30 seconds, if we report another failure in this
  // time then the service will be blacklisted
  Cache<String, String> greylist = null;

  // Cache to hold redlisted CLyman hosts
  // Keys will expire after 5 seconds, if we report another failure in this
  // time then the service will be blacklisted
  Cache<String, String> redlist = null;

  /**
  * Default empty ServiceManager constructor.
  */
  public ServiceManager() {
    super();
    logger.debug("RedList Duration: " + redlistDuration);
    logger.debug("GreyList Duration: " + greylistDuration);
    logger.debug("BlackList Duration: " + blacklistDuration);
  }

  private void initializeCaches() {
    blacklist = CacheBuilder.newBuilder()
        .expireAfterWrite(blacklistDuration, TimeUnit.SECONDS)
        .maximumSize(60)
        .build();

    greylist = CacheBuilder.newBuilder()
        .expireAfterWrite(greylistDuration, TimeUnit.SECONDS)
        .maximumSize(50)
        .build();

    redlist = CacheBuilder.newBuilder()
        .expireAfterWrite(redlistDuration, TimeUnit.SECONDS)
        .maximumSize(40)
        .build();
  }

  // Setup method to find and connect to an instance of a specified service name
  private ServiceInstance findService(String serviceName) {
    if (redlist == null) {
      initializeCaches();
    }
    ServiceInstance returnService = null;
    logger.info("Finding a new Service instance");
    logger.debug("RedList Size: " + redlist.size());
    logger.debug("GreyList Size: " + greylist.size());
    logger.debug("BlackList Size: " + blacklist.size());

    // Find an instance of CrazyIvan
    List<ServiceInstance> serviceInstances =
        consulClient.getInstances(serviceName);
    if (serviceInstances != null) {
      //Log if we find no service instances
      if (serviceInstances.size() == 0) {
        logger.error("No Service instances found");
      }
      // Find a service Instance
      // Start by getting a random start index
      int startIndex = utils.getRandomInt(serviceInstances.size());
      for (int i = 0; i < serviceInstances.size(); i++) {
        // Correct our compare index for the start index
        int currentIndex = i + startIndex;
        if (currentIndex >= serviceInstances.size()) {
          currentIndex = currentIndex - serviceInstances.size();
        }
        // Pull the service instance, and the value from the blacklist
        returnService = serviceInstances.get(currentIndex);
        String instanceString = returnService.getUri().toString().trim();
        logger.debug("Found Service Instance: " + instanceString);
        logger.debug("Blacklist: " + blacklist.asMap().keySet().toString());
        logger.debug("Redlist: " + redlist.asMap().keySet().toString());
        // We can go ahead and connect to the instance as long as it isn't
        // on the blacklist
        if (blacklist.asMap().containsKey(instanceString)
            || redlist.asMap().containsKey(instanceString)) {
          logger.error("Invalid host found");
          returnService = null;
        } else {
          return returnService;
        }
      }
    } else {
      logger.error("Unable to find Service instance");
    }
    return returnService;
  }

  /**
  * Report a Service Failure.
  * @param connectedInstance A ServiceInstance object with failed instance info
  */
  @Override
  public void reportFailure(ServiceInstance connectedInstance) {
    logger.info("Reporting Service Instance Failure");
    // Is the current host already on the greylist?
    try {
      String instanceString = connectedInstance.getUri().toString().trim();
      if (greylist.asMap().containsKey(instanceString)) {
        // We have found an entry in the greylist, add the host to the blacklist
        blacklist.put(instanceString, instanceString);
      } else {
        // We have no entry in the greylist, add the hostname to the greylist and redlist
        greylist.put(instanceString, instanceString);
        redlist.put(instanceString, instanceString);
      }
    } catch (Exception e) {
      logger.error("Error reporting service failure");
      logger.error(e.getMessage());
    }
    logger.debug("RedList Size: " + redlist.size());
    logger.debug("GreyList Size: " + greylist.size());
    logger.debug("BlackList Size: " + blacklist.size());
  }

  /**
  * Find an instance of Crazy Ivan.
  * @return A ServiceInstance object with the instance details found
  */
  @Override
  public ServiceInstance findCrazyIvan() {
    // Actually try to send the message
    try {
      return findService("Ivan");
    } catch (Exception e) {
      logger.error("Error retrieving service: ", e);
    }
    return null;
  }

  /**
  * Find an instance of CLyman.
  * @return A ServiceInstance object with the instance details found
  */
  @Override
  public ServiceInstance findClyman() {
    // Actually try to send the message
    try {
      return findService("Clyman");
    } catch (Exception e) {
      logger.error("Error retrieving service: ", e);
    }
    return null;
  }
}
