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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import com.edugility.throwables.Throwables.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseThrowablePattern {

  private ThrowablePattern pattern;

  public TestCaseThrowablePattern() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    this.pattern = new ThrowablePattern();
  }

  @Test
  public void testLeadingSlashAnchoredAtEndOnly() throws Exception {
    final String s = "/**/java.lang.RuntimeException";
    final ThrowableMatcher p = this.pattern.newThrowableMatcher(s);
    final RuntimeException last = new RuntimeException();
    final NamingException ne = (NamingException)new NamingException().initCause(last);
    final Exception first = new Exception(ne);
    assertNotNull(p);
    assertTrue(p.matches(first));
    assertTrue(p.matches(ne));
    assertTrue(p.matches(last));
  }

  @Test
  public void testAnchoredAtEndOnly() throws Exception {
    final String s = "**/java.lang.RuntimeException";
    final ThrowableMatcher p = this.pattern.newThrowableMatcher(s);
    final RuntimeException last = new RuntimeException();
    final NamingException ne = (NamingException)new NamingException().initCause(last);
    final Exception first = new Exception(ne);
    assertNotNull(p);
    assertTrue(p.matches(first));
    assertTrue(p.matches(ne));
    assertTrue(p.matches(last));
  }

  @Test
  public void testGreedyGlob() throws Exception {
    final String s = "java.io.FileNotFoundException.../**/java.io.IOException/java.lang.RuntimeException...";
    final ThrowableMatcher p = this.pattern.newThrowableMatcher(s);
    assertNotNull(p);
    final RuntimeException last = new IllegalArgumentException();
    final IOException nextToLast = new IOException(last);
    final IllegalStateException nextToNextToLast = new IllegalStateException(nextToLast);
    final FileNotFoundException first = (FileNotFoundException)new FileNotFoundException().initCause(nextToNextToLast);
    
    final List<Throwable> list = Throwables.toList(first);
    assertNotNull(list);
    
    assertTrue(p.matches(first));
  }

  @Test
  public void testStringWithComments() throws Exception {
    final String s = "java.sql.SQLException.../**/java.lang.Exception...(message==\"fred\") # This is a comment";
    final ThrowableMatcher p = this.pattern.newThrowableMatcher(s);
    assertNotNull(p);
    final Exception e = new Exception("fred");
    final RuntimeException penultimate = new RuntimeException(e);
    final NamingException namingException = (NamingException)new NamingException().initCause(penultimate);
    final SQLException exception = new SQLException(namingException);
    assertSame(e, penultimate.getCause());
    assertSame(penultimate, namingException.getCause());
    assertSame(namingException, exception.getCause());
    assertTrue(p.matches(exception));
  }

  @Test
  public void testEdgeCase1() throws Exception {
    final String s = "java.sql.SQLException/java.lang.IllegalStateException";
    final ThrowableMatcher p = this.pattern.newThrowableMatcher(s);
    assertNotNull(p);
    final IllegalStateException ise = new IllegalStateException();
    final SQLException sqlException = new SQLException(ise);
    assertTrue(p.matches(sqlException));
  }

  @Test
  public void testEdgeGlob() throws Exception {
    final String s = "java.lang.NullPointerException/   * /javax.naming.NamingException";
    final ThrowableMatcher p = this.pattern.newThrowableMatcher(s);
    assertNotNull(p);
    final NamingException ne = new NamingException();
    final IllegalStateException ise = new IllegalStateException(ne);
    final NullPointerException npe = (NullPointerException)new NullPointerException().initCause(ise);
    assertSame(ise, npe.getCause());
    assertSame(ne, ise.getCause());
    assertTrue(p.matches(npe));
  }

  @Test
  public void testSimpleClassNameEquality() throws Exception {
    final Throwable t = new NullPointerException();
    final ThrowableMatcher p = this.pattern.newThrowableMatcher("java.lang.NullPointerException");
    assertNotNull(p);
    assertTrue(p.matches(t));
  }

  @Test
  public void testSimplePropertyBlock() throws Exception {
    final ThrowableMatcher p = this.pattern.newThrowableMatcher("java.lang.NullPointerException( message == \"fred\" )");
    assertNotNull(p);
    assertTrue(p.matches(new NullPointerException("fred")));
  }

}