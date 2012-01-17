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

/**
 * An {@link Exception} that indicates that something went wrong
 * during an {@linkplain ThrowableMatcher#matches(Throwable) attempt
 * to match a <tt>Throwable</tt>}.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @see ThrowableMatcher
 *
 * @see ThrowablePattern
 *
 * @since 1.2-SNAPSHOT
 */
public class ThrowableMatcherException extends Exception {

  /**
   * The version of this class for serialization purposes.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new {@link ThrowableMatcherException}.
   */
  public ThrowableMatcherException() {
    super();
  }

  /**
   * Creates a new {@link ThrowableMatcherException}.
   *
   * @param message a detail message explaining the problem; may be
   * {@code null}
   */
  public ThrowableMatcherException(final String message) {
    super(message);
  }

  /**
   * Creates a new {@link ThrowableMatcherException}.
   *
   * @param cause the {@link Throwable} that caused this {@link
   * ThrowableMatcherException} to be thrown; may be {@code null}
   */
  public ThrowableMatcherException(final Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new {@link ThrowableMatcherException}.
   *
   * @param message a detail message explaining the problem; may be
   * {@code null}
   *
   * @param cause the {@link Throwable} that caused this {@link
   * ThrowableMatcherException} to be thrown; may be {@code null}
   */
  public ThrowableMatcherException(final String message, final Throwable cause) {
    super(message, cause);
  }

}