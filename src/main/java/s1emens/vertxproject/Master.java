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
import io.vertx.core.shareddata.SharedData;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 *
 * @author Cyberhawk
 * master-verticle for main computation
 */
public class Master extends AbstractVerticle {
    private final Logger log = LoggerFactory.getLogger(Master.class);
    private static final int NUM_OF_POINTS = 1000000;
    private static final boolean PARALLEL = true;
    private static final double ACCURACY = 1e-4; // required accuracy to stop
    // example value for comparison function results
    private static final double X_EXAMPLE = 1000;
    private static final double ASSUMED_THETA0 = 1; // real value is 150
    private static final double ASSUMED_THETA1 = -50; // real value is 0,18
    private static final double ALFA_0 = 1e-2; // step of algorithm for theta0
    private static final double ALFA_1 = 1e-6; // step of algorithm for theta1
    private static LocalMap<String, Object> sharedMap;
    private int receivedResponces;
    private LocalTime start;
    private LocalTime finish;
    private double[] alfa;
    private double[] newTheta;
    private double[] gradient;
    private int countOfSlaveVerticles;
    
    @Override
    public void start() throws Exception {
        // count of slave-verticles (depends on count of points)
        countOfSlaveVerticles = 10;
        for (int i = 0; i < countOfSlaveVerticles; i++)
            vertx.deployVerticle(new Slave());
        SharedData sharedData = vertx.sharedData();
        sharedMap = sharedData.getLocalMap("sharedMap");
        sharedMap.put("slaves", countOfSlaveVerticles);
        
        vertx.eventBus().consumer("master", message -> {
            double haveAccuracy; //it is used to compare results of function
            //calculate theta0 and theta1 until have requirement accuracy
            // save old values theta0 and theta1
            if (receivedResponces == 0) {
                sharedMap.put("theta0", newTheta[0]);
                sharedMap.put("theta1", newTheta[1]);
                gradient[0] = 0;
                gradient[1] = 0;
            }
            // sum for gradients (from slave-verticles)
            Buffer buffer = (Buffer)message.body();
            gradient[0] += buffer.getDouble(0);
            gradient[1] += buffer.getDouble(8);
            receivedResponces++;
            //log.info(gradient[0] + ": " + gradient[1]);
            if (countOfSlaveVerticles == receivedResponces) {
                double[] theta = new double[2];
                theta[0] = newTheta[0];
                theta[1] = newTheta[1];
                newTheta[0] = theta[0] + alfa[0] * gradient[0] / NUM_OF_POINTS;
                newTheta[1] = theta[1] + alfa[1] * gradient[1] / NUM_OF_POINTS;
                log.info(newTheta[0] + ": " + newTheta[1]);
                try {
                    double reduceStep =
                        BasicFunction.calculateExample(theta, X_EXAMPLE) -
                        BasicFunction.calculateExample(newTheta, X_EXAMPLE);
                    haveAccuracy = Math.abs(reduceStep);
                    if (reduceStep > 0) {
                        alfa[0] /= 2;
                        alfa[1] /= 2;
                    }
                    if (haveAccuracy > ACCURACY) {
                        sharedMap.put("theta0", newTheta[0]);
                        sharedMap.put("theta1", newTheta[1]);
                        receivedResponces = 0;
                        vertx.eventBus().publish("slave", "continue");
                    }
                    else {
                        finish = LocalTime.now();
                        long time = ChronoUnit.MILLIS.between(start, finish);
                        log.info(
                            String.format("(Time %d ms): y = %.4f * x + %.4f%n",
                            time, newTheta[0], newTheta[1]));
                        vertx.close();
                    }
                }
                catch (Exception ex) {
                    log.error(ex);
                    vertx.close();
                }
            }
        });
        
        //main calculation
        vertx.setTimer(1, res -> {
            // create input data, 1000000 example points
            calculatePoints(NUM_OF_POINTS);
            // check used time for computation
            start = LocalTime.now();
            if (PARALLEL)
                parallelGradientDescent();
            else
            {
                double[] results;
                try {
                    results = gradientDescent();
                }
                catch (NullPointerException ex) {
                    log.error("Argument is null-poiner!", ex);
                    vertx.close();
                    return;
                }
                catch (IllegalArgumentException ex) {
                    log.error("Wrong argument size!", ex);
                    vertx.close();
                    return;
                }
                catch (Exception ex) {
                    log.error(ex);
                    vertx.close();
                    return;
                }
                finish = LocalTime.now();
                long time = ChronoUnit.MILLIS.between(start, finish);
                log.info(String.format("(Time %d ms): y = %.4f * x + %.4f%n", time, results[0], results[1]));
                vertx.close();
            }
        });
    }
    
    /*
    * calculates points for input data size
    * uses random to change real values
    */
    private double[] gradientDescent() {
        // initialize alfa, theta0 and theta1 ( y = theta0 + theta1 * x )
        // alfa can change during algorithm
        alfa = new double[2];
        alfa[0] = ALFA_0;
        alfa[1] = ALFA_1;
        newTheta = new double[2];
        newTheta[0] = ASSUMED_THETA0;
        newTheta[1] = ASSUMED_THETA1;
        double[] theta = new double[2];
        double haveAccuracy; //it is used to compare results of function
        //calculate theta0 and theta1 until have requirement accuracy
        do {
            // save old values theta0 and theta1
            theta[0] = newTheta[0];
            theta[1] = newTheta[1];
            // gradients for theta0 and theta1 (all points)
            double[] gradient = calculateGradient(theta);
            newTheta[0] = theta[0] + alfa[0] * gradient[0] / NUM_OF_POINTS;
            newTheta[1] = theta[1] + alfa[1] * gradient[1] / NUM_OF_POINTS;
            try {
            double reduceStep = BasicFunction.calculateExample(theta, X_EXAMPLE) -
                BasicFunction.calculateExample(newTheta, X_EXAMPLE);
                haveAccuracy = Math.abs(reduceStep);
                if (reduceStep > 0) {
                    alfa[0] /= 2;
                    alfa[1] /= 2;
                }
            }
            catch (Exception ex) {
                log.error(ex);
                return null;
            }
        }
        while (haveAccuracy > ACCURACY);
        return newTheta;
    }
    
    /*
    * prepares global data for message handler
    * sends first set of messages for slave-verticles
    */
    private void parallelGradientDescent() {
        // initialize alfa, theta0 and theta1 ( y = theta0 + theta1 * x )
        // alfa can change during algorithm
        // global variables for using in message handler
        alfa = new double[2];
        alfa[0] = ALFA_0;
        alfa[1] = ALFA_1;
        newTheta = new double[2];
        newTheta[0] = ASSUMED_THETA0;
        newTheta[1] = ASSUMED_THETA1;
        gradient = new double[2];
        receivedResponces = 0;
        for (int i = 0; i < countOfSlaveVerticles; i++)
            vertx.eventBus().send("slave", i);
        // start parallel gradient descent
        // following actions of master-verticle are in the message handler
        // waiting for messages from slave-verticles
    }
    
    /*
    * calculate gradient
    * return gradients for theta0 and theta1
    * master-verticle computes this
    */
    private double[] calculateGradient(double[] theta) 
            throws NullPointerException, IllegalArgumentException {
        if (theta == null)
            throw new NullPointerException();
        if (theta.length != 2)
            throw new IllegalArgumentException();
        double[] gradient =  new double[2];
        gradient[0] = 0;
        gradient[1] = 0;
        // gradients for theta0 and theta1 (all points)
        for (int i = 0; i < NUM_OF_POINTS; i++)
        {
            double x = (double) sharedMap.get("x" + i);
            double y = (double) sharedMap.get("y" + i);
            double tmp = y - BasicFunction.calculateExample(theta, x);
            gradient[0] += tmp;
            gradient[1] += tmp * x;
        }
        return gradient;
    }
    
    /*
    * calculates points for input data size
    * uses random to change real values
    */
    private void calculatePoints(int size) {
        // check if size is incorrect
        if (size < 1)
            return;
        sharedMap.put("size", size);
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            double x = random.nextDouble()*5000;
            // calculate y(x) and change it to not ideal value of function
            double y = BasicFunction.calculate(x) + 100*(random.nextDouble() - 0.5);
            sharedMap.put("x" + i, x);
            sharedMap.put("y" + i, y);
        }
    }
    
    @Override
    public void stop() throws Exception {
        log.info("Master stopped!");
    }
  
}
