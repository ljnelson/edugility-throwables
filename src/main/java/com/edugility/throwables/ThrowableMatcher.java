/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
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
 * returned by the {@link
 * ThrowablePattern#newThrowableMatcher(String)} method.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @see ThrowablePattern#newThrowableMatcher(String)
 *
 * @since 1.2-SNAPSHOT
 */
public interface ThrowableMatcher extends Serializable {

  /**
   * Returns {@code true} if the supplied {@link Throwable} is
   * accepted or matched in some way.  The supplied {@link Throwable}
   * may be {@code null}.
   *
   * @param t the {@link Throwable} to match; may be {@code null}
   *
   * @return {@code true} if and only if the supplied {@link
   * Throwable} matches; {@code false} otherwise
   *
   * @exception ThrowableMatcherException if there was a problem
   * determining a return value
   */
  public boolean matches(final Throwable t) throws ThrowableMatcherException;

  /**
   * Returns the {@link String} form of the notional pattern that was
   * used to construct this {@link ThrowableMatcher}.  Implementations
   * are permitted to return {@code null}.
   *
   * @return the {@link String} form of the notional pattern that was
   * used to construct this {@link ThrowableMatcher}, or {@code null}
   */
  public String getPattern();

}