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
        double expResult = 0.18*x + 150;
        double result = BasicFunction.calculate(x);
        assertEquals(expResult, result, 1e-6);
        x = -1000.0;
        expResult = 0.18*x + 150;
        result = BasicFunction.calculate(x);
        assertEquals(expResult, result, 1e-6);
        x = 1000000.0;
        expResult = 0.18*x + 150;
        result = BasicFunction.calculate(x);
        assertEquals(expResult, result, 1e-6);
    }

    /**
     * Test of calculateExample method, of class BasicFunction.
     */
    @Test(expected = NullPointerException.class)
    public void testCalculateExample() {
        double[] theta = null;
        double x = 100.0;
        BasicFunction.calculateExample(theta, x);
        fail("No expected exception");
    }
    
}
