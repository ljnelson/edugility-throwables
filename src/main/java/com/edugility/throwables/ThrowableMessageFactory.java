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

import java.util.ResourceBundle;

public class ThrowableMessageFactory extends ThrowableMessageKeySelector {

  private static final long serialVersionUID = 1L;
  
  private ResourceBundle messages;

  public ThrowableMessageFactory() {
    this(null);
  }

  public ThrowableMessageFactory(final ResourceBundle messages) {
    super();
    this.messages = messages;
  }

  public ResourceBundle getResourceBundle() {
    return this.messages;
  }

  public void setResourceBundle(final ResourceBundle messages) {
    this.messages = messages;
  }

  public String getMessage(final Throwable throwableChain, final String defaultValue) throws ThrowableMatcherException {
    return this.getMessage(throwableChain, this.getResourceBundle(), defaultValue);
  }

  public String getMessage(final Throwable throwableChain, final ResourceBundle messages, final String defaultValue) throws ThrowableMatcherException {
    if (throwableChain == null) {
      throw new IllegalArgumentException("throwableChain", new NullPointerException("throwableChain == null"));
    }
    String message = defaultValue;
    if (messages != null) {
      final String messageKey = this.getKey(throwableChain, defaultValue);
      if (messageKey != null) {
        message = messages.getString(messageKey);
        // TODO: format message
      }
    }
    return message;
  }

}