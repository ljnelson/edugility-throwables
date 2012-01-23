/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * Copyright (c) 2010-2012 Edugility LLC.
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
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

import java.nio.CharBuffer;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mvel2.MVEL;

/**
 * A pattern that can match {@link Throwable} instances.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @see ThrowableMatcher
 *
 * @see #compile(String)
 *
 * @since 1.2-SNAPSHOT
 */
public final class ThrowablePattern implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String LS = System.getProperty("line.separator", "\n");

  private final ConjunctiveThrowableMatcher matcher;

  private enum State {
    START,
    NORMAL,
    COMMENT,
    INDETERMINATE_GLOB,
    GLOB,
    GREEDY_GLOB,
    INDETERMINATE_PERIOD,
    ELLIPSIS,
    IDENTIFIER,
    REFERENCE,
    PROPERTY_BLOCK
  }

  private ThrowablePattern(final ConjunctiveThrowableMatcher matcher) {
    super();
    this.matcher = matcher;
  }

  private static final void newElementMatcher(final ParsingState parsingState, final ThrowableMatcher delegate) {
    parsingState.matchers.add(new ThrowableListElementThrowableMatcher(parsingState.depthLevel, delegate));
  }

  private static final void ellipsis(final ParsingState parsingState) {

  }

  private static final void identifierStart(final ParsingState parsingState) {
    parsingState.classNameBuffer.setLength(0);
  }

  private static final void identifier(final ParsingState parsingState, final int c) {
    parsingState.classNameBuffer.append((char)c);
  }

  private static final void identifier(final ParsingState parsingState, final String s) {
    parsingState.classNameBuffer.append(s);
  }

  private static final void identifierEnd(final ParsingState parsingState) {

  }

  @SuppressWarnings("unchecked")
  private static final void subclassTest(final ParsingState parsingState, final ClassLoader loader) throws ClassNotFoundException {
    newElementMatcher(parsingState, new InstanceOfThrowableMatcher((Class<? extends Throwable>)loader.loadClass(parsingState.classNameBuffer.toString())));
  }

  private static final void classNameMatchTest(final ParsingState parsingState) {
    newElementMatcher(parsingState, new ClassNameEqualityThrowableMatcher(parsingState.classNameBuffer.toString()));
  }

  private static final void slash(final ParsingState parsingState) {
    slash(parsingState, true);
  }

  private static final void slash(final ParsingState parsingState, final boolean adjustDepth) {
    if (adjustDepth) {
      if (parsingState.greedyGlob) {
        parsingState.depthLevel--;
      } else {
        parsingState.depthLevel++;
      }
    }
  }

  private static final void referenceStart(final ParsingState parsingState) {
    parsingState.referenceBuffer.setLength(0);
  }

  private static final void referenceCharacter(final ParsingState parsingState) {
    parsingState.referenceBuffer.append((char)parsingState.character);
  }

  private static final void referenceEnd(final ParsingState parsingState) {

  }

  private static final void setReference(final ParsingState parsingState) {
    final String referenceName = parsingState.referenceBuffer.toString();
    Object key = null;
    try {
      key = Integer.parseInt(referenceName);
    } catch (final NumberFormatException notANumber) {
      key = referenceName;
    }
    newElementMatcher(parsingState, new ReferenceStoringThrowableMatcher(key));
  }

  private static final void glob(final ParsingState parsingState) {
    identifierStart(parsingState);
    identifier(parsingState, "java.lang.Throwable");
  }

  private static final void greedyGlob(final ParsingState parsingState) {
    parsingState.greedyGlob = true;
    parsingState.depthLevel = 0;
  }

  private static final void commentStart(final ParsingState parsingState) {
    parsingState.commentBuffer.setLength(0);
  }

  private static final void comment(final ParsingState parsingState, final int c) {
    parsingState.commentBuffer.append((char)c);
  }

  private static final void commentEnd(final ParsingState parsingState) {

  }

  private static final void propertyBlockStart(final ParsingState parsingState) {
    parsingState.propertyBlockBuffer.setLength(0);
  }

  private static final void propertyBlock(final ParsingState parsingState, final int c) {
    parsingState.propertyBlockBuffer.append((char)c);
  }

  private static final void propertyBlockEnd(final ParsingState parsingState) {
    newElementMatcher(parsingState, new PropertyBlockThrowableMatcher(parsingState.propertyBlockBuffer.toString()));
  }

  private static final void start(final ParsingState parsingState) {
    parsingState.state = State.START;
    parsingState.priorState = null;
    parsingState.periodCount = 0;
    parsingState.depthLevel = 0;
    parsingState.greedyGlob = false;
    parsingState.propertyBlockBuffer.setLength(0);
    parsingState.classNameBuffer.setLength(0);
    parsingState.commentBuffer.setLength(0);
  }

  private static final void leftAnchor(final ParsingState parsingState) {
    parsingState.leftAnchor = true;
  }

  /**
   * Returns a {@link ThrowableMatcher} suitable for testing the
   * supplied {@link Throwable} chain.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param throwableChain the {@link Throwable} to be tested; may be
   * {@code null} (rather uselessly)
   *
   * @return a {@link ThrowableMatcher}; never {@code null}
   */
  public final ThrowableMatcher matcher(final Throwable throwableChain) {
    final ConjunctiveThrowableMatcher result = this.matcher.clone();
    assert result != null;
    result.setThrowable(throwableChain);
    return result;
  }

  /**
   * Compiles the supplied pattern into a new {@link
   * ThrowablePattern}.  This method never returns {@code null}.
   *
   * <p>The supplied pattern must conform to the following (currently
   * informal) grammar:</p>
   *
   * <pre>
   *
   * Pattern = PatternStart '<b>/</b>' PatternBody
   *
   * PatternStart = '<b>/</b>'? ( GreedyGlob | ClassTest )
   *
   * GreedyGlob = '<b>**</b>'
   *
   * PatternBody = ClassTest ( '<b>/</b>' ( GreedyGlob | ClassTest ) )*
   *
   * ClassTest = ( ClassName Ellipsis? PropertyBlock? Reference? ) | Glob
   *
   * Glob = '<b>*</b>'
   *
   * PropertyBlock = '<b>(</b>' <i><a href="http://mvel.codehaus.org/">MVEL</a> expression</i> '<b>)</b>'
   *
   * Reference = '<b>[</b>' ReferenceKey '<b>]</b>'
   *
   * Ellipsis = '<b>\u2026</b>' | '<b>...</b>'
   *
   * ClassName = <i>Fully qualified Java class name</i>
   *
   * ReferenceKey = <i>Java literal or {@link String}</i>
   *
   * </pre>
   *
   * <p><strong>Note:</strong> currently there is support for only one
   * occurrence of <tt>**</tt>.</p>
   *
   * <h3>Common and Hopefully Useful Examples</h3>
   *
   * <p>All of the following examples work on a <i>{@link Throwable}
   * chain</i>, which is defined for these purposes as a {@link
   * Throwable} whose {@linkplain Throwable#getCause() cause} either
   * is {@code null} or references another {@link Throwable} that is
   * not the same as the {@link Throwable} whose cause it is.</p>
   *
   * <dl>
   *
   * <dt><tt>javax.ejb.EJBException.../javax.persistence.PersistenceException.../**&#47;java.sql.SQLException...</tt></dt>
   *
   * <dd>Matches a {@link Throwable} chain that begins with a
   * <tt>javax.ejb.EJBException</tt> instance, followed immediately by
   * a <tt>javax.persistence.PersistenceException</tt>, followed by
   * zero or more {@link Throwable} instances and terminated with an
   * instance of {@link SQLException}.  The ellipsis ("<tt>...</tt>"
   * or "<tt>\u2026</tt>") indicates "any instance"; a simple
   * classname without a trailing ellipsis means that the candidate
   * {@link Throwable}'s {@linkplain Class#getName() class name} must
   * match exactly.</dd>
   *
   * <dt><tt>**&#47;com.foobar.FoobarException...</tt></dt>
   *
   * <dd>Matches any {@link Throwable} chain whose root cause is an
   * instance of <tt>com.foobar.FoobarException</tt>.</dd>
   *
   * </dl>
   *
   * @param pattern the pattern to parse; must not be {@code null}
   *
   * @return a new {@link ThrowablePattern}; never {@code null}
   *
   * @exception IllegalArgumentException if {@code pattern} is {@code
   * null}
   *
   * @exception ClassNotFoundException if the supplied {@code pattern}
   * contains a segment that will result in the attempted loading of a
   * {@link Class}, and if that class loading operation fails
   *
   * @exception IOException if {@linkplain StringReader#read()
   * reading of the supplied <tt>String</tt>} fails for some obscure
   * reason
   */
  public static final ThrowablePattern compile(final String pattern, ClassLoader loader) throws ClassNotFoundException, IOException {

    if (loader == null) {
      loader = Thread.currentThread().getContextClassLoader();
      if (loader == null) {
        loader = Throwable.class.getClassLoader();
      }
    }

    final ParsingState parsingState = new ParsingState(pattern);
    for (parsingState.position = 0; parsingState.read() != -1; parsingState.position++) {
      final State originalState = parsingState.state;

      switch (parsingState.state) {

        // START
      case START:

        if (Character.isWhitespace(parsingState.character)) {
          // Eat whitespace
          break;
        }

        switch (parsingState.character) {

        case '^':
          start(parsingState);
          leftAnchor(parsingState);
          parsingState.state = State.NORMAL;
          break;

        case '/':
          start(parsingState);
          slash(parsingState, false);
          leftAnchor(parsingState);
          parsingState.state = State.NORMAL;
          break;

        case '#':
          start(parsingState);
          parsingState.state = State.COMMENT;
          commentStart(parsingState);
          break;

        case '*':
          start(parsingState);
          parsingState.state = State.INDETERMINATE_GLOB;
          break;

        default:
          start(parsingState);
          if (!Character.isJavaIdentifierStart(parsingState.character)) {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
          }
          parsingState.state = State.IDENTIFIER;
          identifierStart(parsingState);
          identifier(parsingState, (char)parsingState.character);
        }
        break;
        // end START


        // NORMAL
      case NORMAL:

        if (Character.isWhitespace(parsingState.character)) {
          // Eat whitespace
          break;
        }

        switch (parsingState.character) {

        case '#':
          parsingState.state = State.COMMENT;
          commentStart(parsingState);
          break;

        case '*':
          parsingState.state = State.INDETERMINATE_GLOB;
          break;

        case '/':
          slash(parsingState);
          break;

        case '[':
          parsingState.state = State.REFERENCE;
          referenceStart(parsingState);
          break;

        default:
          if (!Character.isJavaIdentifierStart(parsingState.character)) {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
          }
          parsingState.state = State.IDENTIFIER;
          identifierStart(parsingState);
          identifier(parsingState, (char)parsingState.character);
        }
        break;
        // end NORMAL


        // COMMENT
      case COMMENT:
        switch (parsingState.character) {

        case '\r':
        case '\n':
          commentEnd(parsingState);
          parsingState.state = State.NORMAL;
          break;

        default:
          comment(parsingState, parsingState.character);

        }
        break;
        // end COMMENT


        // INDETERMINATE_PERIOD
      case INDETERMINATE_PERIOD:
        assert parsingState.priorState == State.IDENTIFIER || parsingState.priorState == State.INDETERMINATE_PERIOD : "Prior state was not " + State.IDENTIFIER + " and was not " + State.INDETERMINATE_PERIOD + ": " + parsingState.priorState;
        switch (parsingState.character) {

        case '.':
          parsingState.periodCount++;
          assert parsingState.periodCount > 0 : "parsingState.periodCount <= 0: " + parsingState.periodCount;
          assert parsingState.periodCount <= 3 : "parsingState.periodCount > 3: " + parsingState.periodCount;
          if (parsingState.periodCount == 3) {
            parsingState.periodCount = 0;
            // Our current state is INDETERMINATE_PERIOD, but now we
            // know we just ran into an ellipsis.  That means our
            // prior prior prior state was IDENTIFIER.  That means we
            // just ended an identifier.
            identifierEnd(parsingState);
            parsingState.state = State.ELLIPSIS;
            ellipsis(parsingState);
            subclassTest(parsingState, loader);
          }
          break;

        default:
          parsingState.periodCount = 0;
          if (Character.isJavaIdentifierStart(parsingState.character)) { // yes, start, not part: each portion of a package name is an Identifier, so must begin with an IdentifierStart.
            parsingState.state = State.IDENTIFIER;
            identifier(parsingState, '.');
            identifier(parsingState, parsingState.character);
          } else {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
          }
        }
        break;
        // end INDETERMINATE_PERIOD


        // INDETERMINATE_GLOB
      case INDETERMINATE_GLOB:
        if (Character.isWhitespace(parsingState.character) || parsingState.character == '/') {
          parsingState.state = State.GLOB;
          glob(parsingState);
          subclassTest(parsingState, loader);
          parsingState.state = State.NORMAL;
          break;
        }
        switch (parsingState.character) {

        case '*':
          if (parsingState.greedyGlob) {
            // we already found one! This is illegal.
            throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
          } else {
            parsingState.state = State.GREEDY_GLOB;
            greedyGlob(parsingState);
          }
          break;

        default:
          throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
        }
        break;
        // end INDETERMINATE_GLOB


        // GREEDY_GLOB
      case GREEDY_GLOB:
        if (Character.isWhitespace(parsingState.character)) {
          // Eat whitespace
          break;
        }
        switch (parsingState.character) {

        case '/':
          slash(parsingState);
          parsingState.state = State.NORMAL;
          break;

        default:
          throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
        }
        break;
        // end GREEDY GLOB


        // IDENTIFIER
      case IDENTIFIER:

        if (Character.isWhitespace(parsingState.character)) {
          // End the identifier, eat the whitespace, and reset the state
          identifierEnd(parsingState);
          classNameMatchTest(parsingState);
          parsingState.state = State.NORMAL;
          break;
        }

        switch (parsingState.character) {

        case '.':
          parsingState.periodCount++;
          parsingState.state = State.INDETERMINATE_PERIOD;
          break;

        case '\u2026':
          identifierEnd(parsingState);
          parsingState.state = State.ELLIPSIS;
          ellipsis(parsingState);
          subclassTest(parsingState, loader);
          break;

        case '[':
          parsingState.state = State.REFERENCE;
          referenceStart(parsingState);
          break;

        case '/':
          identifierEnd(parsingState);
          classNameMatchTest(parsingState);
          slash(parsingState);
          parsingState.state = State.NORMAL;
          break;

        case '(':
          identifierEnd(parsingState);
          classNameMatchTest(parsingState);
          parsingState.state = State.PROPERTY_BLOCK;
          propertyBlockStart(parsingState);
          break;

        default:
          if (!Character.isJavaIdentifierPart(parsingState.character)) {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
          }
          identifier(parsingState, parsingState.character);
        }
        break;
        // end IDENTIFIER


        // REFERENCE
      case REFERENCE:

        if (Character.isWhitespace(parsingState.character)) {
          // Eat whitespace
          break;
        }

        switch (parsingState.character) {

        case ']':
          referenceEnd(parsingState);
          setReference(parsingState);
          parsingState.state = State.NORMAL;
          break;

        default:
          referenceCharacter(parsingState);
        }

        break;
        // end REFERENCE

        // ELLIPSIS
      case ELLIPSIS:
        if (Character.isWhitespace(parsingState.character)) {
          // Eat whitespace
          break;
        }
        switch (parsingState.character) {
        case '/':
          slash(parsingState);
          parsingState.state = State.NORMAL;
          break;

        case '(':
          parsingState.state = State.PROPERTY_BLOCK;
          propertyBlockStart(parsingState);
          break;

        case '[':
          parsingState.state = State.REFERENCE;
          referenceStart(parsingState);
          break;

        case '#':
          parsingState.state = State.COMMENT;
          commentStart(parsingState);
          break;

        default:
          parsingState.state = State.NORMAL;
        }
        break;
        // end ELLIPSIS


        // PROPERTY_BLOCK
      case PROPERTY_BLOCK:
        switch (parsingState.character) {

        case ')':
          propertyBlockEnd(parsingState);
          parsingState.state = State.NORMAL;
          break;

        default:
          propertyBlock(parsingState, parsingState.character);
        }
        break;
        // end PROPERTY_BLOCK


      default:
        throw new IllegalStateException(buildIllegalStateExceptionMessage(parsingState));
      }

      parsingState.priorState = originalState;
    }

    switch (parsingState.priorState) {
    case COMMENT:
      commentEnd(parsingState);
      break;
    case IDENTIFIER:
      identifierEnd(parsingState);
      classNameMatchTest(parsingState);
      break;
    case INDETERMINATE_PERIOD:
      if (parsingState.periodCount != 0) {
        throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pattern.length() - 1, parsingState.priorState));
      }
      break;
    case PROPERTY_BLOCK:
      break;
    case NORMAL:
      break;
    default:
      throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pattern.length() - 1, parsingState.priorState));
    }

    final ConjunctiveThrowableMatcher matcher = parsingState.matchers.clone();
    return new ThrowablePattern(matcher);
  }

  /**
   * Calls the {@link #compile(String, ClassLoader)} method, passing
   * it the supplied {@code pattern} and the return value of {@link
   * Thread#getContextClassLoader()
   * Thread.currentThread().getContextClassLoader()}, and returns its
   * return value.
   *
   * @param pattern a {@link String} representation of the pattern to
   * be compiled; must not be {@code null}
   *
   * @return a new {@link ThrowablePattern}; never {@code null}
   *
   * @exception IllegalArgumentException if {@code pattern} is {@code
   * null}
   *
   * @exception ClassNotFoundException if the supplied {@code pattern}
   * contains a segment that will result in the attempted loading of a
   * {@link Class}, and if that class loading operation fails
   *
   * @exception IOException if {@linkplain StringReader#read()
   * reading of the supplied <tt>String</tt>} fails for some obscure
   * reason
   */
  public static final ThrowablePattern compile(final String pattern) throws ClassNotFoundException, IOException {
    return compile(pattern, Thread.currentThread().getContextClassLoader());
  }

  private static final String buildIllegalStateExceptionMessage(final ParsingState state) {
    return buildIllegalStateExceptionMessage(state.pattern, state.position, state.state);
  }

  private static final String buildIllegalStateExceptionMessage(final String s, final int position, final State state) {
    final StringBuilder sb = new StringBuilder();
    sb.append("Unexpected character in state ");
    sb.append(state);
    sb.append(" at position ");
    sb.append(position);
    sb.append(LS);
    sb.append(s);
    sb.append(LS);
    for (int i = 0; i < position; i++) {
      sb.append(" ");
    }
    sb.append("^");
    sb.append(LS);
    return sb.toString();
  }

  /**
   * A partial, deliberately na&iuml;ve implementation of the {@link
   * ThrowableMatcher} interface. This class is {@code protected} only
   * so that it is visible to templating tools such as <a
   * href="http://mvel.codehaus.org/">MVEL</a>.
   *
   * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
   *
   * @since 1.2-SNAPSHOT
   *
   * @see ThrowableMatcher
   */
  protected static abstract class AbstractThrowableMatcher implements ThrowableMatcher {

    /**
     * The version of this class used by the Java serialization
     * mechanism.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The {@link Throwable} chain being matched.  This field may be
     * {@code null}.
     */
    private Throwable throwableChain;

    /**
     * Creates a new {@link AbstractThrowableMatcher}.
     */
    protected AbstractThrowableMatcher() {
      super();
    }

    /**
     * Sets the {@link Throwable} to be tested.
     *
     * @param throwableChain the {@link Throwable} chain to match; may
     * be {@code null}
     */
    @Override
    public void setThrowable(final Throwable throwableChain) {
      this.throwableChain = throwableChain;
    }

    /**
     * Returns the {@link Throwable} to be tested.  This method may
     * return {@code null}.
     *
     * @return the {@link Throwable} to be tested, or {@code null}
     */
    @Override
    public Throwable getThrowable() {
      return this.throwableChain;
    }

    /**
     * Returns {@code null} when invoked.  Subclasses wishing to store
     * references should override this method.
     *
     * @param key the {@link Object} under which a reference to a
     * {@link Throwable} is expected to be found; may be {@code null};
     * effectively ignored by this implementation
     *
     * @return {@code null} when invoked
     */
    @Override
    public Throwable getReference(final Object key) {
      return null;
    }

    /**
     * Returns the return value of {@link Collections#emptySet()} when
     * invoked.  Subclasses whiching to store references should
     * override this method to return all known keys under which
     * {@link Throwable}s may be located via the {@link
     * #getReference(Object)} method.
     *
     * <p>Subclass implementations of this method may return {@code
     * null}.</p>
     *
     * @return {@link Collections#emptySet()} when invoked; subclasses
     * may override this implementation to return something else,
     * including {@code null}
     */
    @Override
    public Iterable<Object> getReferenceKeys() {
      return Collections.emptySet();
    }

  }

  private static final class ReferenceStoringThrowableMatcher extends AbstractThrowableMatcher {

    private static final long serialVersionUID = 1L;

    private final Object key;

    private ReferenceStoringThrowableMatcher(final Object key) {
      super();
      this.key = key;
    }

    @Override
    public final String getPattern() {
      return "[" + this.key + "]";
    }

    @Override
    public final boolean matches() {
      return this.getThrowable() != null;
    }

    @Override
    public final Throwable getReference(final Object key) {
      if ((key == null && this.key == null) || key.equals(this.key)) {
        return this.getThrowable();
      } else {
        return null;
      }
    }

    @Override
    public final Iterable<Object> getReferenceKeys() {
      if (this.key == null) {
        return Collections.emptySet();
      } else {
        return Collections.singleton(this.key);
      }
    }

  }

  private static final class ThrowableListElementThrowableMatcher extends AbstractThrowableMatcher {

    private static final long serialVersionUID = 1L;

    private int offset;

    private final ThrowableMatcher delegate;

    private ThrowableListElementThrowableMatcher(final int offset, final ThrowableMatcher delegate) {
      super();
      this.offset = offset;
      this.delegate = delegate;
    }

    @Override
    public final void setThrowable(Throwable t) {
      final List<Throwable> list = toList(t);
      assert list != null;
      final int index;
      if (this.offset < 0) {
        // This is quite hackish.  -1 means the last element.  -2 means the next-to-last element.
        index = list.size() + this.offset; // remember, this.offset is negative
      } else {
        index = this.offset;
      }
      if (index >= 0 && index < list.size()) {
        t = list.get(index);
      } else {
        t = null;
      }
      if (this.delegate != null) {
        this.delegate.setThrowable(t);
      }
    }

    @Override
    public final Throwable getThrowable() {
      Throwable t = null;
      if (this.delegate != null) {
        t = this.delegate.getThrowable();
      }
      return t;
    }

    @Override
    public final Throwable getReference(final Object key) {
      Throwable t = null;
      if (this.delegate != null) {
        t = this.delegate.getReference(key);
      }
      return t;
    }

    @Override
    public final Iterable<Object> getReferenceKeys() {
      Iterable<Object> returnValue = null;
      if (this.delegate != null) {
        returnValue = this.delegate.getReferenceKeys();
      }
      if (returnValue == null) {
        returnValue = Collections.emptySet();
      }
      return returnValue;
    }

    private static final List<Throwable> toList(Throwable t) {
      final List<Throwable> returnValue = new ArrayList<Throwable>();
      if (t != null) {
        do {
          returnValue.add(t);
        } while ((t = t.getCause()) != null);
      }
      return returnValue;
    }

    @Override
    public final boolean matches() throws ThrowableMatcherException {
      return this.delegate != null && this.delegate.matches();
    }

    @Override
    public final String getPattern() {
      String returnValue = null;
      if (this.delegate != null) {
        returnValue = this.delegate.getPattern();
      }
      return returnValue;
    }

  }

  private static final class ClassNameEqualityThrowableMatcher extends AbstractThrowableMatcher {

    private static final long serialVersionUID = 1L;

    private final String className;

    private ClassNameEqualityThrowableMatcher(final String className) {
      super();
      this.className = className;
    }

    @Override
    public final boolean matches() {
      final Throwable t = this.getThrowable();
      return t != null && t.getClass().getName().equals(this.className);
    }

    @Override
    public final String getPattern() {
      return this.className;
    }

  }

  private static final class PropertyBlockThrowableMatcher extends AbstractThrowableMatcher {

    private static final long serialVersionUID = 1L;

    private final String propertyBlock;

    private final Serializable expression;

    private PropertyBlockThrowableMatcher(final String propertyBlock) {
      super();
      this.propertyBlock = propertyBlock;
      if (propertyBlock != null) {
        this.expression = MVEL.compileExpression(propertyBlock);
      } else {
        this.expression = null;
      }
    }

    @Override
    public final boolean matches() throws ThrowableMatcherException {
      boolean returnValue = false;
      final Throwable t = this.getThrowable();
      if (t != null && this.expression != null) {
        returnValue = Boolean.TRUE.equals(MVEL.executeExpression(this.expression, t));
      }
      return returnValue;
    }

    @Override
    public final String getPattern() {
      return this.propertyBlock;
    }

  }

  private static final class ConjunctiveThrowableMatcher extends AbstractThrowableMatcher implements Cloneable {

    private static final long serialVersionUID = 1L;

    private List<ThrowableListElementThrowableMatcher> matchers;

    private int indexOfFirstNegativeMatcher;

    private final String pattern;

    private ConjunctiveThrowableMatcher(final String pattern) {
      super();
      if (pattern == null) {
        throw new IllegalArgumentException("pattern", new NullPointerException("pattern == null"));
      }
      this.pattern = pattern;
      this.indexOfFirstNegativeMatcher = -1;
      this.matchers = new ArrayList<ThrowableListElementThrowableMatcher>();
    }

    @Override
    public final Throwable getReference(final Object key) {
      Throwable returnValue = null;
      if (this.matchers != null && !this.matchers.isEmpty()) {
        for (final ThrowableMatcher m : this.matchers) {
          if (m != null) {
            final Throwable t = m.getReference(key);
            if (t != null) {
              returnValue = t;
              break;
            }
          }
        }
      }
      return returnValue;
    }

    @Override
    public final Iterable<Object> getReferenceKeys() {
      Set<Object> returnValue = null;
      if (this.matchers != null && !this.matchers.isEmpty()) {
        returnValue = new LinkedHashSet<Object>();
        for (final ThrowableMatcher m : this.matchers) {
          if (m != null) {
            final Iterable<Object> keys = m.getReferenceKeys();
            if (keys != null) {
              for (final Object key : keys) {
                if (key != null) {
                  returnValue.add(key);
                }
              }
            }
          }
        }
      }
      if (returnValue == null) {
        returnValue = Collections.emptySet();
      }
      return returnValue;
    }

    @Override
    public final void setThrowable(final Throwable throwableChain) {
      super.setThrowable(throwableChain);
      if (this.matchers != null && !this.matchers.isEmpty()) {
        for (final ThrowableListElementThrowableMatcher m : this.matchers) {
          if (m != null) {
            m.setThrowable(throwableChain);
          }
        }
      }
    }

    @Override
    public final String getPattern() {
      return this.pattern;
    }

    private final void clear() {
      this.matchers.clear();
    }

    private final void add(final ThrowableListElementThrowableMatcher p) {
      assert this.matchers != null;
      if (p != null) {
        this.matchers.add(p);

        if (p.offset < 0) {
          if (this.indexOfFirstNegativeMatcher < 0) {
            this.indexOfFirstNegativeMatcher = this.matchers.size() - 1;
          }
          int offsetToAssign = -(this.matchers.size() - this.indexOfFirstNegativeMatcher);
          for (int i = this.indexOfFirstNegativeMatcher; i < this.matchers.size(); i++) {
            final ThrowableListElementThrowableMatcher matcher = this.matchers.get(i);
            matcher.offset = offsetToAssign++;
          }
        }

      }
    }

    @Override
    public final boolean matches() throws ThrowableMatcherException {
      boolean returnValue = false;
      if (this.matchers != null && !this.matchers.isEmpty()) {
        for (final ThrowableMatcher matcher : this.matchers) {
          returnValue = true;
          if (matcher == null || !matcher.matches()) {
            returnValue = false;
            break;
          }
        }
      }
      return returnValue;
    }

    @Override
    public final ConjunctiveThrowableMatcher clone() {
      try {
        final ConjunctiveThrowableMatcher superClone = (ConjunctiveThrowableMatcher)super.clone();
        assert superClone != null;
        superClone.matchers = new ArrayList<ThrowableListElementThrowableMatcher>(this.matchers);
        return superClone;
      } catch (final CloneNotSupportedException wontHappen) {
        throw (InternalError)new InternalError().initCause(wontHappen);
      }
    }

  }

  /**
   * A {@link ThrowableMatcher} that determines whether a {@link Throwable}
   * is an instance of a given {@link Class}.
   *
   * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
   */
  private static final class InstanceOfThrowableMatcher extends AbstractThrowableMatcher {

    /**
     * The version number of the serialized representation of this
     * class.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 1L;

    /**
     * The {@link Class} to use for the test.
     *
     * <p>This field may be {@code null}.</p>
     */
    private final Class<? extends Throwable> cls;

    /**
     * Creates a new {@link InstanceOfThrowableMatcher}.
     *
     * @param cls the {@link Class} to use for the matcher test; may
     * be {@code null}
     */
    private InstanceOfThrowableMatcher(final Class<? extends Throwable> cls) {
      super();
      this.cls = cls;
    }

    @Override
    public final boolean matches() {
      final Throwable t = this.getThrowable();
      return t != null && this.cls != null && this.cls.isInstance(t);
    }

    @Override
    public final String getPattern() {
      if (this.cls == null) {
        return null;
      }
      return String.format("%s...", this.cls.getName());
    }

    @Override
    public final String toString() {
      return (this.cls == null ? "null" : this.cls.getName()) + "...";
    }

  }

  private static final class ParsingState extends Reader {

    private boolean leftAnchor;

    private boolean rightAnchor;

    private int position;

    private int periodCount;

    private final StringBuilder classNameBuffer;

    private final StringBuilder commentBuffer;

    private final StringBuilder propertyBlockBuffer;

    private final StringBuilder referenceBuffer;

    private boolean greedyGlob;

    private int depthLevel;

    private final ConjunctiveThrowableMatcher matchers;

    private State state;

    private State priorState;

    private int character;

    private final String pattern;

    private final Reader reader;

    private ParsingState(final String pattern) {
      super();
      if (pattern == null) {
        throw new IllegalArgumentException("pattern", new NullPointerException("pattern == null"));
      }
      this.pattern = pattern;
      this.leftAnchor = true; // XXX TODO FIXME for now
      this.rightAnchor = false;
      this.reader = new StringReader(pattern);
      this.state = State.START;
      this.classNameBuffer = new StringBuilder();
      this.commentBuffer = new StringBuilder();
      this.propertyBlockBuffer = new StringBuilder();
      this.referenceBuffer = new StringBuilder();
      this.matchers = new ConjunctiveThrowableMatcher(pattern);
    }

    @Override
    public final int read() throws IOException {
      final int returnValue = this.reader.read();
      this.character = returnValue;
      return returnValue;
    }

    @Override
    public final int read(final char[] buffer) throws IOException {
      return this.reader.read(buffer);
    }

    @Override
    public final int read(final char[] buffer, final int offset, final int length) throws IOException {
      return this.reader.read(buffer, offset, length);
    }

    @Override
    public final int read(final CharBuffer buffer) throws IOException {
      return this.reader.read(buffer);
    }

    @Override
    public final boolean ready() throws IOException {
      return this.reader.ready();
    }

    @Override
    public final long skip(final long characterCount) throws IOException {
      return this.reader.skip(characterCount);
    }

    @Override
    public final void reset() throws IOException {
      this.reader.reset();
    }

    @Override
    public final boolean markSupported() {
      return this.reader.markSupported();
    }

    @Override
    public final void mark(final int readAheadLimit) throws IOException {
      this.reader.mark(readAheadLimit);
    }

    @Override
    public final void close() throws IOException {
      this.reader.close();
    }

  }

}