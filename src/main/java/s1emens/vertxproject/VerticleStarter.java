/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 *
 * @author Cyberhawk
 */
public class VerticleStarter {
    public static void main(String[] args) {
        VertxOptions  opt = new VertxOptions();
        Vertx vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        Master master = new Master();
        vertx.deployVerticle(master, options); //deploy Master-verticle
    }
}
