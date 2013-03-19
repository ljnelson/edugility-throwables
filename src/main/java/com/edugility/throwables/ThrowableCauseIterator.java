/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2011-2013 Edugility LLC.
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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@link Iterator} that iterates over a {@link Throwable}'s
 * <i>causal chain</i>.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public final class ThrowableCauseIterator extends AbstractList<Throwable> {
  
  /*
   * Note to self and future maintainers: Please do not get clever and
   * make this class extend Exception.
   */

  /**
   * The {@link Throwable} to iterate over.  This field may be {@code
   * null}.
   *
   * @see #next()
   */
  private Throwable t;

  /**
   * Creates a new {@link ThrowableCauseIterator}.  The first element
   * of this {@link ThrowableCauseIterator}'s iteration will be the
   * supplied {@link Throwable}, followed by its {@linkplain
   * Throwable#getCause() cause}, and its cause's cause, and so on.
   *
   * @param t the {@link Throwable} to iterate over; may be {@code
   * null}
   */
  public ThrowableCauseIterator(final Throwable t) {
    super();
    this.t = t;
  }

  /**
   * Returns the {@link String} representation of the {@link
   * Throwable} that was supplied to this {@link
   * ThrowableCauseIterator}'s {@linkplain
   * #ThrowableCauseIterator(Throwable) constructor}, or "{@code
   * null}" if the supplied {@link Throwable} was {@code null}.
   *
   * @return a {@link String} representation of this {@link
   * ThrowableCauseIterator}; never {@code null}
   */
  @Override
  public String toString() {
    return this.t == null ? "null" : this.t.toString();
  }

  @Override
  public int size() {
    int size = 0;
    Throwable t = this.t;
    while (t != null) {
      size++;
      t = t.getCause();
    }
    return size;
  }

  @Override
  public Throwable get(final int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException();
    }
    Throwable t = this.t;
    for (int i = 0; i < index && t != null; i++) {
      t = t.getCause();
    }
    if (t == null) {
      throw new IndexOutOfBoundsException("index: " + index + "; size: " + this.size());
    }
    return t;
  }

}
