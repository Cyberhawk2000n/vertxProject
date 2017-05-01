/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
/**
 *
 * @author Cyberhawk
 * Slave-verticle for computation of partial gradient
 */
public class Slave extends AbstractVerticle {
    private final Logger log = LoggerFactory.getLogger(Slave.class);
    
    @Override
    public void start() {
        /*
        * Register consumer with address "slave" for computing partial gradient
        * Message contains number of sharedMap with input data for computing
        * Answer message contains partial gradients
        */
        vertx.eventBus().consumer("slave", message -> {
            double[] gradient = new double[2];
            gradient[0] = 0;
            gradient[1] = 0;
            // Get sharedMap by using specified number
            int slaveNumber = (int) message.body();
            LocalMap<String, Object> map =
                    vertx.sharedData().getLocalMap("sharedMap" + slaveNumber);
            double[] theta = new double[2];
            theta[0] = (double) map.get("theta0");
            theta[1] = (double) map.get("theta1");
            int countOfPoints = (int) map.get("count");
            // Partial gradients for theta0 and theta1 (all points)
            for (int i = 0; i < countOfPoints; i++)
            {
                double x = (double) map.get("x" + i);
                double y = (double) map.get("y" + i);
                double tmp = y - BasicFunction.calculateExample(theta, x);
                gradient[0] += tmp;
                gradient[1] += tmp * x;
            }
            Buffer buffer = Buffer.buffer();
            buffer.appendDouble(gradient[0]);
            buffer.appendDouble(gradient[1]);
            vertx.eventBus().send("master", buffer);
        });
    }
    
    // Notice user when finished
    @Override
    public void stop() {
        log.info("Slave stopped!");
    }
    
}
