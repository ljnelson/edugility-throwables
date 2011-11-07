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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseThrowables {

  private Throwable first;

  private Throwable thirdToLast;

  private Throwable secondToLast;

  private Throwable last;

  public TestCaseThrowables() {
    super();
  }

  @Before
  public void setUp() {
    this.last = new NumberFormatException();
    this.secondToLast = new IllegalArgumentException(this.last);
    this.thirdToLast = new IllegalArgumentException(this.secondToLast);
    this.first = new Throwable(this.thirdToLast);
  }

  @Test
  public void testFirstInstance() {
    final NumberFormatException result = Throwables.firstInstance(this.first, NumberFormatException.class);
    assertSame(result, this.last);
  }

  @Test
  public void testLastInstance() {
    final IllegalArgumentException result = Throwables.lastInstance(this.first, IllegalArgumentException.class);
    assertSame(result, this.last);
  }

  @Test
  public void testGetPrimordialCause() {
    Throwable cause = Throwables.getPrimordialCause(this.first);
    assertSame(cause, this.last);
    cause = Throwables.getPrimordialCause(null);
    assertNull(cause);
  }

  @Test
  public void testToList() {
    List<Throwable> list = Throwables.toList(null);
    assertNotNull(list);
    assertTrue(list.isEmpty());
    list = Throwables.toList(this.first);
    assertNotNull(list);
    assertEquals(4, list.size());
    assertSame(this.first, list.get(0));
    assertSame(this.thirdToLast, list.get(1));
    assertSame(this.secondToLast, list.get(2));
    assertSame(this.last, list.get(3));
  }

  @Test
  public void testToListWithThrowableChain() {
    final ThrowableChain chain = new ThrowableChain();
    assertEquals(1, chain.size());
    final Exception cause = new Exception("cause");
    final Exception firstAffiliate = new Exception("firstAffiliate");
    chain.add(cause); // actually initializes cause, does not add to list
    assertEquals(1, chain.size());
    assertSame(cause, chain.getCause());
    chain.add(firstAffiliate);
    assertEquals(2, chain.size());
    final List<Throwable> list = Throwables.toList(chain);
    assertNotNull(list);
    assertEquals(3, list.size());
    assertSame(chain, list.get(0));
    assertSame(cause, list.get(1));
    assertSame(firstAffiliate, list.get(2));
  }

  @Test
  public void testWeird() {
    // TODO: test a throwable that is an Iterable<Throwable> that does
    // not contain itself
    final List<Throwable> list = Throwables.toList(new ThrowableCollection());
    assertNotNull(list);
    assertEquals(3, list.size());
  }

  private static final class ThrowableCollection extends Throwable implements Iterable<Throwable> {

    /**
     * A serial version identifier uniquely identifying the version of
     * this class.  See the <a
     * href="http://download.oracle.com/javase/6/docs/api/java/io/Serializable.html">documentation
     * for the {@code Serializable} class</a> for details.
     */
    private static final long serialVersionUID = 1L;

    private final List<Throwable> list;

    private ThrowableCollection() {
      super();
      this.list = new ArrayList<Throwable>();
      this.list.add(new Exception("first affiliate"));
      this.list.add(new Exception("second affiliate"));
    }

    @Override
    public Iterator<Throwable> iterator() {
      return this.list.iterator();
    }

  }

}