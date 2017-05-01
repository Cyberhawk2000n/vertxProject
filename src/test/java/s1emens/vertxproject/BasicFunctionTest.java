/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package s1emens.vertxproject;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Cyberhawk
 */
public class BasicFunctionTest {
    
    public BasicFunctionTest() {
    }

    /**
     * Test of calculate method, of class BasicFunction.
     */
    @Test
    public void testCalculateNormalValues() {
        double x = 0.0;
        double expResult = 0.18*x + 150.0;
        double result = BasicFunction.calculate(x);
        assertEquals(expResult, result, 1.0e-6);
        double x2 = -1000.83;
        double expResult2 = 0.18*x2 + 150.0;
        double result2 = BasicFunction.calculate(x2);
        assertEquals(expResult2, result2, 1.0e-6);
        double x3 = 1000000.15;
        double expResult3 = 0.18*x3 + 150.0;
        double result3 = BasicFunction.calculate(x3);
        assertEquals(expResult3, result3, 1.0e-6);
    }
    
    /**
     * Test of calculate method, of class BasicFunction.
     * Test special value: NaN
     */
    @Test
    public void testCalculateNormalValuesXIsNaN() {
        double x = Double.NaN;
        double expResult = 0.18*x + 150.0;
        double result = BasicFunction.calculate(x);
        assertEquals(expResult, result, 1.0e-6);
    }
    
    /**
     * Test of calculate method, of class BasicFunction.
     * Test special values: +Infinity -Infinity
     */
    @Test
    public void testCalculateNormalValuesXIsInfinity() {
        double x = Double.NEGATIVE_INFINITY;
        double expResult = 0.18*x + 150.0;
        double result = BasicFunction.calculate(x);
        assertEquals(expResult, result, 1.0e-6);
        double x2 = Double.POSITIVE_INFINITY;
        double expResult2 = 0.18*x2 + 150.0;
        double result2 = BasicFunction.calculate(x2);
        assertEquals(expResult2, result2, 1.0e-6);
    }

    /**
     * Test of calculateExample method, of class BasicFunction.
     * Test normal behavior
     */
    @Test
    public void testCalculateExample() {
        double[] theta = new double[2];
        theta[0] = 3.0;
        theta[1] = -10.0;
        double x = 100.0;
        double expResult = theta[1]*x + theta[0];
        double result = BasicFunction.calculateExample(theta, x);
        assertEquals(expResult, result, 1.0e-6);
        double[] theta2 = new double[2];
        theta2[0] = -5000.4;
        theta2[1] = 100.31;
        double x2 = -1000.0;
        double expResult2 = theta2[1]*x2 + theta2[0];
        double result2 = BasicFunction.calculateExample(theta2, x2);
        assertEquals(expResult2, result2, 1.0e-6);
    }
    
    /**
     * Test of calculateExample method, of class BasicFunction.
     * Test throwing NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testCalculateExampleNullPointerException() {
        double[] theta = null;
        double x = 100.0;
        BasicFunction.calculateExample(theta, x);
        fail("No expected exception");
    }
    
    /**
     * Test of calculateExample method, of class BasicFunction.
     * Test throwing NullPointerException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateExampleIllegalArgumentException() {
        double[] theta = new double[10];
        double x = 100.0;
        BasicFunction.calculateExample(theta, x);
        fail("No expected exception");
    }
    
}
