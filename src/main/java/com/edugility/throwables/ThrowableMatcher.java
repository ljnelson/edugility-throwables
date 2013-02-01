/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2012-2012 Edugility LLC.
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

import java.io.Serializable;

/**
 * A predicate interface whose implementations can match a {@link
 * Throwable}.  {@link ThrowableMatcher} instances are most commonly
 * returned by the {@link ThrowablePattern#matcher(Throwable)} method.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @see ThrowablePattern#matcher(Throwable)
 *
 * @see ThrowablePattern#compile(String)
 *
 * @since 1.2-SNAPSHOT
 */
public interface ThrowableMatcher extends Serializable {

  /**
   * Returns {@code true} if this {@link ThrowableMatcher}
   * implementation's {@linkplain #getThrowable() associated
   * <tt>Throwable</tt>} matches the conditions represented by this
   * {@link ThrowableMatcher} in some way.
   *
   * @return {@code true} if this {@link ThrowableMatcher} matches its
   * {@linkplain #getThrowable() associated <tt>Throwable</tt>};
   * {@code false} otherwise
   *
   * @exception ThrowableMatcherException if there was an error in
   * applying the matching operation
   */
  public boolean matches() throws ThrowableMatcherException;

  /**
   * Sets the {@link Throwable} chain to be tested by the {@link
   * #matches()} method.
   *
   * @param throwable the {@link Throwable} to be tested; may be
   * {@code null}
   */
  public void setThrowable(final Throwable throwable);

  /**
   * Returns the {@link Throwable} currently affiliated with this
   * {@link ThrowableMatcher}.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return the {@link Throwable} currently affiliated with this
   * {@link ThrowableMatcher}, or {@code null}
   */
  public Throwable getThrowable();

  /**
   * Returns the {@link String} form of the notional pattern that was
   * used to construct this {@link ThrowableMatcher}.  Implementations
   * are permitted to return {@code null}.
   *
   * @return the {@link String} form of the notional pattern that was
   * used to construct this {@link ThrowableMatcher}, or {@code null}
   */
  public String getPattern();

  /**
   * Returns any reference that was marked in the original {@link
   * ThrowablePattern} that produced this {@link ThrowableMatcher}.
   *
   * <p>Implementations of this method are permitted to return {@code
   * null}.</p>
   *
   * @param key the {@link Object} key under which a {@link Throwable}
   * reference is expected to be found; may be {@code null} in which
   * case {@code null} should be returned
   *
   * @return the {@link Throwable} stored under the supplied {@link
   * Object}, or {@code null} if there is no such {@link Throwable}
   */
  public Throwable getReference(final Object key);

  /**
   * Returns an {@link Iterable} of all keys known to this {@link
   * ThrowableMatcher} that can be used as inputs to its {@link
   * #getReference(Object)} method.  Implementations of this method
   * must not return {@code null}.
   *
   * @return a non-{@code null} {@link Iterable} of known keys
   */
  public Iterable<Object> getReferenceKeys();

}
