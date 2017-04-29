/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
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
    private Logger log = LoggerFactory.getLogger(Master.class);
    private static final int NUM_OF_POINTS = 1000000;
    private static final double ACCURACY = 1e-4; // required accuracy to stop
    private static final double X_EXAMPLE = 1000;
    // example value for comparison function results
    private static final double ASSUMED_THETA0 = 1; // real value is 150
    private static final double ASSUMED_THETA1 = -50; // real value is 0,18
    private static final double ALFA_0 = 1e-2; // step of algorithm for theta0
    private static final double ALFA_1 = 1e-6; // step of algorithm for theta1
    private static LocalMap<String, Object> sharedMap;
    
    @Override
    public void start() throws Exception {
        // main calculation
        vertx.deployVerticle(new Slave());
        SharedData sharedData = vertx.sharedData();
        sharedMap = sharedData.getLocalMap("sharedMap");
        vertx.setTimer(10, res -> {
            //vertx.eventBus().send("slave", "Heeeeeeeeey!!!!!");
            //MessageConsumer consumer = vertx.eventBus().consumer("master");
            /*consumer.bodyStream().handler(answer -> {
                log.info(answer);
            });*/
            // create input data, 1000000 example points
            calculatePoints(NUM_OF_POINTS);
            vertx.eventBus().send("slave", "Heeeeeeeeey!!!!!");
            // check used time for computation
            LocalTime start = LocalTime.now();
            // initialize alfa, theta0 and theta1 ( y = theta0 + theta1 * x )
            // alfa can change during algorithm
            /*double[] alfa = new double[2];
            alfa[0] = ALFA_0;
            alfa[1] = ALFA_1;
            double[] newTheta = new double[2];
            newTheta[0] = ASSUMED_THETA0;
            newTheta[1] = ASSUMED_THETA1;
            double[] results;
            try {
            results = gradientDescent(points, newTheta, alfa, true);
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
            LocalTime finish = LocalTime.now();
            long time = ChronoUnit.MILLIS.between(start, finish);
            log.info(String.format("(Time %d ms): y = %.4f * x + %.4f%n", time, results[0], results[1]));*/
            LocalTime finish = LocalTime.now();
            long time = ChronoUnit.MILLIS.between(start, finish);
            log.info(String.format("(Time %d ms)%n", time));
            vertx.close();
        });
        // vertx.registerHandler() :)
    }
    
    /*
    * calculates points for input data size
    * uses random to change real values
    */
    private double[] gradientDescent(Point[] points, double[] newTheta,
            double[] alfa, boolean parallel)
            throws NullPointerException, IllegalArgumentException {
        if (points == null || newTheta == null || alfa == null)
            throw new NullPointerException();
        if (points.length < 1 || newTheta.length != 2 || alfa.length != 2)
            throw new IllegalArgumentException();
        // initialize alfa, theta0 and theta1 ( y = theta0 + theta1 * x )
        // alfa can change during algorithm
        double[] theta = new double[2];
        double haveAccuracy; //it is used to compare results of function
        //calculate theta0 and theta1 until have requirement accuracy
        do {
            // save old values theta0 and theta1
            theta[0] = newTheta[0];
            theta[1] = newTheta[1];
            // gradients for theta0 and theta1 (all points)
            double[] gradient;
            if (parallel == true)
                gradient = calculateParallelGradient(points, theta);
            else gradient = calculateGradient(points, theta);
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
    * calculate gradient
    * return gradients for theta0 and theta1
    * master-verticle computes this
    */
    private double[] calculateGradient(Point[] points, double[] theta) 
            throws NullPointerException, IllegalArgumentException {
        if (points == null || theta == null)
            throw new NullPointerException();
        if (points.length < 1 || theta.length != 2)
            throw new IllegalArgumentException();
        double[] gradient =  new double[2];
        gradient[0] = 0;
        gradient[1] = 0;
        // gradients for theta0 and theta1 (all points)
        for (int i = 0; i < NUM_OF_POINTS; i++)
        {
            double tmp = points[i].getY() - BasicFunction.calculateExample(theta, points[i].getX());
            gradient[0] += tmp;
            gradient[1] += tmp * points[i].getX();
        }
        return gradient;
    }
    
    /*
    * calculate gradient
    * return gradients for theta0 and theta1
    * master-verticle send messages to slave-verticles
    * slave-verticles compute sum of point set
    */
    private double[] calculateParallelGradient(Point[] points, double[] theta) 
            throws NullPointerException, IllegalArgumentException {
        if (points == null || theta == null)
            throw new NullPointerException();
        if (points.length < 1 || theta.length != 2)
            throw new IllegalArgumentException();
        double[] gradient =  new double[2];
        gradient[0] = 0;
        gradient[1] = 0;
        // gradients for theta0 and theta1 (all points)
        //vertx.deployVerticle(new Slave());
        /*vertx.executeBlocking(future -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendInt(NUM_OF_POINTS);
        for (int i = 0; i < NUM_OF_POINTS; i++)
        {
            buffer.appendDouble(points[i].getX());
            buffer.appendDouble(points[i].getY());
        }
        buffer.appendDouble(theta[0]);
        buffer.appendDouble(theta[1]);
        vertx.eventBus().send("slave", buffer, answer -> {
            Buffer answerBuffer = (Buffer)answer.result().body();
            gradient[0] += answerBuffer.getDouble(0);
            gradient[1] += answerBuffer.getDouble(1);
            log.info(gradient[0] + ": " + gradient[1]);
        });
        }, null);*/
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
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            double x = random.nextDouble()*5000;
            // calculate y(x) and change it to not ideal value of function
            double y = BasicFunction.calculate(x) + 100*(random.nextDouble() - 0.5);
            sharedMap.put("x" + i, x);
            sharedMap.put("y" + i, y);
            if (i == 0)
                log.info(x + ": " + y);
        }
        
    }
    
    @Override
    public void stop() throws Exception {
        log.info("Master stopped!");
    }
  
}
