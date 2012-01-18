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
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ThrowableMessageKeySelector implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String LS = System.getProperty("line.separator", "\n");

  private final Map<String, Set<ThrowableMatcher>> matchers;

  public ThrowableMessageKeySelector() {
    super();
    this.matchers = new LinkedHashMap<String, Set<ThrowableMatcher>>();
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

  public String getKey(final Throwable chain, final String defaultValue) throws ThrowableMatcherException {
    String returnValue = defaultValue;
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
    return returnValue;
  }

  public static final ThrowableMessageKeySelector read(final Reader reader) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (reader == null) {
      throw new IllegalArgumentException("reader", new NullPointerException("reader"));
    }
    if (reader instanceof BufferedReader) {
      return read((BufferedReader)reader);
    } else {
      return read(new BufferedReader(reader));
    }
  }

  @SuppressWarnings("fallthrough")
  public static final ThrowableMessageKeySelector read(final BufferedReader reader) throws ClassNotFoundException, IOException, ThrowableMatcherException {
    if (reader == null) {
      throw new IllegalArgumentException("reader", new NullPointerException("reader"));
    }

    final ThrowableMessageKeySelector returnValue = new ThrowableMessageKeySelector();

    final Set<String> patterns = new LinkedHashSet<String>();
    final List<String> messageLines = new ArrayList<String>();
    State priorState = null;
    State state = State.NORMAL;
    String line = null;
    for (int i = 1; (line = reader.readLine()) != null; i++) {
      line = line.trim();

      switch (state) {

        // BLOCK_COMMENT
      case BLOCK_COMMENT:
        final int endIndex = line.indexOf("*/");
        if (endIndex < 0) {
          break;
        }
        line = line.substring(endIndex + 2).trim();
        state = state.NORMAL;
        /*
         * FALL THROUGH TO STATE NORMAL
         */
        // end BLOCK_COMMENT


        // NORMAL
      case NORMAL:
        if (line.isEmpty()) {
          break;
        } else if (line.startsWith("--")) {
          throw new IllegalStateException("\"--\" is not permitted here at line " + i);
        } else if (line.startsWith("#") || line.startsWith("//") || line.startsWith("!")) {
          // line comment; nothing to do
          break;
        } else if (line.startsWith("/*")) {
          state = State.BLOCK_COMMENT;
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
          throw new IllegalStateException("An empty line is not permitted here at line " + i);
        } else if (line.startsWith("--")) {
          state = State.MESSAGE;
        } else {
          patterns.add(line);
        }
        break;
        // end MATCHERS

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
          messageLines.add(line);
        }
        break;

      default:
        throw new IllegalStateException("Unexpected state: " + state);
      }
      priorState = state;
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