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
 * Master-verticle for main computation
 */
public class Master extends AbstractVerticle {
    private final Logger log = LoggerFactory.getLogger(Master.class);
    private static final int NUM_OF_POINTS = 1000000;
    private static final double ACCURACY = 1.0e-4; // required accuracy to stop
    // Example value for comparison function results
    private static final double X_EXAMPLE = 1000.0;
    private static final double ASSUMED_THETA0 = 1.0; // real value is 150
    private static final double ASSUMED_THETA1 = -50.0; // real value is 0,18
    private static final double ALFA_0 = 1.0e-2; // step of algorithm for theta0
    private static final double ALFA_1 = 1.0e-6; // step of algorithm for theta1
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
        // Check key -c if there is count of slave-verticles
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
        /* If count of slave-verticles is incorrect -> just run master-verticle
        * Otherwise run master-verticle with specified count
        * of slave-verticles
        */
        if (countOfSlaveVerticles < 1 || countOfSlaveVerticles > 128)
            parallel = false;
        else
            parallel = true;
        if (parallel == false)
            sharedMap = sharedData.getLocalMap("sharedMap");
        else {
            // Deploy specified count of slave-verticles
            for (int i = 0; i < countOfSlaveVerticles; i++) {
                sharedData.getLocalMap("sharedMap" + i);
                vertx.deployVerticle(new Slave());
            }
            //Message handler for distributed computing
            vertx.eventBus().consumer("master", message -> {
                double haveAccuracy; //it is used to compare results of function
                //calculate theta0 and theta1 until have requirement accuracy
                // save old values theta0 and theta1
                // sum for gradients (from slave-verticles)
                Buffer buffer = (Buffer)message.body();
                gradient[0] += buffer.getDouble(0);
                gradient[1] += buffer.getDouble(8);
                receivedResponces++;
                // If have answer messages from all slave-verticles ->
                // Calculate new values of theta0 and theta1
                if (countOfSlaveVerticles == receivedResponces) {
                    double[] theta = new double[2];
                    theta[0] = newTheta[0];
                    theta[1] = newTheta[1];
                    newTheta[0] =
                            theta[0] + alfa[0] * gradient[0] / NUM_OF_POINTS;
                    newTheta[1] =
                            theta[1] + alfa[1] * gradient[1] / NUM_OF_POINTS;
                    try {
                        double reduceStep =
                            BasicFunction.calculateExample(theta, X_EXAMPLE) -
                            BasicFunction.calculateExample(newTheta, X_EXAMPLE);
                        haveAccuracy = Math.abs(reduceStep);
                        if (reduceStep > 0) {
                            alfa[0] /= 2;
                            alfa[1] /= 2;
                        }
                        // If have accuracy is more than required one ->
                        // continue, otherwise finish and show the result
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
        //Main section (initializing and running algorithm)
        vertx.setTimer(1, res -> {
            // Create input data, 1000000 example points
            try {
                int result = calculatePoints(sharedData, NUM_OF_POINTS, parallel,
                    countOfSlaveVerticles);
                if (result != 0) {
                    log.error("Wrong count of points or verticles!");
                    vertx.close();
                }
            }
            catch (Exception ex) {
                log.error(ex);
                vertx.close();
            }
            // Check used time for computation
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
    * Calculates gradient descent
    * Master-verticle calculates this section in case non-parallel algorithm
    */
    private double[] gradientDescent() {
        // Initialize alfa, theta0 and theta1 ( y = theta0 + theta1 * x )
        // Alfa can change during algorithm
        alfa = new double[2];
        alfa[0] = ALFA_0;
        alfa[1] = ALFA_1;
        newTheta = new double[2];
        newTheta[0] = ASSUMED_THETA0;
        newTheta[1] = ASSUMED_THETA1;
        double[] theta = new double[2];
        double haveAccuracy; //It is used to compare results of function
        //Calculate theta0 and theta1 until have requirement accuracy
        do {
            // Save old values theta0 and theta1
            theta[0] = newTheta[0];
            theta[1] = newTheta[1];
            // Gradients for theta0 and theta1 (all points)
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
    * Prepares global data for message handler
    * Sends first set of messages for slave-verticles
    */
    private void parallelGradientDescent() {
        // Initialize alfa, theta0 and theta1 ( y = theta0 + theta1 * x )
        // Alfa can change during algorithm
        // Global variables for using in message handler
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
        // Start parallel gradient descent
        // Following actions of master-verticle are in the message handler
        // Waiting for messages from all slave-verticles
    }
    
    /*
    * Calculate gradient
    * Returns gradients for theta0 and theta1
    * Master-verticle computes this
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
    * Calculates points for input data size
    * Uses random to change real values
    */
    public static int calculatePoints(SharedData sharedData,
            int count, boolean parallel, int countOfSlaveVerticles) 
            throws NullPointerException {
        // check if size is incorrect or sharedData is null
        if (sharedData == null)
            throw new NullPointerException();
        if (count < 1)
            return -1;
        Random random = new Random();
        /* If algorithm will be parallel it will create shared map for all
        * slave-verticles, otherwise just one shared map
        */
        if (parallel == false) {
            LocalMap<String, Object> sharedMap =
                    sharedData.getLocalMap("sharedMap");
            sharedMap.put("count", count);
            for (int i = 0; i < count; i++) {
                double x = random.nextDouble()*5000.0;
                // Calculate y(x) and change it to not ideal value of function
                double y = BasicFunction.calculate(x) + 100.0*(random.nextDouble() - 0.5);
                sharedMap.put("x" + i, x);
                sharedMap.put("y" + i, y);
            }
            return 0;
        }
        else {
            if (countOfSlaveVerticles < 1 || countOfSlaveVerticles > 128)
                return -1;
            for (int i = 0; i < countOfSlaveVerticles; i++) {
                LocalMap<String, Object> map =
                    sharedData.getLocalMap("sharedMap" + i);
                int slavePointCount = count / countOfSlaveVerticles;
                map.put("count", slavePointCount);
                for (int j = 0; j < slavePointCount; j++) {
                    double x = random.nextDouble()*5000.0;
                    // Calculate y(x) and change it to not ideal value of function
                    double y = BasicFunction.calculate(x) + 100.0*(random.nextDouble() - 0.5);
                    map.put("x" + j, x);
                    map.put("y" + j, y);
                }
            }
            return 0;
        }
    }
    
    // Notice user when finished
    @Override
    public void stop() throws Exception {
        log.info("Master stopped!");
    }
  
}
