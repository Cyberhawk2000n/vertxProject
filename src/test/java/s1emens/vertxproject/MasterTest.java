/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 *
 * @author Cyberhawk
 */
@RunWith(VertxUnitRunner.class)
public class MasterTest {
    
    public MasterTest() {
    }

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    /**
     * Test of calculatePoints method, of class Master
     * Correct non-parallel initialization
     * @param testContext
     */
    @Test
    public void testCalculatePointsCorrectNonParallel(TestContext testContext) {
        Vertx vertx = rule.vertx();
        SharedData sharedData = vertx.sharedData();
        int expCount = 10;
        boolean parallel = false;
        int countOfSlaveVerticles = -1;
        int expResult = 0;
        int result = Master.calculatePoints(sharedData, expCount, parallel,
                countOfSlaveVerticles);
        testContext.assertEquals(expResult, result);
        LocalMap<String, Object> map = sharedData.getLocalMap("sharedMap");
        int count = (int)map.get("count");
        testContext.assertEquals(expCount, count);
        int pointPosition = expCount / 2;
        testContext.assertNotNull(map.get("x" + pointPosition));
        testContext.assertNotNull(map.get("y" + pointPosition));
    }
    
    /**
     * Test of calculatePoints method, of class Master.
     * Correct parallel initialization
     * @param testContext
     */
    @Test
    public void testCalculatePointsCorrectParallel(TestContext testContext) {
        Vertx vertx = rule.vertx();
        SharedData sharedData = vertx.sharedData();
        int expCount = 10;
        boolean parallel = true;
        int countOfSlaveVerticles = 2;
        int expResult = 0;
        int result = Master.calculatePoints(sharedData, expCount, parallel,
                countOfSlaveVerticles);
        testContext.assertEquals(expResult, result);
        LocalMap<String, Object> map = sharedData.getLocalMap("sharedMap0");
        int count = (int)map.get("count");
        int expPointCount = expCount / countOfSlaveVerticles;
        testContext.assertEquals(expPointCount, count);
        int pointPosition = expPointCount / 2;
        testContext.assertNotNull(map.get("x" + pointPosition));
        testContext.assertNotNull(map.get("y" + pointPosition));
        LocalMap<String, Object> map2 = sharedData.getLocalMap("sharedMap1");
        int count2 = (int)map2.get("count");
        int expPointCount2 = expCount / countOfSlaveVerticles;
        testContext.assertEquals(expPointCount2, count2);
        int pointPosition2 = expPointCount2 / 2;
        testContext.assertNotNull(map2.get("x" + pointPosition2));
        testContext.assertNotNull(map2.get("y" + pointPosition2));
    }
    
    /**
     * Test of calculatePoints method, of class Master.
     * Not correct non-parallel initialization
     * Negative count of points
     * @param testContext
     */
    @Test
    public void testCalculatePointsNegativePointCount(TestContext testContext) {
        Vertx vertx = rule.vertx();
        SharedData sharedData = vertx.sharedData();
        int expCount = -10;
        boolean parallel = false;
        int countOfSlaveVerticles = 2;
        int expResult = -1;
        int result = Master.calculatePoints(sharedData, expCount, parallel,
                countOfSlaveVerticles);
        testContext.assertEquals(expResult, result);
    }
    
    /**
     * Test of calculatePoints method, of class Master.
     * Not correct parallel initialization
     * Negative count of slave-verticles
     * @param testContext
     */
    @Test
    public void testCalculatePointsNegativeSlaveCount(TestContext testContext) {
        Vertx vertx = rule.vertx();
        SharedData sharedData = vertx.sharedData();
        int expCount = 10;
        boolean parallel = true;
        int countOfSlaveVerticles = -2;
        int expResult = -1;
        int result = Master.calculatePoints(sharedData, expCount, parallel,
                countOfSlaveVerticles);
        testContext.assertEquals(expResult, result);
    }
    
    /**
     * Test of calculatePoints method, of class Master.
     * Not correct parallel initialization
     * Null pointer exception for shared map
     * @param testContext
     */
    @Test(expected = NullPointerException.class)
    public void testCalculatePointsNullPointerException(TestContext testContext) {
        SharedData sharedData = null;
        int expCount = 10;
        boolean parallel = true;
        int countOfSlaveVerticles = 2;
        int expResult = 1;
        int result = Master.calculatePoints(sharedData, expCount, parallel,
                countOfSlaveVerticles);
        testContext.assertEquals(expResult, result);
    }
}
