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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Deprecated
public abstract class MultipleDelegateThrowableFinder extends AbstractThrowableFinder {

  private static final long serialVersionUID = 1L;

  protected final LinkedHashSet<AbstractThrowableFinder> delegates;
  
  protected MultipleDelegateThrowableFinder(final AbstractThrowableFinder... delegates) {
    this(delegates == null ? (LinkedHashSet<AbstractThrowableFinder>)null : new LinkedHashSet<AbstractThrowableFinder>(Arrays.asList(delegates)));
  }

  protected MultipleDelegateThrowableFinder(final List<AbstractThrowableFinder> delegates) {
    this(delegates == null ? (LinkedHashSet<AbstractThrowableFinder>)null : new LinkedHashSet<AbstractThrowableFinder>(delegates));
  }

  protected MultipleDelegateThrowableFinder(final LinkedHashSet<AbstractThrowableFinder> delegates) {
    super();
    if (delegates == null) {
      this.delegates = new LinkedHashSet<AbstractThrowableFinder>();
    } else {
      this.delegates = new LinkedHashSet<AbstractThrowableFinder>(delegates);
    }
    this.clear();
  }

  public void addDelegate(final AbstractThrowableFinder delegate) {
    if (delegate != null) {
      assert this.delegates != null;
      this.delegates.add(delegate);
    }
  }

  public void removeDelegate(final AbstractThrowableFinder delegate) {
    if (delegate != null && this.delegates != null && !this.delegates.isEmpty()) {
      this.delegates.remove(delegate);
    }
  }

  public AbstractThrowableFinder popDelegate() {
    AbstractThrowableFinder delegate = null;
    if (this.delegates != null && !this.delegates.isEmpty()) {
      final Iterator<AbstractThrowableFinder> iterator = this.delegates.iterator();
      if (iterator != null) {
        while (iterator.hasNext()) {
          delegate = iterator.next();
          if (!iterator.hasNext()) {
            iterator.remove();
            break;
          }
        }
      }
    }
    return delegate;
  }

  @Override
  public void clear() {
    super.clear();
    if (this.delegates != null && !this.delegates.isEmpty()) {
      for (final AbstractThrowableFinder delegate : this.delegates) {
        if (delegate != null) {
          delegate.clear();
        }
      }
    }
  }

  @Override
  public void setThrowable(final Throwable t) {
    super.setThrowable(t);
    if (this.delegates != null) {
      for (final AbstractThrowableFinder delegate : this.delegates) {
        if (delegate != null) {
          delegate.setThrowable(t);
        }
      }
    }
  }

}
