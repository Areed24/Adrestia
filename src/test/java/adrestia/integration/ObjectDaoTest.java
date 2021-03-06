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

import adrestia.ClymanConnector;
import adrestia.ObjectDao;
import java.io.PrintWriter;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
* Testing Object Data Access Object.
*/
@RunWith(SpringRunner.class)
@SpringBootTest
public class ObjectDaoTest {

  @Autowired
  ObjectDao objData;

  private static final double tolerance = 0.001;

  /**
   * Test the Object Data Access.
   */
  @Test
  public void testObjectCrud() throws Exception {
    // Open up a file that we can write some test results to
    // Shouldn't be relied on for automated testing but good for debugging
    PrintWriter testLogger = new PrintWriter("logs/testDao_obj.txt", "UTF-8");
    testLogger.println("Starting Test for Object Dao");
    try {
      // Create test
      double[] translation = {0.0, 0.0, 0.0};
      double[] rotationEuler = {0.0, 0.0, 0.0, 0.0};
      double[] scale = {0.0, 0.0, 0.0};
      String[] assets = {"TestAsset1", "TestAsset2"};
      ObjectDocument testDocument = new ObjectDocument("TestKey", "TestName",
          "TestType", "TestSubtype", "TestOwner", "TestScene",
          translation, rotationEuler, scale, assets, null);
      ObjectList crtResp = objData.create(testDocument);
      testLogger.println("Create Test Response: ");
      testLogger.println(crtResp.getErrorCode());
      testLogger.println(crtResp.getErrorMessage());
      assert (crtResp.getErrorCode() == 100);
      String clymanKey = crtResp.getDocuments()[0].getKey();

      // Get test
      ObjectList getResp = objData.get(clymanKey);
      assert (getResp.getErrorCode() == 100);
      assert (getResp.getNumRecords() > 0);
      assert (getResp.getDocuments()[0].getType().equals("TestType"));
      assert (getResp.getDocuments()[0].getOwner().equals("TestOwner"));

      // Update test
      testDocument.setKey(clymanKey);
      testDocument.setType("TestType2");
      testDocument.setOwner("TestOwner2");
      ObjectList updResp = objData.update(testDocument);
      assert (updResp.getErrorCode() == 100);
      ObjectList getResp2 = objData.get(clymanKey);
      assert (getResp2.getErrorCode() == 100);
      assert (getResp2.getNumRecords() > 0);
      assert (getResp2.getDocuments()[0].getType().equals("TestType2"));
      assert (getResp2.getDocuments()[0].getOwner().equals("TestOwner2"));

      // Lock Test
      ObjectList lockResp1 = objData.lock(clymanKey, "ud1");
      assert (lockResp1.getErrorCode() == 100);
      ObjectList lockResp2 = objData.lock(clymanKey, "ud2");
      assert (lockResp2.getErrorCode() != 100);
      ObjectList lockResp3 = objData.unlock(clymanKey, "ud1");
      assert (lockResp3.getErrorCode() == 100);
      ObjectList lockResp4 = objData.lock(clymanKey, "ud2");
      assert (lockResp4.getErrorCode() == 100);
      ObjectList lockResp5 = objData.unlock(clymanKey, "ud2");
      assert (lockResp5.getErrorCode() == 100);

      // Delete test
      ObjectList delResp = objData.destroy(clymanKey);
      assert (delResp.getErrorCode() == 100);
      ObjectList getResp3 = objData.get(clymanKey);
      assert (getResp3.getErrorCode() == 102);
    } catch (Exception e) {
      e.printStackTrace(testLogger);
      assert (false);
    } finally  {
      // Close the output text file
      testLogger.close();
    }
  }

}
