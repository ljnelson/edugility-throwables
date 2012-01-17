/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * $Id$
 *
 * Copyright (c) 2010-2011 Edugility LLC.
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

import java.sql.SQLException; // for javadoc only

import java.util.ArrayList;
import java.util.List;

import org.mvel2.MVEL;

/**
 * A pattern that can match {@link Throwable} instances.
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @see ThrowableMatcher
 *
 * @see #newThrowableMatcher(String)
 *
 * @since 1.2-SNAPSHOT
 */
public class ThrowablePattern implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private static final String LS = System.getProperty("line.separator", "\n");

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
    PROPERTY_BLOCK
  }

  /**
   * Creates a new {@link ThrowablePattern}.
   */
  public ThrowablePattern() {
    super();
  }

  /**
   * Loads the {@link Class} named by the supplied classname.  This
   * implementation first attempts to use the {@linkplain
   * Thread#getContextClassLoader() context <tt>ClassLoader</tt>}, and
   * then uses the {@link ClassLoader} returned by {@link
   * Class#getClassLoader() Throwable.class.getClassLoader()}.
   *
   * @return the loaded {@link Class}; never {@code null}
   *
   * @exception ClassNotFoundException if the {@link Class} could not
   * be loaded
   */
  protected Class<? extends Throwable> loadClass(final String name) throws ClassNotFoundException {
    Class<?> c = null;
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader != null) {
      c = loader.loadClass(name);
    } else {
      loader = Throwable.class.getClassLoader();
      c = loader.loadClass(name);
    }
    @SuppressWarnings("unchecked")
    final Class<? extends Throwable> returnValue = (Class<? extends Throwable>)c;
    return returnValue;
  }

  private static final void newElementMatcher(final ParsingState parsingState, final ThrowableMatcher delegate) {
    parsingState.matchers.add(new ThrowableListElementThrowableMatcher(parsingState.depthLevel, delegate));
  }

  private final void subclassTest(final ParsingState parsingState) throws ClassNotFoundException {
    newElementMatcher(parsingState, new InstanceOfThrowableMatcher(this.loadClass(parsingState.classNameBuffer.toString())));
  }

  private static final void classNameMatchTest(final ParsingState parsingState) {
    newElementMatcher(parsingState, new ClassNameEqualityThrowableMatcher(parsingState.classNameBuffer.toString()));
  }

  private static final void ellipsis(final ParsingState parsingState) {

  }

  private static final void identifierStart(final ParsingState parsingState) {    
    parsingState.classNameBuffer.setLength(0);
  }

  private static final void identifierEnd(final ParsingState parsingState) {
    
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

  private static final void identifier(final ParsingState parsingState, final int c) {    
    parsingState.classNameBuffer.append((char)c);
  }

  private static final void identifier(final ParsingState parsingState, final String s) {
    parsingState.classNameBuffer.append(s);
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

  /**
   * Compiles the supplied pattern into a new {@link
   * ThrowableMatcher}.  This method never returns {@code null}.
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
   * ClassTest = ( ClassName Ellipsis? PropertyBlock? ) | Glob
   *
   * Glob = '<b>*</b>'
   *
   * PropertyBlock = '<b>(</b>' <i><a href="http://mvel.codehaus.org/">MVEL</a> expression</i> '<b>)</b>'
   *
   * Ellipsis = '<b>\u2026</b>' | '<b>...</b>'
   *
   * ClassName = <i>Fully qualified Java class name</i>
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
  public ThrowableMatcher newThrowableMatcher(final String pattern) throws ClassNotFoundException, IOException {
    if (pattern == null) {
      throw new IllegalArgumentException("pattern", new NullPointerException("pattern == null"));
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

        case '/':
          start(parsingState);
          slash(parsingState, false);
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
            subclassTest(parsingState);
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
          subclassTest(parsingState);
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
          subclassTest(parsingState);
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

    return (ThrowableMatcher)parsingState.matchers.clone();
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

  private static final class ThrowableListElementThrowableMatcher implements ThrowableMatcher {

    private static final long serialVersionUID = 1L;

    private int offset;

    private final ThrowableMatcher delegate;

    private ThrowableListElementThrowableMatcher(final int offset, final ThrowableMatcher delegate) {
      super();
      this.offset = offset;
      this.delegate = delegate;
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
    public final boolean matches(final Throwable t) throws ThrowableMatcherException {
      boolean returnValue = false;
      final List<Throwable> list = toList(t);
      assert list != null;
      final int index;
      if (this.offset < 0) {
        // This is quite hackish.  -1 means the last element.  -2 means the next-to-last element.
        index = list.size() + this.offset; // remember, this.offset is negative
      } else {
        index = this.offset;
      }
      if (this.delegate != null && index >= 0 && index < list.size()) {
        returnValue = this.delegate.matches(list.get(index));
      }
      return returnValue;
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

  private static final class ClassNameEqualityThrowableMatcher implements ThrowableMatcher {
    
    private static final long serialVersionUID = 1L;

    private final String className;

    private ClassNameEqualityThrowableMatcher(final String className) {
      super();
      this.className = className;
    }

    @Override
    public final boolean matches(final Throwable t) {
      return t != null && t.getClass().getName().equals(this.className);
    }

    @Override
    public final String getPattern() {
      return this.className;
    }

  }

  private static final class PropertyBlockThrowableMatcher implements ThrowableMatcher {
    
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
    public final boolean matches(final Throwable t) {
      boolean returnValue = false;
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

  private static final class ConjunctiveThrowableMatcher implements ThrowableMatcher, Cloneable {

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
    public final boolean matches(final Throwable t) throws ThrowableMatcherException {
      boolean returnValue = false;
      for (final ThrowableMatcher p : this.matchers) {
        returnValue = true;
        if (p == null || !p.matches(t)) {
          returnValue = false;
          break;
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
  private static final class InstanceOfThrowableMatcher implements ThrowableMatcher {

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

    /**
     * Returns {@code true} if the supplied {@link Throwable} is an
     * instance of the {@link Class} supplied to this {@link
     * InstanceOfThrowableMatcher}'s {@linkplain
     * #Throwables.InstanceOfThrowableMatcher(Class) constructor}.
     *
     * @param t the {@link Throwable} to test; may be {@code null}
     *
     * @return {@code true} if the supplied {@link Throwable} is an
     * instance of the {@link Class} supplied to this {@link
     * InstanceOfThrowableMatcher}'s {@linkplain
     * #Throwables.InstanceOfThrowableMatcher(Class) constructor}; {@code
     * false} in all other cases
     */
    @Override
    public final boolean matches(final Throwable t) {
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

    private int position;

    private int periodCount;

    private final StringBuilder classNameBuffer;

    private final StringBuilder commentBuffer;
    
    private final StringBuilder propertyBlockBuffer;

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
      this.reader = new StringReader(pattern);
      this.state = State.START;
      this.classNameBuffer = new StringBuilder();
      this.commentBuffer = new StringBuilder();
      this.propertyBlockBuffer = new StringBuilder();
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