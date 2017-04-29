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
/**
 *
 * @author Cyberhawk
 */
public class Slave extends AbstractVerticle {
    private Logger log = LoggerFactory.getLogger(Slave.class);
    
    @Override
    public void start() {
        vertx.eventBus().consumer("slave", message -> {
            double x = (double) vertx.sharedData().getLocalMap("sharedMap").get("x0");
            double y = (double) vertx.sharedData().getLocalMap("sharedMap").get("y0");
            log.info(x + ": " + y);
            /*Buffer requestBuffer = (Buffer)message.body();
            int size = requestBuffer.getInt(0);
            double[] theta = new double[2];
            theta[0] = requestBuffer.getDouble(size*8+4);
            theta[1] = requestBuffer.getDouble(size*8+8);
            double[] gradient = new double[2];
            gradient[0] = 0;
            gradient[1] = 0;
            for (int i = 0; i < size; i++)
            {
                double x = requestBuffer.getDouble(i*8+4);
                double y = requestBuffer.getDouble(i*8+8);
                double tmp = y - BasicFunction.calculateExample(theta, x);
                gradient[0] += tmp;
                gradient[1] += tmp * x;
            }
            Buffer buffer = Buffer.buffer();
            buffer.appendDouble(gradient[0]);
            buffer.appendDouble(gradient[1]);
            log.info("Finished!\n");
            message.reply(buffer);*/
        });
    }
}
