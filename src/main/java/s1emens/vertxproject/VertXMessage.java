/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

/**
 *
 * @author Cyberhawk
 * master-verticle can send this complex message to slave-verticle
 * contains points set, theta0 and theta1
 */
class VertXMessage {
    Point[] points;
    double[] theta;

    public VertXMessage(Point[] points, double[] theta) {
        this.points = points;
        this.theta = theta;
    }

    public Point[] getPoints() {
        return points;
    }

    public void setPoints(Point[] points) {
        this.points = points;
    }

    public double[] getTheta() {
        return theta;
    }

    public void setTheta(double[] theta) {
        this.theta = theta;
    }
    
}
