/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

/**
 *
 * @author Cyberhawk
 * class contains static methods to calculate a values of functions
 */
public class BasicFunction {
    
    /*
    * ideal function y = 150 + 0,18 * x
    */
    public static double calculate(double x) {
        return 150 + 0.18 * x;
    }
    
    /*
    * function with parameters y = theta0 + theta1 * x
    */
    public static double calculateExample(double[] theta, double x) throws NullPointerException, IllegalArgumentException {
        if (theta == null)
            throw new NullPointerException();
        if (theta.length != 2)
            throw new IllegalArgumentException();
        return theta[0] + theta[1] * x;
    }
}
