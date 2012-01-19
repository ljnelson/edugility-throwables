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

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ThrowableMessageKeySelector implements Serializable {

  private static final long serialVersionUID = 1L;

  protected static final String LS = System.getProperty("line.separator", "\n");

  protected transient Logger logger;

  private final Map<String, Set<ThrowableMatcher>> matchers;

  public ThrowableMessageKeySelector() {
    super();
    this.logger = this.createLogger();
    if (this.logger == null) {
      this.logger = this.defaultCreateLogger();
    }
    assert this.logger != null;
    this.matchers = new LinkedHashMap<String, Set<ThrowableMatcher>>();
  }

  protected Logger createLogger() {
    return this.defaultCreateLogger();
  }

  private final Logger defaultCreateLogger() {
    Class<?> c = this.getClass();
    final String className = c.getName();
    String bundleName = null;
    while (c != null && bundleName == null) {
      bundleName = String.format("%sLogMessages", c.getName());
      ResourceBundle rb = null;
      try {
        rb = ResourceBundle.getBundle(bundleName);        
      } catch (final MissingResourceException noBundle) {
        bundleName = null;
        rb = null;
      }
      c = c.getSuperclass();
    }
    final Logger logger;
    if (bundleName != null) {
      logger = Logger.getLogger(className, bundleName);
      assert logger != null;
      logger.log(Level.CONFIG, "usingBundleName", bundleName);
    } else {
      logger = Logger.getLogger(className);
    }
    return logger;
  }

  public final void putPattern(final String pattern, final String key) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    this.putPatterns(Collections.singleton(pattern), key);
  }

  public final void putPatterns(final Iterable<String> patterns, final String key) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (key != null && patterns != null) {
      this.put(patternsToMatchers(patterns), key);
    }
  }

  public final void put(final ThrowableMatcher matcher, final String key) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    this.put(Collections.singleton(matcher), key);
  }

  public void put(final Iterable<ThrowableMatcher> matchers, final String key) {
    if (key != null && matchers != null) {
      final Iterator<ThrowableMatcher> iterator = matchers.iterator();
      if (iterator != null && iterator.hasNext()) {
        final Set<ThrowableMatcher> storedMatchers = new LinkedHashSet<ThrowableMatcher>();
        while (iterator.hasNext()) {
          final ThrowableMatcher matcher = iterator.next();
          if (matcher != null) {
            storedMatchers.add(matcher);
          }
        }
        this.matchers.put(key, storedMatchers);
      }
    }
  }

  public static final Iterable<ThrowableMatcher> patternsToMatchers(final Iterable<String> patterns) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    Iterable<ThrowableMatcher> returnValue = null;
    if (patterns != null) {
      final Iterator<String> iterator = patterns.iterator();
      if (iterator != null && iterator.hasNext()) {
        final ThrowablePattern throwablePattern = new ThrowablePattern();
        final Set<ThrowableMatcher> matchers = new LinkedHashSet<ThrowableMatcher>();
        while (iterator.hasNext()) {
          final String pattern = iterator.next();
          if (pattern != null) {
            final ThrowableMatcher matcher = throwablePattern.newThrowableMatcher(pattern);
            if (matcher != null) {
              matchers.add(matcher);
            }
          }
        }
        if (!matchers.isEmpty()) {
          returnValue = matchers;
        }
      }
    }
    if (returnValue == null) {
      returnValue = Collections.<ThrowableMatcher>emptySet();
    }
    return returnValue;
  }

  public final void addPattern(final String pattern, final String key) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    this.addPatterns(Collections.singleton(pattern), key);
  }

  public final void addPatterns(final Iterable<String> patterns, final String key) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (key != null && patterns != null) {
      this.add(patternsToMatchers(patterns), key);
    }
  }

  public final void add(final ThrowableMatcher matcher, final String key) {
    this.add(Collections.singleton(matcher), key);
  }

  public void add(final Iterable<ThrowableMatcher> matchers, final String key) {
    if (key != null && matchers != null) {
      final Iterator<ThrowableMatcher> iterator = matchers.iterator();
      if (iterator != null && iterator.hasNext()) {
        Set<ThrowableMatcher> storedMatchers = this.matchers.get(key);
        if (storedMatchers == null) {
          storedMatchers = new LinkedHashSet<ThrowableMatcher>();
          this.matchers.put(key, storedMatchers);
        }
        while (iterator.hasNext()) {
          final ThrowableMatcher matcher = iterator.next();
          if (matcher != null) {
            storedMatchers.add(matcher);
          }
        }
      }
    }
  }

  public void remove(final String key) {
    if (key != null && !this.matchers.isEmpty()) {
      this.matchers.remove(key);
    }
  }

  public final void remove(final String key, final ThrowableMatcher matcher) {
    this.remove(key, Collections.singleton(matcher));
  }

  public void remove(final String key, final Iterable<ThrowableMatcher> matchers) {
    if (key != null && matchers != null && !this.matchers.isEmpty()) {
      final Set<ThrowableMatcher> storedMatchers = this.matchers.get(key);
      if (storedMatchers != null && !storedMatchers.isEmpty()) {
        for (final ThrowableMatcher matcher : matchers) {
          if (matcher != null) {
            storedMatchers.remove(matcher);
          }
        }
      }
    }
  }

  public final String getKey(final Throwable chain) throws ThrowableMatcherException {
    return this.getKey(chain, null);
  }

  public String getKey(final Throwable chain, final String defaultValue) throws ThrowableMatcherException {
    String returnValue = defaultValue;
    if (this.matchers != null && !this.matchers.isEmpty()) {
      final Set<Entry<String, Set<ThrowableMatcher>>> entrySet = this.matchers.entrySet();
      if (entrySet != null) {
        ENTRY_LOOP:
        for (final Entry<String, Set<ThrowableMatcher>> entry : entrySet) {
          if (entry != null) {
            final String messageKey = entry.getKey();
            if (messageKey != null) {
              final Iterable<ThrowableMatcher> matchers = entry.getValue();
              if (matchers != null) {
                for (final ThrowableMatcher matcher : matchers) {
                  if (matcher != null && matcher.matches(chain)) {
                    returnValue = messageKey;
                    break ENTRY_LOOP;
                  }
                }
              }
            }
          }
        }
      }
    }
    return returnValue;
  }

  private final void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    if (stream != null) {
      stream.defaultReadObject();
    }
    if (this.logger == null) {
      this.logger = this.createLogger();
      if (this.logger == null) {
        this.logger = this.defaultCreateLogger();
      }
    }
    assert this.logger != null;
  }

}