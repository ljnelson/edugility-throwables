/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2012-2013 Edugility LLC.
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

@Deprecated
public abstract class AbstractThrowableFinder implements Cloneable, Serializable {

  private static final long serialVersionUID = 1L;

  private Throwable initialThrowable;

  private Throwable found;

  private int startIndex;

  private int exclusiveEndIndex;

  private AbstractThrowableFinder next;

  protected AbstractThrowableFinder() {
    super();
    this.clear();
  }

  protected AbstractThrowableFinder(final AbstractThrowableFinder next) {
    this();
    this.setNext(next);
  }

  public void setNext(final AbstractThrowableFinder next) {
    this.next = next;
    if (next != null) {
      next.clear();
    }
  }

  public AbstractThrowableFinder getNext() {
    return this.next;
  }

  @Override
  public AbstractThrowableFinder clone() {
    AbstractThrowableFinder copy = null;
    try {
      copy = (AbstractThrowableFinder)super.clone();
    } catch (final CloneNotSupportedException cnse) {
      throw (InternalError)new InternalError().initCause(cnse);
    }
    return copy;
  }

  public void clear() {
    this.found = null;
    this.startIndex = -1;
    this.exclusiveEndIndex = -1;
  }

  public Throwable getThrowable() {
    return this.initialThrowable;
  }

  public void setThrowable(final Throwable throwable) {
    this.clear();
    this.initialThrowable = throwable;
    final AbstractThrowableFinder next = this.getNext();
    if (next != null) {
      next.setThrowable(throwable);
    }
  }

  public Throwable getFound() {
    return this.found;
  }

  protected final void setFound(final Throwable t) {
    final Throwable initialThrowable = this.getThrowable();
    if (initialThrowable == null) {
      throw new IllegalStateException();
    }
    this.setFound(t, 1);
  }

  protected final void setFound(final Throwable t, final int length) {
    final Throwable initialThrowable = this.getThrowable();
    if (initialThrowable == null) {
      throw new IllegalStateException();
    }
    if (t == null || length < 1) {
      this.setFound(null, null);
    } else if (length == 1) {
      this.setFound(t, t);
    } else {
      this.setFound(t, this.get(t, length - 1));
    }
  }

  public static final Throwable get(final Throwable start, final int index) {
    Throwable returnValue = null;
    if (index <= 0) {
      returnValue = start;
    } else if (start != null) {
      final ThrowableCauseIterator ti = new ThrowableCauseIterator(start);
      int i = 0;
      while (ti.hasNext()) {
        final Throwable t = ti.next();
        assert t != null;
        if (i == index) {
          returnValue = t;
          break;
        }
        i++;
      }
    }
    return returnValue;
  }

  public static final int indexOf(final Throwable start, final Throwable end) {
    int returnValue = -1;
    if (start != null && end != null) {
      final ThrowableCauseIterator ti = new ThrowableCauseIterator(start);
      int i = 0;
      while (ti.hasNext()) {
        final Throwable t = ti.next();
        assert t != null;
        if (t == end) {
          returnValue = i;
          break;
        }
        i++;
      }
    }
    return returnValue;
  }

  protected void setFound(final Throwable start, final Throwable end) {
    final Throwable initialThrowable = this.getThrowable();
    if (initialThrowable == null) {
      throw new IllegalStateException();
    }
    this.found = start;
    this.startIndex = indexOf(initialThrowable, start);
    this.exclusiveEndIndex = this.startIndex + this.indexOf(start, end) + 1;
  }

  public int start() {
    return this.startIndex;
  }

  public int end() {
    return this.exclusiveEndIndex;
  }

  protected Throwable getThrowableForNext() {
    return this.getThrowable();
  }

  public final boolean find() throws ThrowableFinderException {
    boolean returnValue = this.findSolo();
    if (returnValue) {
      final Throwable found = this.getFound();
      assert found != null;
      final AbstractThrowableFinder next = this.getNext();
      if (next != null) {
        next.setThrowable(this.getThrowableForNext());
        returnValue = next.find();
        if (returnValue) {
          final Throwable nextFound = next.getFound();
          assert nextFound != null;
          final Throwable initialThrowable = this.getThrowable();
          if (nextFound != found && indexOf(initialThrowable, nextFound) < indexOf(initialThrowable, found)) {
            this.setFound(nextFound);
          }
        }
      }
    }
    return returnValue;
  }

  protected abstract boolean findSolo() throws ThrowableFinderException;

}
