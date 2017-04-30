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
 */
public class Slave extends AbstractVerticle {
    private final Logger log = LoggerFactory.getLogger(Slave.class);
    
    @Override
    public void start() {
        vertx.eventBus().consumer("slave", message -> {
            double[] gradient = new double[2];
            gradient[0] = 0;
            gradient[1] = 0;
            // gradients for theta0 and theta1 (all points)
            LocalMap<String, Object> sharedMap =
                    vertx.sharedData().getLocalMap("sharedMap");
            double[] theta = new double[2];
            theta[0] = (double) sharedMap.get("theta0");
            theta[1] = (double) sharedMap.get("theta1");
            int size = (int) sharedMap.get("size");
            for (int i = 0; i < size; i++)
            {
                double x = (double) sharedMap.get("x" + i);
                double y = (double) sharedMap.get("y" + i);
                double tmp = y - BasicFunction.calculateExample(theta, x);
                gradient[0] += tmp;
                gradient[1] += tmp * x;
            }
            Buffer buffer = Buffer.buffer();
            buffer.appendDouble(gradient[0]);
            buffer.appendDouble(gradient[1]);
            //log.info(gradient[0] + ": " + gradient[1]);
            vertx.eventBus().send("master", buffer);
        });
    }
    
    @Override
    public void stop() {
        log.info("Slave stopped!");
    }
    
}
