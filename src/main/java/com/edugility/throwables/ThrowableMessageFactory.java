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

import java.text.MessageFormat;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;

import com.edugility.throwables.ThrowableMessageKeySelector.Match;

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

  /**
   * Returns a localized message that is appropriate for the supplied
   * {@link Throwable} chain in the supplied {@link Locale}.
   */
  public String getMessage(final Throwable throwableChain, Locale locale, final String defaultValue) throws ThrowableMatcherException {
    if (throwableChain == null) {
      throw new IllegalArgumentException("throwableChain", new NullPointerException("throwableChain == null"));
    }
    if (locale == null) {
      locale = Locale.getDefault();
    }
    String message = null;
    final Match match = this.getMatch(throwableChain);
    if (match != null) {
      final String messageKey = match.getMessageKey();
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

    }
    if (message == null) {
      message = defaultValue;
    }

    message = this.interpolateMessage(match, locale, message, throwableChain);

    return message;
  }

  /**
   * Expands template constructs in the supplied {@code message}.
   *
   * @param match the {@link Match} that
   * produced the message to interpolate; may be {@code null} in which
   * case no match actually occurred
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
  protected String interpolateMessage(final Match match, Locale locale, final String message, final Throwable throwableChain) {
    String returnValue = message;
    if (match != null && message != null && throwableChain != null) {
      final ThrowableMatcher matcher = match.getThrowableMatcher();
      assert matcher != null;
      int index = 0;
      while (index >= 0) {
        index = returnValue.indexOf("@{");
        if (index >= 0) {
          // we have an MVEL 2 message template
          final Map<Object, Object> variables = new HashMap<Object, Object>(5);
          variables.put("matcher", matcher);
          returnValue = (String)TemplateRuntime.eval(returnValue, throwableChain, variables);
        } else {
          if (locale == null) {
            locale = Locale.getDefault();
          }
          index = returnValue.indexOf("{0}");
          if (index >= 0) {
            // we have a MessageFormat message template
            final MessageFormat messageFormat = new MessageFormat(returnValue, locale);
            final Iterable<Object> referenceKeys = matcher.getReferenceKeys();
            final SortedMap<Integer, Throwable> arguments = new TreeMap<Integer, Throwable>();
            arguments.put(Integer.valueOf(0), throwableChain);
            if (referenceKeys != null) {
              for (final Object key : referenceKeys) {
                if (key instanceof Integer) {
                  arguments.put((Integer)key, matcher.getReference(key));
                } else if (key instanceof Number) {
                  arguments.put(Integer.valueOf(((Number)key).intValue()), matcher.getReference(key));
                }
              }
            }
            final Object[] args = new Object[arguments.size()];
            final Set<Entry<Integer, Throwable>> entrySet = arguments.entrySet();
            assert entrySet != null;
            int i = 0;
            for (final Entry<Integer, Throwable> argEntry : entrySet) {
              assert argEntry != null;
              args[i++] = argEntry.getValue();
            }
            final StringBuffer buffer = messageFormat.format(args, new StringBuffer(), null);
            assert buffer != null;
            returnValue = buffer.toString();
          } else {
            index = returnValue.indexOf("%s");
            if (index >= 0) {
              // we have a java.util.Formatter message template
              final Formatter formatter = new Formatter(locale);
              formatter.format(returnValue, throwableChain);
              returnValue = formatter.toString();
            }
          }
        }
      }
    }
    return returnValue;
  }

}