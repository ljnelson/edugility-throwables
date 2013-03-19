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
import java.util.Collection;

/**
 * An {@link AbstractList} formed by a {@link Throwable} and its
 * {@linkplain Throwable#getCause() causal chain}.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public final class ThrowableList extends AbstractList<Throwable> {
  

  /*
   * Note to self and future maintainers: Please do not get clever and
   * make this class extend Exception.
   */


  /**
   * The root {@link Throwable} and therefore the first item in this
   * {@link ThrowableList}.
   *
   * <p>This field may be {@code null}.</p>
   */
  private final Throwable t;

  /**
   * Creates a new {@link ThrowableList}.  The first element of this
   * {@link ThrowableList}'s iteration will be the supplied {@link
   * Throwable}, followed by its {@linkplain Throwable#getCause()
   * cause}, and its cause's cause, and so on.
   *
   * @param t the {@link Throwable} to iterate over; may be {@code
   * null}
   */
  public ThrowableList(final Throwable t) {
    super();
    this.t = t;
  }

  /**
   * Returns the {@link Throwable} at the supplied {@code index}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param index the index at which a {@link Throwable} will be
   * found; must be greater than or equal to {@code 0} and less than
   * the return value of the {@link #size()} method
   *
   * @return a non-{@code nul} {@link Throwable}
   *
   * @exception IndexOutOfBoundsException if {@code index} is less
   * than {@code 0} or greater than or equal to the return value of
   * the {@link #size()} method
   */
  @Override
  public Throwable get(final int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException(String.format("index < 0: %d", index));
    }
    Throwable t = this.t;
    for (int i = 0; i < index && t != null; i++) {
      t = t.getCause();
    }
    if (t == null) {
      throw new IndexOutOfBoundsException(String.format("index >= size(): %d; size: %d", index, this.size()));
    }
    return t;
  }

  /**
   * Returns {@code true} if this {@link ThrowableList} is empty.
   *
   * @return {@code true} if this {@link ThrowableList} is empty
   */
  @Override
  public final boolean isEmpty() {
    return this.t == null;
  }

  /**
   * Returns the size of this {@link ThrowableList}.
   *
   * @return an {@code int} greater than or equal to {@code 0}
   * representing the size of this {@link ThrowableList}
   */
  @Override
  public final int size() {
    int size = 0;
    Throwable t = this.t;
    while (t != null) {
      size++;
      t = t.getCause();
    }
    return size;
  }

  /**
   * Returns the {@link String} representation of the {@link
   * Throwable} that was supplied to this {@link ThrowableList}'s
   * {@linkplain #ThrowableList(Throwable) constructor}, or "{@code
   * null}" if the supplied {@link Throwable} was {@code null}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return a {@link String} representation of this {@link
   * ThrowableList}; never {@code null}
   */
  @Override
  public String toString() {
    return this.t == null ? "null" : this.t.toString();
  }


  /*
   * Unsupported methods.
   */


  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @param throwable a {@link Throwable} that is ignored; may be
   * {@code null}
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  public final boolean add(final Throwable throwable) {
    throw new UnsupportedOperationException("add");
  }

  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @param index an {@code int} that is ingored
   *
   * @param throwable a {@link Throwable} that is ignored; may be
   * {@code null}
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  public final void add(final int index, final Throwable throwable) {
    throw new UnsupportedOperationException("add");
  }

  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @param index an {@code int} that is ingored
   *
   * @param stuff a {@link Collection} that is ignored; may be {@code
   * null}
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  public final boolean addAll(final int index, final Collection<? extends Throwable> stuff) {
    throw new UnsupportedOperationException("addAll");
  }

  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  public final void clear() {
    throw new UnsupportedOperationException("clear");
  }

  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @param index an {@code int} that is ingored
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  public final Throwable remove(final int index) {
    throw new UnsupportedOperationException("remove");
  }

  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @param index1 an {@code int} that is ingored
   *
   * @param index2 an {@code int} that is ingored
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  protected void removeRange(final int index1, final int index2) {
    throw new UnsupportedOperationException("removeRange");
  }

  /**
   * Throws {@link UnsupportedOperationException} when invoked.
   *
   * @param index an {@code int} that is ingored
   *
   * @param throwable a {@link Throwable} that is ignored; may be
   * {@code null}
   *
   * @exception UnsupportedOperationException when invoked
   */
  @Override
  public final Throwable set(final int index, final Throwable throwable) {
    throw new UnsupportedOperationException("set");
  }  

}
