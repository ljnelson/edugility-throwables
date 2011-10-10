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
import java.util.List;

public final class Throwables {

  private Throwables() {
    super();
  }

  public static final Throwable getPrimordialCause(Throwable t) {
    if (t == null) {
      return null;
    }
    Throwable cause = t;
    while ((cause = cause.getCause()) != null) {
      t = cause;
    }
    return t;
  }

  public static final List<Throwable> asList(final Throwable t) {
    if (t == null) {
      return null;
    }
    final List<Throwable> l = new ArrayList<Throwable>();
    l.add(t);
    Throwable cause = t;
    while ((cause = cause.getCause()) != null) {
      l.add(cause);
    }
    return Collections.unmodifiableList(l);
  }

  public static final <T extends Throwable> T firstInstance(Throwable t, final Class<T> throwableClass) {
    if (throwableClass == null) {
      return null;
    }
    while (t != null) {
      if (throwableClass.isInstance(t)) {
        return throwableClass.cast(t);
      }
      t = t.getCause();
    }
    return null;
  }

  public static final <T extends Throwable> T lastInstance(Throwable t, final Class<T> throwableClass) {
    if (throwableClass == null) {
      return null;
    }
    T returnValue = null;
    while (t != null) {
      if (throwableClass.isInstance(t)) {
        returnValue = throwableClass.cast(t);
      }
      t = t.getCause();
    }
    return returnValue;
  }

}