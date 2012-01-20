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

import java.util.List;

import javax.naming.NamingException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A test case that exercises the {@link ThrowablePattern} and {@link
 * ThrowableMatcher} classes.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.2-SNAPSHOT
 *
 * @see {@link ThrowableMatcher}
 *
 * @see {@link ThrowablePattern}
 */
public class TestCaseThrowablePattern {

  /**
   * Creates a new {@link TestCaseThrowablePattern}.
   */
  public TestCaseThrowablePattern() {
    super();
  }

  @Test
  public void testReferences() throws Exception {
    final String s = "java.lang.Exception/java.lang.IllegalStateException[1]/java.lang.RuntimeException";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);

    final RuntimeException re = new RuntimeException("bottom");
    final IllegalStateException ise = new IllegalStateException("middle", re);
    final Exception e = new Exception("top", ise);

    final ThrowableMatcher matcher = pattern.matcher(e);
    assertNotNull(matcher);
    final Throwable ref = matcher.getReference(Integer.valueOf(1));
    assertSame(ref, ise);
  }

  @Test
  public void testLeadingSlashAnchoredAtEndOnly() throws Exception {
    final String s = "/**/java.lang.RuntimeException";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);
    final RuntimeException last = new RuntimeException();
    final NamingException ne = (NamingException)new NamingException().initCause(last);
    final Exception first = new Exception(ne);

    final Throwable[] throwables = new Throwable[] { first, ne, last };
    for (final Throwable t : throwables) {
      final ThrowableMatcher m = pattern.matcher(t);
      assertNotNull(m);
      assertTrue(m.matches());
    }
  }

  @Test
  public void testAnchoredAtEndOnly() throws Exception {
    final String s = "**/java.lang.RuntimeException";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);
    final RuntimeException last = new RuntimeException();
    final NamingException ne = (NamingException)new NamingException().initCause(last);
    final Exception first = new Exception(ne);

    final Throwable[] throwables = new Throwable[] { first, ne, last };
    for (final Throwable t : throwables) {
      final ThrowableMatcher m = pattern.matcher(t);
      assertNotNull(m);
      assertTrue(m.matches());
    }
  }

  @Test
  public void testGreedyGlob() throws Exception {
    final String s = "java.io.FileNotFoundException.../**/java.io.IOException/java.lang.RuntimeException...";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);

    final RuntimeException last = new IllegalArgumentException();
    final IOException nextToLast = new IOException(last);
    final IllegalStateException nextToNextToLast = new IllegalStateException(nextToLast);
    final FileNotFoundException first = (FileNotFoundException)new FileNotFoundException().initCause(nextToNextToLast);

    final ThrowableMatcher matcher = pattern.matcher(first);
    assertNotNull(matcher);
    assertTrue(matcher.matches());

    final List<Throwable> list = Throwables.toList(first);
    assertNotNull(list);
  }

  @Test
  public void testStringWithComments() throws Exception {
    final String s = "java.sql.SQLException.../**/java.lang.Exception...(message==\"fred\") # This is a comment";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);

    final Exception e = new Exception("fred");
    final RuntimeException penultimate = new RuntimeException(e);
    final NamingException namingException = (NamingException)new NamingException().initCause(penultimate);
    final SQLException exception = new SQLException(namingException);

    assertSame(e, penultimate.getCause());
    assertSame(penultimate, namingException.getCause());
    assertSame(namingException, exception.getCause());

    final ThrowableMatcher p = pattern.matcher(exception);
    assertNotNull(p);
    assertTrue(p.matches());
  }

  @Test
  public void testEdgeCase1() throws Exception {
    final String s = "java.sql.SQLException /  java.lang.IllegalStateException";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);
    final IllegalStateException ise = new IllegalStateException();
    final SQLException sqlException = new SQLException(ise);
    final ThrowableMatcher matcher = pattern.matcher(sqlException);
    assertNotNull(matcher);
    assertTrue(matcher.matches());
  }

  @Test
  public void testGlobWithWhitespace() throws Exception {
    final String s = "java.lang.NullPointerException/   * /javax.naming.NamingException";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);
    final NamingException ne = new NamingException();
    final IllegalStateException ise = new IllegalStateException(ne);
    final NullPointerException npe = (NullPointerException)new NullPointerException().initCause(ise);

    assertSame(ise, npe.getCause());
    assertSame(ne, ise.getCause());

    final ThrowableMatcher matcher = pattern.matcher(npe);
    assertNotNull(matcher);
    assertTrue(matcher.matches());
  }

  @Test
  public void testSimpleClassNameEquality() throws Exception {
    final String s = "java.lang.NullPointerException";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);
    final ThrowableMatcher matcher = pattern.matcher(new NullPointerException());
    assertNotNull(matcher);
    assertTrue(matcher.matches());
  }

  @Test
  public void testSimplePropertyBlock() throws Exception {
    final String s = "java.lang.NullPointerException( message == \"fred\" )";
    final ThrowablePattern pattern = ThrowablePattern.compile(s);
    assertNotNull(pattern);
    final ThrowableMatcher matcher = pattern.matcher(new NullPointerException("fred"));
    assertNotNull(matcher);
    assertTrue(matcher.matches());
  }

}