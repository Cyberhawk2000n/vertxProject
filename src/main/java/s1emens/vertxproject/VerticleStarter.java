/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.Vertx;

/**
 *
 * @author Cyberhawk
 */
public class VerticleStarter {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Master master = new Master();
        vertx.deployVerticle(master); //deploy Master-verticle
    }
}
