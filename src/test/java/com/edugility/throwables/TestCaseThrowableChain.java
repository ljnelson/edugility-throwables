/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * Copyright (c) 2011 Edugility LLC.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.edugility.throwables;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class TestCaseThrowableChain {

  private ThrowableChain chain;

  private Exception expectedCause;
  
  @Before
  public void setUp() throws Exception {
    this.chain = new ThrowableChain();
    this.expectedCause = new Exception("1");
    this.chain.add(this.expectedCause);
    assertSame(this.expectedCause, this.chain.getCause());
    this.chain.add(new Exception("2"));
    this.chain.add(new Exception("3"));
  }

  @Test
  public void testCause() {
    assertNotNull(this.chain.getCause());
    assertEquals("1", this.chain.getCause().getMessage());
    assertEquals(3, this.chain.size());
  }

  @Test
  public void testIteration() {
    final Iterator<Throwable> i = this.chain.iterator();
    assertNotNull(i);
    assertTrue(i.hasNext());
    Throwable t = i.next();

    // The first item in the iteration is the ThrowableChain itself.
    assertSame(this.chain, t);

    assertTrue(i.hasNext());
    t = i.next();
    assertNotNull(t);

    // The next item is NOT the first exception added (that becomes
    // the ThrowableChain's cause).  It is instead the SECOND
    // exception added.
    assertEquals("2", t.getMessage());

    t = i.next();
    assertNotNull(t);

    // From that point forward the iteration proceeds normally.
    assertEquals("3", t.getMessage());
  }

  @Test
  public void testOddCauseAndAddSituations() throws Exception {
    ThrowableChain chain = new ThrowableChain();
    assertEquals(1, chain.size());
    assertTrue(chain.getCause() == null);

    // Initializing a chain's cause does not actually add that cause
    // to the list or alter the chain's size.
    final Exception cause = new Exception("cause");
    chain.initCause(cause);
    assertSame(cause, chain.getCause());
    assertEquals(1, chain.size());
    assertFalse(chain.asList().contains(cause));
    
    // Removal has no effect on the cause.
    assertFalse(chain.remove(cause));
    assertEquals(1, chain.size());
    assertSame(cause, chain.getCause());

    chain = new ThrowableChain();
    
    // Adding the first item initializes the cause but does not affect
    // the size/contents of the chain's affiliates.
    assertFalse(chain.add(cause));
    assertEquals(1, chain.size());
    assertFalse(chain.asList().contains(cause));

    // Adding subsequent items affects the list but does not affect
    // the cause.
    final Exception affiliate = new Exception("affiliate");
    chain.add(affiliate);
    assertSame(cause, chain.getCause());
    assertEquals(2, chain.size());
    assertFalse(chain.asList().contains(cause));

  }
  

}