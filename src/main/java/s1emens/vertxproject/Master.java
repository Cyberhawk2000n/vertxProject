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
import java.util.List;
import java.util.Random;

/**
 *
 * @author Cyberhawk
 * master-verticle for main computation
 */
public class Master extends AbstractVerticle {
    private final Logger log = LoggerFactory.getLogger(Master.class);
    private static final int NUM_OF_POINTS = 1000000;
    private static final double ACCURACY = 1e-4; // required accuracy to stop
    // example value for comparison function results
    private static final double X_EXAMPLE = 1000;
    private static final double ASSUMED_THETA0 = 1; // real value is 150
    private static final double ASSUMED_THETA1 = -50; // real value is 0,18
    private static final double ALFA_0 = 1e-2; // step of algorithm for theta0
    private static final double ALFA_1 = 1e-6; // step of algorithm for theta1
    private boolean parallel;
    private LocalMap<String, Object> sharedMap;
    private int receivedResponces;
    private LocalTime start;
    private LocalTime finish;
    private double[] alfa;
    private double[] newTheta;
    private double[] gradient;
    private int countOfSlaveVerticles;

    public Master() {
        this.countOfSlaveVerticles = -1;
    }

    public Master(int countOfSlaveVerticles) {
        this.countOfSlaveVerticles = countOfSlaveVerticles;
    }
    
    @Override
    public void start() throws Exception {
        SharedData sharedData = vertx.sharedData();
        if (countOfSlaveVerticles == -1) {
            List<String> args = this.processArgs();
            if (args != null && !args.isEmpty()) {
                int position = -1;
                for (int i = 0; i < (args.size() - 1); i++) {
                    if ("-c".equals(args.get(i)))
                        position = i + 1;
                }
                if (position != -1) {
                    try {
                        countOfSlaveVerticles =
                                Integer.valueOf(
                                args.get(position));
                    }
                    catch (NumberFormatException ex) {
                        log.info("Invalid count of slave-verticles " +
                                "(must be from 1 to 128): " + ex.toString());
                    }
                    catch (Exception ex) {}
                }
            }
        }
        if (countOfSlaveVerticles < 1 || countOfSlaveVerticles > 128)
            parallel = false;
        else
            parallel = true;
        if (parallel == false)
            sharedMap = sharedData.getLocalMap("sharedMap");
        else {
            // count of slave-verticles (depends on count of points)
            for (int i = 0; i < countOfSlaveVerticles; i++) {
                sharedData.getLocalMap("sharedMap" + i);
                vertx.deployVerticle(new Slave());
            }
            //message handler for distributed computing
            vertx.eventBus().consumer("master", message -> {
                double haveAccuracy; //it is used to compare results of function
                //calculate theta0 and theta1 until have requirement accuracy
                // save old values theta0 and theta1
                // sum for gradients (from slave-verticles)
                Buffer buffer = (Buffer)message.body();
                gradient[0] += buffer.getDouble(0);
                gradient[1] += buffer.getDouble(8);
                receivedResponces++;
                //log.info(gradient[0] + ": " + gradient[1] + "\n");
                if (countOfSlaveVerticles == receivedResponces) {
                    double[] theta = new double[2];
                    theta[0] = newTheta[0];
                    theta[1] = newTheta[1];
                    newTheta[0] = theta[0] + alfa[0] * gradient[0] / NUM_OF_POINTS;
                    newTheta[1] = theta[1] + alfa[1] * gradient[1] / NUM_OF_POINTS;
                    //log.info(newTheta[0] + ": " + newTheta[1]);
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
                            receivedResponces = 0;
                            gradient[0] = 0;
                            gradient[1] = 0;
                            for (int i = 0; i < countOfSlaveVerticles; i++) {
                                LocalMap<String, Object> map =
                                    sharedData.getLocalMap("sharedMap" + i);
                                map.put("theta0", newTheta[0]);
                                map.put("theta1", newTheta[1]);
                                vertx.eventBus().send("slave", i);
                            }
                        }
                        else {
                            finish = LocalTime.now();
                            long time = ChronoUnit.MILLIS.between(start, finish);
                            log.info(
                                String.format("(Time %d ms): y = %.4f * x + %.4f%n",
                                time, newTheta[1], newTheta[0]));
                            vertx.close();
                        }
                    }
                    catch (Exception ex) {
                        log.error(ex);
                        vertx.close();
                    }
                }
            });
        }
        //main calculation
        vertx.setTimer(1, res -> {
            // create input data, 1000000 example points
            calculatePoints(NUM_OF_POINTS);
            // check used time for computation
            start = LocalTime.now();
            if (parallel == true)
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
                log.info(String.format("(Time %d ms): y = %.4f * x + %.4f%n",
                        time, results[1], results[0]));
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
        gradient[0] = 0;
        gradient[1] = 0;
        receivedResponces = 0;
        for (int i = 0; i < countOfSlaveVerticles; i++) {
            LocalMap<String, Object> map =
                vertx.sharedData().getLocalMap("sharedMap" + i);
            map.put("theta0", newTheta[0]);
            map.put("theta1", newTheta[1]);
            vertx.eventBus().send("slave", i);
        }
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
    private void calculatePoints(int count) {
        // check if size is incorrect
        if (count < 1)
            return;
        Random random = new Random();
        if (parallel == false) {
            sharedMap.put("count", count);
            for (int i = 0; i < count; i++) {
                double x = random.nextDouble()*5000;
                // calculate y(x) and change it to not ideal value of function
                double y = BasicFunction.calculate(x) + 100*(random.nextDouble() - 0.5);
                sharedMap.put("x" + i, x);
                sharedMap.put("y" + i, y);
            }
        }
        else {
            for (int i = 0; i < countOfSlaveVerticles; i++) {
                LocalMap<String, Object> map =
                    vertx.sharedData().getLocalMap("sharedMap" + i);
                int slavePointCount = count / countOfSlaveVerticles;
                map.put("count", slavePointCount);
                for (int j = 0; j < slavePointCount; j++) {
                    double x = random.nextDouble()*5000;
                    // calculate y(x) and change it to not ideal value of function
                    double y = BasicFunction.calculate(x) + 100*(random.nextDouble() - 0.5);
                    map.put("x" + j, x);
                    map.put("y" + j, y);
                }
            }
        }
    }
    
    @Override
    public void stop() throws Exception {
        log.info("Master stopped!");
    }
  
}
