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
import java.io.LineNumberReader;
import java.io.Reader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

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

  public ResourceBundle getBundle() {
    return this.messages;
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

  public void setBundle(final ResourceBundle messages) {
    this.messages = messages;
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
    String message = defaultValue;
    String messageKey = this.getKey(throwableChain, defaultValue);
    if (messageKey != null) {
      final int poundIndex = messageKey.indexOf("#");
      if (poundIndex < 0) {
        // message key is the actual message itself.
        message = messageKey;
      } else if (poundIndex == 0) {
        // whole message is a bundle lookup, but we didn't specify the bundle; use ours.
        final ResourceBundle messages = this.getBundle();
        if (messages != null) {
          try {
            message = messages.getString(messageKey.substring(1));
          } catch (final MissingResourceException kaboom) {
            // TODO: log at WARNING level
            kaboom.printStackTrace();
            message = null;
          }
        }
      } else if (poundIndex >= messageKey.length() - 1) {
        // line terminates in a pound; that should be a bundle name,
        // but there's no key.  We'll just use the whole thing as a
        // message.
        message = messageKey.substring(0, messageKey.length() - 2).trim();
      } else {
        final String bundleName = messageKey.substring(0, poundIndex).trim();
        final String bundleKey = messageKey.substring(poundIndex + 1).trim();
        final ResourceBundle messages = this.getBundle(bundleName, locale);
        if (messages != null) {
          try {
            message = messages.getString(bundleKey);
          } catch (final MissingResourceException notFound) {
            // TODO: log at WARNING level
            message = null;
          }
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
          returnValue.addPatterns(patterns, message.toString());
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
      returnValue.addPatterns(patterns, message.toString());
      patterns.clear();
      messageLines.clear();
    }

    return returnValue;
  }

  private enum State {
    NORMAL,
    BLOCK_COMMENT,
    MATCHERS,
    MESSAGE
  }

}