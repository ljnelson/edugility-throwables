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

/**
 * A {@link Throwable} that is also holds a modifiable list of other
 * {@link Throwable}s that are not connected to the direct {@linkplain
 * Throwable#getCause() causation chain}, but are affiliated
 * nonetheless.  Instances of this class are particularly useful when
 * dealing with {@link Throwable}s in {@code finally} blocks.
 *
 * <p>A {@link ThrowableChain} always contains itself, so the return
 * value of its {@link #size()} method is always at least {@code
 * 1}.</p>
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.0
 */
public class ThrowableChain extends Throwable implements Iterable<Throwable> {

  /**
   * The {@link List} containing additional {@link Throwable}s.  This
   * field is never {@code null}.
   */
  private final List<Throwable> list;

  /**
   * Creates a new {@link ThrowableChain}.
   */
  public ThrowableChain() {
    this(null);
  }

  /**
   * Creates a new {@link ThrowableChain} with the supplied {@code
   * message}.
   *
   * @param message the message; may be {@code null}
   */
  public ThrowableChain(final String message) {
    super(message);
    this.list = new ArrayList<Throwable>(11);
    this.add(this);
  }

  /**
   * Adds the supplied {@link Throwable} to this {@link
   * ThrowableChain} if it is non-{@code null}.  The supplied {@link
   * Throwable}'s {@linkplain Throwable#getCause() cause} is not
   * added.
   *
   * @param throwable the {@link Throwable} to add; may be {@code
   * null} in which case no action will be taken
   */
  public final boolean add(final Throwable throwable) {
    boolean returnValue = false;
    if (throwable != null) {
      returnValue = this.list.add(throwable);
      // We deliberately do not add his cause
    }
    return returnValue;
  }

  /**
   * Returns the size of this {@link ThrowableChain}.  A {@link
   * ThrowableChain} always has a size of at least {@code 1}.
   *
   * @return the size of this {@link ThrowableChain}&mdash;a positive
   * integer greater than or equal to {@code 1}
   */
  public final int size() {
    return this.list.size();
  }

  /**
   * Returns an {@link Iterator} that can be used to iterate over all
   * {@linkplain #add(Throwable) contained <tt>Throwable</tt>s}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return an {@link Iterator}; never {@code null}; the first
   * element of the {@link Iterator} will always be this {@link
   * ThrowableChain}
   */
  @Override
  public final Iterator<Throwable> iterator() {
    return this.list.iterator();
  }


}