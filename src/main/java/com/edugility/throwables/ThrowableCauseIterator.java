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
import java.util.NoSuchElementException;

/**
 * An {@link Iterator} that iterates over a {@link Throwable}'s
 * <i>causal chain</i>.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.0-SNAPSHOT
 */
public final class ThrowableCauseIterator implements Iterable<Throwable>, Iterator<Throwable> {
  
  /**
   * The {@link Throwable} to iterate over.  This field may be {@code
   * null}.
   *
   * @see #next()
   */
  private Throwable t;

  /**
   * Indicates whether iteration has started.  This field is {@code
   * true} only before the first call to the {@link #next()} method.
   */
  private boolean notYetStarted;

  /**
   * Creates a new {@link ThrowableCauseIterator}.  The first element
   * of this {@link ThrowableCauseIterator}'s iteration will be the
   * supplied {@link Throwable}, followed by its {@linkplain
   * Throwable#getCause() cause}, and its cause's cause, and so on.
   *
   * @param t the {@link {@link Throwable} to iterate over; may be
   * {@code null}
   */
  public ThrowableCauseIterator(final Throwable t) {
    super();
    this.notYetStarted = true;
    this.t = t;
  }

  /**
   * Returns this {@link ThrowableCauseIterator}.
   *
   * @return this {@link ThrowableCauseIterator}
   */
  @Override
  public final Iterator<Throwable> iterator() {
    return this;
  }

  /**
   * Returns {@code true} if there is another {@link Throwable} to
   * return from the {@link #next()} method.
   *
   * @return {@code true} if there is another {@link Throwable} to
   * return from the {@link #next()} method
   */
  @Override
  public final boolean hasNext() {
    return this.t != null && (this.notYetStarted || this.t.getCause() != null);
  }

  /**
   * Returns the next {@link Throwable} in the causal chain.  This
   * method never returns {@code null}.
   *
   * @return the next {@link Throwable} in the causal chain; never
   * {@code null}
   *
   * @exception NoSuchElementException if there are no more elements
   */
  @Override
  public final Throwable next() {
    if (this.notYetStarted) {
      this.notYetStarted = false;
    } else if (this.t != null) {
      this.t = this.t.getCause();
    }
    if (this.t == null) {
      throw new NoSuchElementException();
    }
    return this.t;
  }

  /**
   * Throws an {@link UnsupportedOperationException}.
   *
   * @exception UnsupportedOperationException if invoked
   */
  @Override
  public final void remove() {
    throw new UnsupportedOperationException("remove");
  }

  /**
   * Returns the {@link String} representation of the {@link
   * Throwable} that was supplied to this {@link
   * ThrowableCauseIterator}'s {@linkplain
   * #ThrowableCauseIterator(Throwable) constructor}, or "{@code null"
   * if the supplied {@link Throwable} was {@code null}.
   *
   * @return a {@link String} representation of this {@link
   * ThrowableCauseIterator}; never {@code null}
   */
  @Override
  public final String toString() {
    return String.valueOf(this.t);
  }

}