/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * Copyright (c) 2011-2012 Edugility LLC.
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

import java.io.FileNotFoundException;
import java.io.IOException;

import java.sql.SQLException;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.HashSet;

import javax.naming.NamingException;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseThrowableFinders {

  public TestCaseThrowableFinders() {
    super();
  }

  @Test
  public void testCausalChain() throws Exception {
    final ClassNameMatchingThrowableFinder f1 = new ClassNameMatchingThrowableFinder("java.lang.Exception");
    final ClassNameMatchingThrowableFinder f2 = new ClassNameMatchingThrowableFinder("java.lang.RuntimeException");
    final CausalChainMatchingThrowableFinder tf = new CausalChainMatchingThrowableFinder(f1, f2);
    final RuntimeException bottom = new RuntimeException("bottom");
    final Exception middle = new Exception("middle", bottom);
    final Exception top = new Exception("top", middle);
    tf.setThrowable(top);
    assertFalse(tf.find());
    tf.setThrowable(middle);
    assertTrue(tf.find());
    assertSame(middle, tf.getFound());
    tf.clear();
    final FirstMatchingThrowableFinder firstMatching = new FirstMatchingThrowableFinder(tf);
    firstMatching.setThrowable(top);
    assertTrue(firstMatching.find());
    assertSame(middle, firstMatching.getFound());
    firstMatching.clear();
    final LastMatchingThrowableFinder lastMatching = new LastMatchingThrowableFinder(tf);
    lastMatching.setThrowable(top);
    assertTrue(lastMatching.find());
    assertSame(middle, lastMatching.getFound());
    final RuntimeException bigTop = new RuntimeException("bigTop", top);
    final Exception superTop = new Exception("superTop", bigTop);
    firstMatching.setThrowable(superTop);
    assertTrue(firstMatching.find());
    assertSame(superTop, firstMatching.getFound());
    lastMatching.setThrowable(superTop);
    assertTrue(lastMatching.find());
    assertSame(middle, lastMatching.getFound());
  }

  @Test
  public void testDisjunctive() throws Exception {
    final ClassNameMatchingThrowableFinder f1 = new ClassNameMatchingThrowableFinder("java.lang.Exception");
    final ClassNameMatchingThrowableFinder f2 = new ClassNameMatchingThrowableFinder("java.lang.RuntimeException");
    final DisjunctiveThrowableFinder d = new DisjunctiveThrowableFinder(f1, f2);
    final Exception e = new Exception("e");
    final RuntimeException re = new RuntimeException("re");
    d.setThrowable(e);
    assertTrue(d.find());
    assertSame(e, d.getFound());
    d.setThrowable(re);
    d.clear();
    assertTrue(d.find());
    assertSame(re, d.getFound());
  }

  @Test
  public void testFirstMatching() throws Exception {
    final ClassNameMatchingThrowableFinder f1 = new ClassNameMatchingThrowableFinder("java.lang.Exception");
    final FirstMatchingThrowableFinder f2 = new FirstMatchingThrowableFinder(f1);
    final Exception e = new Exception("bottom");
    final RuntimeException re = new RuntimeException("middle", e);
    final IllegalArgumentException iae = new IllegalArgumentException("top", re);
    f2.setThrowable(iae);
    assertTrue(f2.find());
    assertEquals(2, f2.start());
    assertEquals(3, f2.end());
    assertSame(e, f2.getFound());
  }

  @Test
  public void testLastMatching() throws Exception {
    final ClassNameMatchingThrowableFinder f1 = new ClassNameMatchingThrowableFinder("java.lang.Exception");
    final LastMatchingThrowableFinder f2 = new LastMatchingThrowableFinder(f1);
    final Exception e = new Exception("bottom");
    final RuntimeException re = new RuntimeException("middle", e);
    final IllegalArgumentException iae = new IllegalArgumentException("top", re);
    f2.setThrowable(iae);
    assertTrue(f2.find());
    assertSame(e, f2.getFound());
    f2.setThrowable(e);
    assertTrue(f2.find());
    assertSame(e, f2.getFound());
  }

  @Test
  public void testRootCauseMatching() throws Exception {
    final ClassNameMatchingThrowableFinder f1 = new ClassNameMatchingThrowableFinder("java.lang.Exception");
    final RootCauseMatchingThrowableFinder f2 = new RootCauseMatchingThrowableFinder(f1);
    final Exception e = new Exception("bottom");
    final RuntimeException re = new RuntimeException("middle", e);
    final IllegalArgumentException iae = new IllegalArgumentException("top", re);
    f2.setThrowable(iae);
    assertTrue(f2.find());
    assertSame(e, f2.getFound());
    f2.setThrowable(e);
    assertTrue(f2.find());
    assertSame(e, f2.getFound());
  }

}