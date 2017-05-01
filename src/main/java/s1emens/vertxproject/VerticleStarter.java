/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 * @author Cyberhawk
 */
public class VerticleStarter {
    private static final Logger LOG = LoggerFactory.getLogger(Master.class);
    public static void main(String[] args) {
        int countOfSlaveVerticles = -1;
        // Check key -c if there is count of slave-verticles
        if (args != null && args.length > 1) {
            int position = -1;
            for (int i = 0; i < (args.length - 1); i++) {
                if ("-c".equals(args[i]))
                    position = i + 1;
            }
            if (position != -1) {
                try {
                    countOfSlaveVerticles =
                            Integer.valueOf(
                            args[position]);
                }
                catch (NumberFormatException ex) {
                    LOG.info("Invalid count of slave-verticles " +
                            "(must be from 1 to 128): " + ex.toString());
                }
                catch (Exception ex) {}
            }
        }
        Vertx vertx = Vertx.vertx();
        Master master;
        /* If count of slave-verticles is incorrect -> just run master-verticle
        * Otherwise run master-verticle with specified count
        * of slave-verticles
        */
        if (countOfSlaveVerticles < 1 || countOfSlaveVerticles > 128)
            master = new Master();
        else
            master = new Master(countOfSlaveVerticles);
        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(master, options); //deploy Master-verticle
    }
}
