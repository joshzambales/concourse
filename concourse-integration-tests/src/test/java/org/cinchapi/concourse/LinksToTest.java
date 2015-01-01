/*
 * The MIT License (MIT)
 * 
 * 2013-2015 Jeff Nelson, Cinchapi Software Collective
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cinchapi.concourse;

import org.cinchapi.concourse.thrift.Operator;
import org.cinchapi.concourse.util.TestData;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to make sure that {@link Operator#LINKS_TO} works.
 * 
 * @author jnelson
 */
public class LinksToTest extends ConcourseIntegrationTest {
    
    @Test
    public void testNoLinksButLong(){
        long value = TestData.getLong();
        client.add("foo", value, 1);
        Assert.assertFalse(client.find("foo", Operator.LINKS_TO, value).contains(1L));    
    }
    
    @Test
    public void testLinkAndLong(){
        long value = TestData.getLong();
        while(value == 1){
            value = TestData.getLong();
        }
        client.add("foo", value, 1);
        client.link("foo", 1, value);
        Assert.assertTrue(client.find("foo", Operator.LINKS_TO, value).contains(1L));  
    }
    
    @Test
    public void testLinkAndNoLong(){
        long value = TestData.getLong();
        client.link("foo", 1, value);
        Assert.assertTrue(client.find("foo", Operator.LINKS_TO, value).contains(1L));
    }

}
