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
 * An {@link Iterator} (and, for convenience, an {@link Iterable})
 * over a {@link Throwable} that exposes its {@linkplain
 * Throwable#getCause() causal chain} as an iteration.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.0
 */
public final class ThrowableIterator implements Iterator<Throwable>, Iterable<Throwable> {

  /**
   * The {@link Throwable} to begin iterating over&mdash;also the
   * first element returned.  This field may be {@code null} at any
   * point and is mutated by iteration.
   */
  private Throwable throwable;

  /**
   * Creates a new {@link ThrowableIterator} that will begin iteration
   * with the supplied {@link Throwable}.
   *
   * @param t the {@link Throwable} whose identity and causal chain
   * will form the iteration; may be {@code null}
   */
  public ThrowableIterator(final Throwable t) {
    super();
    this.throwable = t;
  }
  
  /**
   * Returns this {@link ThrowableIterator} and thereby implements the
   * {@link Iterable} contract.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return this {@link ThrowableIterator}; never {@code null}
   */
  @Override
  public final Iterator<Throwable> iterator() {
    return this;
  }

  /**
   * Returns {@code true} if this {@link ThrowableIterator} has more elements.
   *
   * @return {@code true} if this {@link ThrowableIterator} hsa more
   * elements; {@code false} otherwise
   */
  @Override
  public final boolean hasNext() {
    return this.throwable != null;
  }

  /**
   * Returns the next {@link Throwable} reachable from this {@link ThrowableIterator}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the next {@link Throwable}; never {@code null}
   *
   * @exception NoSuchElementException if there are no more {@link
   * Throwable}s to return
   */
  @Override
  public final Throwable next() {
    final Throwable returnValue = this.throwable;
    if (returnValue == null) {
      throw new NoSuchElementException();
    }
    this.throwable = this.throwable.getCause();
    return returnValue;
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
  
}
