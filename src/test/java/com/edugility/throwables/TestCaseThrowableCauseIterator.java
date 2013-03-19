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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class TestCaseThrowableCauseIterator {

  private ThrowableCauseIterator iterator;

  @Before
  public void setUp() {
    final Exception one = new Exception("one");
    final Exception two = new Exception("two", one);
    final Exception three = new Exception("three", two);
    assertSame(two, three.getCause());
    assertSame(one, two.getCause());
    assertNull(one.getCause());
    this.iterator = new ThrowableCauseIterator(three);
  }

  @Test
  public void testSize() {
    assertEquals(3, this.iterator.size());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testBadGetTooBig() {
    this.iterator.get(27);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testBadGetTooSmall() {
    this.iterator.get(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetEdge() {
    assertEquals(3, this.iterator.size());
    final Throwable bottom = this.iterator.get(2);
    assertNotNull(bottom);
    assertEquals("one", bottom.getMessage());
    this.iterator.get(3);
  }


  @Test
  public void testIteration() {
    int i = 0;
    final Iterator<Throwable> it = this.iterator.iterator();
    assertNotNull(it);

    while (it.hasNext()) {
      final Throwable next = it.next();
      assertNotNull(next);
      i++;
    }
    assertEquals(3, i);
  }

}
