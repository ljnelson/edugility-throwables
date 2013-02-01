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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Deprecated
public class DisjunctiveThrowableFinder extends MultipleDelegateThrowableFinder {

  private static final long serialVersionUID = 1L;
  
  public DisjunctiveThrowableFinder(final AbstractThrowableFinder... delegates) {
    super(delegates);
  }

  public DisjunctiveThrowableFinder(final List<AbstractThrowableFinder> delegates) {
    super(delegates);
  }

  public DisjunctiveThrowableFinder(final LinkedHashSet<AbstractThrowableFinder> delegates) {
    super(delegates);
  }

  @Override
  protected boolean findSolo() throws ThrowableFinderException {
    boolean returnValue = false;
    Throwable initialThrowable = this.getThrowable();
    if (initialThrowable != null && this.delegates != null && !this.delegates.isEmpty()) {
      for (final AbstractThrowableFinder delegate : this.delegates) {
        if (delegate != null) {
          delegate.setThrowable(initialThrowable);
          if (delegate.find()) {
            returnValue = true;
          }
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
