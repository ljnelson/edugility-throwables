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

import java.io.StringReader;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.edugility.throwables.Throwables.Predicate;

import org.mvel2.MVEL;

public class ThrowablePattern {

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

  private final StringBuilder classNameBuffer;

  private final StringBuilder commentBuffer;

  private final StringBuilder propertyBlockBuffer;

  private boolean greedyGlob;

  private int depthLevel;

  private final ConjunctiveThrowableMatcher matchers;

  public ThrowablePattern() {
    super();
    this.classNameBuffer = new StringBuilder();
    this.commentBuffer = new StringBuilder();
    this.propertyBlockBuffer = new StringBuilder();
    this.matchers = new ConjunctiveThrowableMatcher();
  }

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

  private final void newElementMatcher(final ThrowableMatcher delegate) {
    final ThrowableListElementThrowableMatcher matcher = new ThrowableListElementThrowableMatcher(this.depthLevel, delegate);
    this.matchers.add(matcher);
  }

  @SuppressWarnings("unchecked")
  private void subclassTest() throws ClassNotFoundException {
    newElementMatcher(new InstanceOfThrowableMatcher(this.loadClass(this.classNameBuffer.toString())));
  }

  private void classNameMatchTest() {
    newElementMatcher(new ClassNameEqualityThrowableMatcher(this.classNameBuffer.toString()));
  }

  private void ellipsis() {
    
  }

  private void identifierStart() {    
    this.classNameBuffer.setLength(0);
  }

  private void identifierEnd() {
    
  }

  private void slash() {
    this.slash(true);
  }

  private void slash(final boolean adjustDepth) {
    if (adjustDepth) {
      if (this.greedyGlob) {
        this.depthLevel--;
      } else {
        this.depthLevel++;
      }
    }
  }

  private void identifier(final int c) {    
    this.classNameBuffer.append((char)c);
  }

  private void identifier(final String s) {
    this.classNameBuffer.append(s);
  }

  private void glob() {
    this.identifierStart();
    this.identifier("java.lang.Throwable");
  }

  private void greedyGlob() {
    this.greedyGlob = true;
    this.depthLevel = 0;
  }

  private void commentStart() {
    this.commentBuffer.setLength(0);
  }

  private void comment(final int c) {    
    this.commentBuffer.append((char)c);
  }

  private void commentEnd() {

  }

  private void propertyBlockStart() {
    this.propertyBlockBuffer.setLength(0);
  }

  private void propertyBlock(final int c) {    
    this.propertyBlockBuffer.append((char)c);
  }

  private void propertyBlockEnd() {
    // TODO: investigate how to do this in the presence of a greedyGlob
    //final ThrowableMatcher matcher = new PropertyBlockThrowableMatcher(this.propertyBlockBuffer.toString());
    newElementMatcher(new PropertyBlockThrowableMatcher(this.propertyBlockBuffer.toString()));
    // this.matchers.add(matcher);
  }

  private void start() {
    this.depthLevel = 0;
    this.greedyGlob = false;
    this.clearBuffers();
    this.matchers.clear();
  }

  private final void clearBuffers() {
    this.propertyBlockBuffer.setLength(0);
    this.classNameBuffer.setLength(0);
    this.commentBuffer.setLength(0);
  }

  /**
   * Compiles the supplied pattern into a new {@link
   * ThrowableMatcher}.  This method never returns {@code null}.
   * 
   * <pre>
   *
   * Pattern = PatternStart ('/' PatternBody)?
   *
   * PatternStart = '/'? ( GreedyGlob | ClassTest )
   * 
   * PatternBody = ClassTest ( '/' ( GreedyGlob | ClassTest ) )*
   *
   * ClassTest = ClassName Ellipsis? PropertyBlock?
   *
   * PropertyBlock = '(' <i>MVEL expression</i> ')'
   *
   * Ellipsis = \u2026 | ...
   *
   * ClassName = Fully qualified Java class name
   *
   * </pre>
   */
  public ThrowableMatcher newThrowableMatcher(final String pattern) throws ClassNotFoundException, IOException {
    if (pattern == null) {
      throw new IllegalArgumentException("pattern", new NullPointerException("pattern == null"));
    }

    final StringReader reader = new StringReader(pattern);

    State state = State.START;

    State priorState = null;

    int periodCount = 0;
    int c;
    for (int pos = 0; (c = reader.read()) != -1; pos++) {
      final State originalState = state;

      switch (state) {


        // START
      case START:
        
        if (Character.isWhitespace(c)) {
          // Eat whitespace
          break;
        }

        switch (c) {

        case '/':
          start();
          slash(false);
          state = State.NORMAL;
          break;

        case '#':
          start();
          state = State.COMMENT;
          commentStart();
          break;

        case '*':
          start();
          state = State.INDETERMINATE_GLOB;
          break;

        default:
          start();
          if (!Character.isJavaIdentifierStart(c)) {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
          }
          state = State.IDENTIFIER;
          identifierStart();
          identifier((char)c);
        }
        break;
        // end START


        // NORMAL
      case NORMAL:

        if (Character.isWhitespace(c)) {
          // Eat whitespace
          break;
        }

        switch (c) {

        case '#':
          state = State.COMMENT;
          commentStart();
          break;

        case '*':
          state = State.INDETERMINATE_GLOB;
          break;

        case '/':
          slash();
          break;

        default:
          if (!Character.isJavaIdentifierStart(c)) {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
          }
          state = State.IDENTIFIER;
          identifierStart();
          identifier((char)c);
        }
        break;
        // end NORMAL


        // COMMENT
      case COMMENT:
        switch (c) {

        case '\r':
        case '\n':
          commentEnd();
          state = State.NORMAL;
          break;

        default:
          comment(c);

        }
        break;
        // end COMMENT


        // INDETERMINATE_PERIOD
      case INDETERMINATE_PERIOD:
        assert priorState == State.IDENTIFIER || priorState == State.INDETERMINATE_PERIOD : "Prior state was not " + State.IDENTIFIER + " and was not " + State.INDETERMINATE_PERIOD + ": " + priorState;
        switch (c) {

        case '.':
          periodCount++;
          assert periodCount > 0 : "periodCount <= 0: " + periodCount;
          assert periodCount <= 3 : "periodCount > 3: " + periodCount;
          if (periodCount == 3) {
            periodCount = 0;
            // Our current state is INDETERMINATE_PERIOD, but now we
            // know we just ran into an ellipsis.  That means our
            // prior prior prior state was IDENTIFIER.  That means we
            // just ended an identifier.
            identifierEnd();
            state = State.ELLIPSIS;
            ellipsis();
            subclassTest();
          }
          break;

        default:
          periodCount = 0;
          if (Character.isJavaIdentifierStart(c)) { // yes, start, not part: each portion of a package name is an Identifier, so must begin with an IdentifierStart.
            state = State.IDENTIFIER;
            identifier('.');
            identifier(c);
          } else {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
          }
        }
        break;
        // end INDETERMINATE_PERIOD


        // INDETERMINATE_GLOB
      case INDETERMINATE_GLOB:
        if (Character.isWhitespace(c) || c == '/') {
          state = State.GLOB;
          glob();
          subclassTest();
          state = State.NORMAL;
          break;
        }
        switch (c) {

        case '*':
          if (this.greedyGlob) {
            // we already found one! This is illegal.
            throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
          } else {
            state = State.GREEDY_GLOB;
            greedyGlob();
          }
          break;

        default:
          throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
        }
        break;
        // end INDETERMINATE_GLOB


        // GREEDY_GLOB
      case GREEDY_GLOB:
        if (Character.isWhitespace(c)) {
          // Eat whitespace
          break;
        }
        switch (c) {

        case '/':
          slash();
          state = State.NORMAL;
          break;

        default:
          throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
        }
        break;
        // end GREEDY GLOB


        // IDENTIFIER
      case IDENTIFIER:
        
        if (Character.isWhitespace(c)) {
          // End the identifier, eat the whitespace, and reset the state
          identifierEnd();
          classNameMatchTest();
          state = State.NORMAL;
          break;
        }

        switch (c) {

        case '.':
          periodCount++;
          state = State.INDETERMINATE_PERIOD;
          break;

        case '\u2026':
          identifierEnd();
          state = State.ELLIPSIS;
          ellipsis();
          subclassTest();
          break;

        case '/':
          identifierEnd();
          classNameMatchTest();
          slash();
          state = State.NORMAL;
          break;

        case '(':
          identifierEnd();
          classNameMatchTest();
          state = State.PROPERTY_BLOCK;
          propertyBlockStart();
          break;

        default:
          if (!Character.isJavaIdentifierPart(c)) {
            throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
          }
          identifier(c);
        }
        break;
        // end IDENTIFIER


        // ELLIPSIS
      case ELLIPSIS:
        if (Character.isWhitespace(c)) {
          // Eat whitespace
          break;
        }
        switch (c) {
        case '/':
          slash();
          state = State.NORMAL;
          break;

        case '(':
          state = State.PROPERTY_BLOCK;
          propertyBlockStart();
          break;

        case '#':
          state = State.COMMENT;          
          commentStart();
          break;          

        default:
          state = State.NORMAL;
        }
        break;
        // end ELLIPSIS


        // PROPERTY_BLOCK
      case PROPERTY_BLOCK:
        switch (c) {

        case ')':
          propertyBlockEnd();
          state = State.NORMAL;
          break;

        default:
          propertyBlock(c);
        }
        break;
        // end PROPERTY_BLOCK


      default:
        throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pos, state));
      }

      priorState = originalState;
    }

    switch (priorState) {
    case PROPERTY_BLOCK:
      // propertyBlockEnd(); // this has been taken care of already
      break;
    case COMMENT:
      commentEnd();
      break;
    case IDENTIFIER:
      identifierEnd();
      classNameMatchTest();
      break;
    case INDETERMINATE_PERIOD:
      if (periodCount == 0) {
        // everything is AOK
      } else {
        throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pattern.length() - 1, priorState));
      }
      break;
    case NORMAL:
      break;      
    default:
      throw new IllegalStateException(buildIllegalStateExceptionMessage(pattern, pattern.length() - 1, priorState));
    }

    return (ThrowableMatcher)this.matchers.clone();
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

  }

  private static final class PropertyBlockThrowableMatcher implements ThrowableMatcher {
    
    private static final long serialVersionUID = 1L;

    private final Serializable expression;

    private PropertyBlockThrowableMatcher(final String propertyBlock) {
      super();
      this.expression = MVEL.compileExpression(propertyBlock);
    }

    @Override
    public final boolean matches(final Throwable t) {
      boolean returnValue = false;
      if (t != null) {
        returnValue = Boolean.TRUE.equals(MVEL.executeExpression(this.expression, t));
      }
      return returnValue;
    }

  }

  private static final class ConjunctiveThrowableMatcher implements ThrowableMatcher, Cloneable {

    private static final long serialVersionUID = 1L;

    private List<ThrowableListElementThrowableMatcher> matchers;

    private int indexOfFirstNegativeMatcher;

    private ConjunctiveThrowableMatcher() {
      super();
      this.indexOfFirstNegativeMatcher = -1;
      this.matchers = new ArrayList<ThrowableListElementThrowableMatcher>();
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
    public final String toString() {
      return (this.cls == null ? "null" : this.cls.getName()) + "...";
    }

  }

  private final class MutableInteger extends Number implements Comparable<Number> {

    private static final long serialVersionUID = 1L;

    private int value;

    private MutableInteger(final int initialValue) {
      super();
      this.value = initialValue;
    }

    private final void setValue(final int value) {
      this.value = value;
    }

    @Override
    public final int intValue() {
      return this.value;
    }

    @Override
    public final long longValue() {
      return this.value;
    }

    @Override
    public final float floatValue() {
      return (float)this.value;
    }

    @Override
    public final double doubleValue() {
      return (double)this.value;
    }

    @Override
    public final short shortValue() {
      return (short)this.value;
    }

    @Override
    public final byte byteValue() {
      return (byte)this.value;
    }

    @Override
    public final int compareTo(final Number other) {
      int hisValue = 0;
      if (other != null) {
        hisValue = other.intValue();
      }
      if (this.value == hisValue) {
        return 0;
      } else if (this.value < hisValue) {
        return -1;
      } else {
        return 1;
      }
    }

    @Override
    public final int hashCode() {
      return this.value;
    }

    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other instanceof Number) {
        return this.value == ((Number)other).intValue();
      } else {
        return false;
      }
    }
    
    @Override
    public final String toString() {
      return String.valueOf(this.value);
    }

  }

}