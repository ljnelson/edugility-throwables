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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import java.net.URL;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseThrowableMessageKeySelector {

  private Reader reader;

  public TestCaseThrowableMessageKeySelector() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    final URL catalogURL = this.getClass().getResource("/MessageKeys.mkc");
    assertNotNull(catalogURL);
    this.reader = new InputStreamReader(catalogURL.openStream(), "UTF-8");
  }

  @After
  public void tearDown() throws Exception {
    this.reader.close();
  }

  @Test
  public void testRead() throws Exception {
    assertNotNull(this.reader);
    final ThrowableMessageFactory selector = ThrowableMessageFactory.read(this.reader);
    assertNotNull(selector);
    final IllegalStateException bottom = new IllegalStateException("hello");
    final RuntimeException top = new RuntimeException(bottom);
    assertEquals("com.edugility.throwables.Bundle#noDice", selector.getKey(top, "Default value"));
    assertEquals("No dice, senor.", selector.getMessage(top, "Default value"));
    assertEquals("This is the default message.", selector.getKey(bottom, "Default value"));
  }

  @Test
  public void testRead2() throws Exception {
    assertNotNull(this.reader);
    final ThrowableMessageKeySelector selector = ThrowableMessageFactory.read(this.reader);
    assertNotNull(selector);
    final Exception bottom = new Exception("bottom");
    final IOException middle = new IOException(bottom);
    final SQLException top = new SQLException(middle);
    final String ls = System.getProperty("line.separator", "\n");
    assertEquals("A key" + ls + "goes here", selector.getKey(top, "Default"));
  }

  @Test
  public void testGetMessage() throws Exception {
    assertNotNull(this.reader);
    final ThrowableMessageFactory factory = ThrowableMessageFactory.read(this.reader);
    assertNotNull(factory);
    final Exception bottom = new Exception("bottom");
    final IOException middle = new IOException("middle", bottom);
    final SQLException top = new SQLException("top", middle);
    final String message = factory.getMessage(top, "Default");
  }

}