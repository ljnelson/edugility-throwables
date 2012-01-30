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

public class FirstMatchingThrowableFinder extends AbstractThrowableFinder {

  private static final long serialVersionUID = 1L;

  private final AbstractThrowableFinder delegate;
  
  public FirstMatchingThrowableFinder(final AbstractThrowableFinder delegate) {
    super();
    if (delegate == null) {
      throw new IllegalArgumentException("delegate");
    }
    this.delegate = delegate;
    this.clear();
  }

  @Override
  public void clear() {
    super.clear();
    if (this.delegate != null) {
      this.delegate.clear();
    }
  }

  @Override
  public boolean find() throws ThrowableFinderException {
    boolean returnValue = false;
    final ThrowableCauseIterator iterator = new ThrowableCauseIterator(this.getThrowable());
    while (iterator.hasNext()) {
      final Throwable t = iterator.next();
      assert t != null;
      this.delegate.setThrowable(t);
      if (this.delegate.find()) {
        this.setFound(this.delegate.getFound());
        returnValue = true;
        break;
      }
    }
    if (!returnValue) {
      this.clear();
    }
    return returnValue;
  }

}