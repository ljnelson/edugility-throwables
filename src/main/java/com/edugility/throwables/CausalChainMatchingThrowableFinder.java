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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class CausalChainMatchingThrowableFinder extends AbstractThrowableFinder {

  private static final long serialVersionUID = 1L;

  private final Set<AbstractThrowableFinder> delegates;
  
  public CausalChainMatchingThrowableFinder(final Set<AbstractThrowableFinder> delegates) {
    super();
    if (delegates == null) {
      this.delegates = Collections.emptySet();
    } else {
      this.delegates = new LinkedHashSet<AbstractThrowableFinder>(delegates);
    }
    this.clear();
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
    for (final AbstractThrowableFinder delegate : this.delegates) {
      if (delegate != null) {
        delegate.setThrowable(t);
      }
    }
  }

  @Override
  public boolean find() throws ThrowableFinderException {
    boolean returnValue = true;
    final Throwable initialThrowable = this.getThrowable();
    Throwable t = initialThrowable;
    if (t != null && this.delegates != null && !this.delegates.isEmpty()) {
      for (final AbstractThrowableFinder delegate : this.delegates) {
        if (t != null && delegate != null) {
          delegate.setThrowable(t);
          if (!delegate.find()) {
            returnValue = false;
            break;
          }
          t = t.getCause();
        }
      }
    }
    if (!returnValue) {
      this.clear();
    } else {
      this.setFound(initialThrowable);
    }
    return returnValue;
  }

}