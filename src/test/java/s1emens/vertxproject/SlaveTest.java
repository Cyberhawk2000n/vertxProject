/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.unit.*;
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
public class SlaveTest {
    
    public SlaveTest() {
    }
    
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();
    
    /**
     * Test of start method, of class Slave.
     * @param testContext
     */
    @Test
    public void testStart(TestContext testContext) {
        Vertx vertx = rule.vertx();
        Slave instance = new Slave();
        vertx.deployVerticle(instance);
        Async async = testContext.async();
        int countOfPoints = 100;
        SharedData sharedData = vertx.sharedData();
        Master.calculatePoints(sharedData, countOfPoints, true, 1);
        double[] expGradient = new double[2];
        expGradient[0] = 0.0;
        expGradient[1] = 0.0;
        // Get sharedMap by using specified number
        LocalMap<String, Object> map =
                sharedData.getLocalMap("sharedMap0");
        double[] theta = new double[2];
        theta[0] = 150.0;
        theta[1] = 0.18;
        map.put("theta0", theta[0]);
        map.put("theta1", theta[1]);
        // Partial gradients for theta0 and theta1 (all points)
        for (int i = 0; i < countOfPoints; i++)
        {
            double x = (double) map.get("x" + i);
            double y = (double) map.get("y" + i);
            double tmp = y - BasicFunction.calculateExample(theta, x);
            expGradient[0] += tmp;
            expGradient[1] += tmp * x;
        }
        vertx.eventBus().consumer("master", message -> {
            Buffer buffer = (Buffer)message.body();
            double[] gradient = new double[2];
            gradient[0] += buffer.getDouble(0);
            gradient[1] += buffer.getDouble(8);
            testContext.assertEquals(expGradient[0], gradient[0]);
            testContext.assertEquals(expGradient[1], gradient[1]);
            async.complete();
        });
        vertx.eventBus().send("slave", 0);
    }
}
