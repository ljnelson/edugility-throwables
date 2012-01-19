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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ThrowableMessageFactory extends ThrowableMessageKeySelector {

  private static final long serialVersionUID = 1L;
  
  private ResourceBundle defaultMessages;

  public ThrowableMessageFactory() {
    this(null);
  }

  public ThrowableMessageFactory(final String resourceBundleName) {
    super();
    if (resourceBundleName != null) {
      this.setBundle(this.getBundle(resourceBundleName, Locale.getDefault()));
    }
  }

  public ResourceBundle getBundle() {
    return this.defaultMessages;
  }

  protected ResourceBundle getBundle(final String bundleName, Locale locale) {
    if (bundleName == null) {
      throw new IllegalArgumentException("bundleName");
    }
    if (locale == null) {
      locale = Locale.getDefault();
    }
    return ResourceBundle.getBundle(bundleName, locale, Thread.currentThread().getContextClassLoader());
  }

  private final void setBundle(final ResourceBundle messages) {
    this.defaultMessages = messages;
  }

  public String getMessage(final Throwable throwableChain, final String defaultValue) throws ThrowableMatcherException {
    return this.getMessage(throwableChain, Locale.getDefault(), defaultValue);
  }

  public String getMessage(final Throwable throwableChain, Locale locale, final String defaultValue) throws ThrowableMatcherException {
    if (throwableChain == null) {
      throw new IllegalArgumentException("throwableChain", new NullPointerException("throwableChain == null"));
    }
    if (locale == null) {
      locale = Locale.getDefault();
    }
    String message = null;
    String messageKey = this.getKey(throwableChain, defaultValue);
    if (messageKey != null) {

      final ResourceBundle messages;
      final String bundleKey;

      final int poundIndex = messageKey.indexOf("#");
      if (poundIndex < 0) {
        // e.g. "There was no file found by that name."
        messages = null;
        bundleKey = null;
        message = messageKey;
      } else if (poundIndex == 0) {
        // e.g. "#fileNotFound"
        messages = this.getBundle();
        bundleKey = messageKey.substring(1);
      } else if (poundIndex >= messageKey.length() - 1) {        
        // e.g. "com.foobar.bizbaw.Messages#"
        if (this.logger != null && this.logger.isLoggable(Level.WARNING)) {
          this.logger.logp(Level.WARNING, this.getClass().getName(), "getMessage", "messageKeyEndsWithPound", messageKey);
        }
        messages = null;
        bundleKey = null;
      } else {
        // e.g. "com.foobar.bizbaw.Messages#fileNotFound"
        messages = this.getBundle(messageKey.substring(0, poundIndex).trim(), locale);
        bundleKey = messageKey.substring(poundIndex + 1).trim();
      }

      if (messages != null && bundleKey != null) {
        try {
          message = messages.getString(bundleKey);
        } catch (final MissingResourceException notFound) {
          if (this.logger != null && this.logger.isLoggable(Level.WARNING)) {
            this.logger.logp(Level.WARNING, this.getClass().getName(), "getMessage", "noSuchBundleKey", new Object[] { messageKey });
          }
          message = null;
        }
      }
      
    }
    if (message == null) {
      message = defaultValue;
    }
    return message;
  }

  public static final ThrowableMessageFactory read(final Reader reader) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (reader == null) {
      throw new IllegalArgumentException("reader", new NullPointerException("reader"));
    }
    if (reader instanceof LineNumberReader) {
      return read((LineNumberReader)reader);
    } else {
      return read(new LineNumberReader(reader));
    }
  }

  @SuppressWarnings("fallthrough")
  public static final ThrowableMessageFactory read(final LineNumberReader reader) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (reader == null) {
      throw new IllegalArgumentException("reader", new NullPointerException("reader"));
    }
    final ThrowableMessageFactory returnValue = new ThrowableMessageFactory();
    returnValue.load(reader);
    return returnValue;
  }

  public void load(final LineNumberReader reader) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (reader == null) {
      throw new IllegalArgumentException("reader", new NullPointerException("reader"));
    }
    final Set<String> patterns = new LinkedHashSet<String>();
    final List<String> messageLines = new ArrayList<String>();
    State state = State.NORMAL;
    String line = null;
    while ((line = reader.readLine()) != null) {
      line = line.trim();

      switch (state) {

        // NORMAL
      case NORMAL:
        if (line.isEmpty()) {
          break;
        } else if (line.startsWith("--")) {
          throw new IllegalStateException("\"--\" is not permitted here at line " + reader.getLineNumber());
        } else if (line.startsWith("#")) {
          // line comment; nothing to do
          break;
        } else {
          state = State.MATCHERS;
          patterns.add(line);
          break;
        }
        // end NORMAL


        // MATCHERS
      case MATCHERS:
        if (line.isEmpty()) {
          throw new IllegalStateException("An empty line is not permitted here at line " + reader.getLineNumber());
        } else if (line.startsWith("--")) {
          state = State.MESSAGE;
        } else if (line.startsWith("#")) {
          // line comment; nothing to do
        } else {
          patterns.add(line);
        }
        break;
        // end MATCHERS


        // MESSAGE
      case MESSAGE:
        if (line.isEmpty()) {
          final StringBuilder message = new StringBuilder();
          final Iterator<String> iterator = messageLines.iterator();
          assert iterator != null;
          while (iterator.hasNext()) {
            final String ml = iterator.next();
            if (ml != null) {
              message.append(ml);
              if (iterator.hasNext()) {
                message.append(LS);
              }
            }
          }
          this.addPatterns(patterns, message.toString());
          patterns.clear();
          messageLines.clear();
          state = State.NORMAL;
        } else {
          // Yes, even if the line starts with "#".
          messageLines.add(line);
        }
        break;
        // end MESSAGE


      default:
        throw new IllegalStateException("Unexpected state: " + state);
      }
    }

    if (!messageLines.isEmpty() && !patterns.isEmpty()) {
      final StringBuilder message = new StringBuilder();
      final Iterator<String> iterator = messageLines.iterator();
      assert iterator != null;
      while (iterator.hasNext()) {
        final String ml = iterator.next();
        if (ml != null) {
          message.append(ml);
          if (iterator.hasNext()) {
            message.append(LS);
          }
        }
      }
      this.addPatterns(patterns, message.toString());
      patterns.clear();
      messageLines.clear();
    }
  }

  private enum State {
    NORMAL,
    BLOCK_COMMENT,
    MATCHERS,
    MESSAGE
  }

}