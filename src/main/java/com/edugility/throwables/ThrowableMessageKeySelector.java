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

import java.io.IOException;
import java.io.LineNumberReader;
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

/**
 * A class that 
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.2-SNAPSHOT
 *
 * @see ThrowableMessageFactory
 */
public class ThrowableMessageKeySelector implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String LS = System.getProperty("line.separator", "\n");

  /**
   * The {@link Logger} used by this {@link
   * ThrowableMessageKeySelector} instance.  Please see <a
   * href="http://wiki.apache.org/commons/Logging/StaticLog">this
   * article</a> for many good reasons why this class does not use a
   * {@code static} logger.
   *
   * <p>This field may be {@code null}.</p>
   *
   * <p>This field is declared {@code transient} because {@link
   * Logger} instances are not {@link Serializable}.</p>
   *
   * <p>This class defines a {@code private final void
   * readObject(final ObjectInputStream stream) throws
   * ClassNotFoundException, IOException} method internally that
   * correctly initializes this field upon deserialization.</p>
   *
   * @see #createLogger()
   */
  protected transient Logger logger;

  private final Map<String, Set<ThrowablePattern>> patterns;

  /**
   * Creates a new {@link ThrowableMessageKeySelector}.
   */
  public ThrowableMessageKeySelector() {
    super();
    this.logger = this.createLogger();
    if (this.logger == null) {
      this.logger = this.defaultCreateLogger();
    }
    assert this.logger != null;
    this.patterns = new LinkedHashMap<String, Set<ThrowablePattern>>();
  }

  /**
   * Returns a {@link Logger} instance suitable for use by this class
   * and its subclasses.
   *
   * @return a {@link Logger} instance
   */
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

  public final void compileAndPut(final String pattern, final String key) throws ClassNotFoundException, IOException {
    this.compileAndPut(Collections.singleton(pattern), key);
  }

  public final void compileAndPut(final Iterable<String> patterns, final String key) throws ClassNotFoundException, IOException {
    if (key != null && patterns != null) {
      this.put(stringsToPatterns(patterns), key);
    }
  }

  public final void put(final ThrowablePattern pattern, final String key) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    this.put(Collections.singleton(pattern), key);
  }

  public void put(final Iterable<ThrowablePattern> patterns, final String key) {
    if (key != null && patterns != null) {
      final Iterator<ThrowablePattern> iterator = patterns.iterator();
      if (iterator != null && iterator.hasNext()) {
        final Set<ThrowablePattern> storedPatterns = new LinkedHashSet<ThrowablePattern>();
        while (iterator.hasNext()) {
          final ThrowablePattern pattern = iterator.next();
          if (pattern != null) {
            storedPatterns.add(pattern);
          }
        }
        this.patterns.put(key, storedPatterns);
      }
    }
  }

  
  public static final Iterable<ThrowablePattern> stringsToPatterns(final Iterable<String> stringPatterns) throws ClassNotFoundException, IOException {
    Iterable<ThrowablePattern> returnValue = null;
    if (stringPatterns != null) {
      final Iterator<String> iterator = stringPatterns.iterator();
      if (iterator != null && iterator.hasNext()) {
        // final ThrowablePattern throwablePattern = new ThrowablePattern();
        final Set<ThrowablePattern> patterns = new LinkedHashSet<ThrowablePattern>();
        while (iterator.hasNext()) {
          final String patternString = iterator.next();
          if (patternString != null) {
            patterns.add(ThrowablePattern.compile(patternString));
          }
        }
        if (!patterns.isEmpty()) {
          returnValue = patterns;
        }
      }
    }
    if (returnValue == null) {
      returnValue = Collections.<ThrowablePattern>emptySet();
    }
    return returnValue;
  }

  public final void compileAndAdd(final String pattern, final String key) throws ClassNotFoundException, IOException {
    this.compileAndAdd(Collections.singleton(pattern), key);
  }

  public final void compileAndAdd(final Iterable<String> patterns, final String key) throws ClassNotFoundException, IOException {
    if (key != null && patterns != null) {
      this.add(stringsToPatterns(patterns), key);
    }
  }

  public final void add(final ThrowablePattern pattern, final String key) {
    this.add(Collections.singleton(pattern), key);
  }

  public final void add(final Iterable<ThrowablePattern> patterns, final String key) {
    if (key != null && patterns != null) {
      final Iterator<ThrowablePattern> iterator = patterns.iterator();
      if (iterator != null && iterator.hasNext()) {
        Set<ThrowablePattern> storedPatterns = this.patterns.get(key);
        if (storedPatterns == null) {
          storedPatterns = new LinkedHashSet<ThrowablePattern>();
          this.patterns.put(key, storedPatterns);
        }
        while (iterator.hasNext()) {
          final ThrowablePattern pattern = iterator.next();
          if (pattern != null) {
            storedPatterns.add(pattern);
          }
        }
      }
    }
  }

  public void remove(final String key) {
    if (key != null && !this.patterns.isEmpty()) {
      this.patterns.remove(key);
    }
  }

  public final void remove(final String key, final ThrowablePattern pattern) {
    this.remove(key, Collections.singleton(pattern));
  }

  public void remove(final String key, final Iterable<ThrowablePattern> patterns) {
    if (key != null && patterns != null && !this.patterns.isEmpty()) {
      final Set<ThrowablePattern> storedPatterns = this.patterns.get(key);
      if (storedPatterns != null && !storedPatterns.isEmpty()) {
        for (final ThrowablePattern pattern : patterns) {
          if (pattern != null) {
            storedPatterns.remove(pattern);
          }
        }
      }
    }
  }

  public final String getKey(final Throwable chain) throws ThrowableMatcherException {
    return this.getKey(chain, null);
  }

  public String getKey(final Throwable chain, final String defaultValue) throws ThrowableMatcherException {
    String returnValue = null;
    final Match match = this.getMatch(chain);
    if (match != null) {
      returnValue = match.messageKey;
    }
    if (returnValue == null) {
      returnValue = defaultValue;
    }
    return returnValue;
  }

  protected final Match getMatch(final Throwable chain) throws ThrowableMatcherException {
    Match returnValue = null;
    if (this.patterns != null && !this.patterns.isEmpty()) {
      final Set<Entry<String, Set<ThrowablePattern>>> entrySet = this.patterns.entrySet();
      if (entrySet != null) {
        ENTRY_LOOP:
        for (final Entry<String, Set<ThrowablePattern>> entry : entrySet) {
          if (entry != null) {
            final String messageKey = entry.getKey();
            if (messageKey != null) {
              final Iterable<ThrowablePattern> patterns = entry.getValue();
              if (patterns != null) {
                for (final ThrowablePattern pattern : patterns) {
                  if (pattern != null) {
                    final ThrowableMatcher matcher = pattern.matcher(chain);
                    if (matcher != null && matcher.matches()) {
                      returnValue = new Match(matcher, messageKey);
                      break ENTRY_LOOP;
                    }
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
          this.compileAndAdd(patterns, message.toString());
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
      this.compileAndAdd(patterns, message.toString());
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

  protected static final class Match implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ThrowableMatcher matcher;

    private final String messageKey;

    private Match(final ThrowableMatcher matcher, final String messageKey) {
      super();
      this.matcher = matcher;
      this.messageKey = messageKey;
    }

    public final ThrowableMatcher getThrowableMatcher() {
      return this.matcher;
    }

    public final String getMessageKey() {
      return this.messageKey;
    }


  }

}
