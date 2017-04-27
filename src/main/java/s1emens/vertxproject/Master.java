/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.AbstractVerticle;

/**
 *
 * @author Cyberhawk
 */
public class Master extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    vertx.setPeriodic(3000, res -> {
      System.out.println("Periodic event triggered.");
    });
  }
  
}
