/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.AbstractVerticle;
import java.util.Random;

/**
 *
 * @author Cyberhawk
 * master-verticle for major computation
 */
public class Master extends AbstractVerticle {
    private static final int NUM_OF_POINTS = 100;
    @Override
    public void start() throws Exception {
        vertx.setPeriodic(5000, res -> {
            //create input data, 100 example points
            Point[] points = new Point[NUM_OF_POINTS];
            for (int i = 0; i < NUM_OF_POINTS; i++) {
                Random random = new Random();
                double x = random.nextDouble()*5000;
                //calculate y(x) and change it to not ideal point of function
                double y = BasicFunction.calculate(x) + 200*(random.nextDouble() - 0.5);
                points[i] = new Point(x, y);
                //System.out.println(points[i].getX() + ": " + points[i].getY());
            }
            //give start values for alfa, teta0 and teta1
            double alfa = 0.1;
            double[] theta;
            double[] accuracy = new double[2];
            accuracy[0] = 0.01;
            accuracy[1] = 10;
            double[] newTheta = new double[2];
            newTheta[0] = 1;
            newTheta[1] = 100;
            //calculate theta0 and theta1 until have requirement accuracy
            do {
                theta = newTheta;
                double sum0 = 0;
                //theta0 for all points
                for (int i = 0; i < NUM_OF_POINTS; i++)
                    sum0 += (points[i].getY() - (theta[0]*points[i].getX() + theta[1]) ) * points[i].getX();
                newTheta[0] = theta[0] + alfa * sum0 / NUM_OF_POINTS;
                double sum1 = 0;
                //theta1 for all points
                for (int i = 0; i < NUM_OF_POINTS; i++)
                    sum1 += (points[i].getY() - (theta[0]*points[i].getX() + theta[1]) ) * points[i].getX();
                newTheta[1] = theta[1] + alfa * sum1 / NUM_OF_POINTS;
            }
            while ( (Math.abs(newTheta[0] - theta[0]) > accuracy[0]) ||
                        (Math.abs(newTheta[1] - theta[1])) > accuracy[1] );
            System.out.println("\n" + newTheta[0] + ": " + newTheta[1] + "\n");
            System.out.println("Periodic event triggered.");
        });
    }
  
}
