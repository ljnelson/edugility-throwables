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

import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mvel2.templates.TemplateRuntime;

public class ThrowableMessageFactory extends ThrowableMessageKeySelector {

  private static final long serialVersionUID = 1L;
  
  private ResourceBundle defaultMessages;

  public ThrowableMessageFactory() {
    this((ResourceBundle)null);
  }

  public ThrowableMessageFactory(final String defaultMessagesResourceBundleName) {
    super();
    if (defaultMessagesResourceBundleName != null) {
      this.setBundle(defaultMessagesResourceBundleName);
    }
  }

  public ThrowableMessageFactory(final ResourceBundle defaultMessages) {
    super();
    this.setBundle(defaultMessages);
  }

  public ResourceBundle getBundle() {
    return this.defaultMessages;
  }

  public void setBundle(final ResourceBundle messages) {
    this.defaultMessages = messages;
  }

  public void setBundle(final String resourceBundleName) {
    this.setBundle(this.getBundle(resourceBundleName, Locale.getDefault()));
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

  public final String getMessage(final Throwable throwableChain) throws ThrowableMatcherException {
    return this.getMessage(throwableChain, Locale.getDefault(), throwableChain == null ? null : throwableChain.getMessage());
  }

  public final String getMessage(final Throwable throwableChain, final Locale locale) throws ThrowableMatcherException {
    return this.getMessage(throwableChain, locale, throwableChain == null ? null : throwableChain.getMessage());
  }

  public final String getMessage(final Throwable throwableChain, final String defaultValue) throws ThrowableMatcherException {
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
    final String messageKey = this.getKey(throwableChain);
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
            this.logger.logp(Level.WARNING, this.getClass().getName(), "getMessage", "noSuchBundleKey", messageKey);
          }
          message = null;
        }
      }
      
    }
    if (message == null) {
      message = defaultValue;
    }

    message = this.interpolateMessage(messageKey, locale, message, throwableChain);

    return message;
  }

  /**
   * 
   * @param messageKey the {@linkplain
   * ThrowableMessageKeySelector#getKey(Throwable, String) message
   * key} used to produce the message being interpolated; must not be
   * {@code null}
   *
   * @param locale the {@link Locale} to use; may be {@code null} in
   * which case if a {@link Locale} is needed then the return value of
   * {@link Locale#getDefault()} is used instead
   *
   * @param message the message to interpolate; may be {@code null} in
   * which case {@code null} will be returned
   *
   * @param throwableChain the {@link Throwable} chain for which a
   * message is ultimately being produced; may be {@code null} in
   * which case the supplied {@code message} will be returned as-is
   */
  protected String interpolateMessage(final String messageKey, Locale locale, final String message, final Throwable throwableChain) {
    String returnValue = message;
    if (message != null && throwableChain != null) {
      final int orbTagIndex = message.indexOf("@{");
      if (orbTagIndex >= 0) {
        // we have an MVEL 2 message template
        // let's eval it at runtime; no need to compile it
        returnValue = (String)TemplateRuntime.eval(message, throwableChain);
      } else {
        if (locale == null) {
          locale = Locale.getDefault();
        }
        final int braceIndex = message.indexOf("{0}");
        if (braceIndex >= 0) {
          // we have a MessageFormat message template
          final MessageFormat messageFormat = new MessageFormat(message, locale);
          final StringBuffer buffer = messageFormat.format(throwableChain, new StringBuffer(), null);
          assert buffer != null;
          returnValue = buffer.toString();
        } else {
          final int percentSIndex = message.indexOf("%s");
          if (percentSIndex >= 0) {
            // we have a java.util.Formatter message template
            final Formatter formatter = new Formatter(locale);
            formatter.format(message, throwableChain);
            returnValue = formatter.toString();
          }
        }
      } 
    }
    return returnValue;
  }

  @Deprecated
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

  @Deprecated
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