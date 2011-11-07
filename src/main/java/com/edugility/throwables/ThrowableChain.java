/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * Copyright (c) 2011 Edugility LLC.
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

import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An {@link Exception} that is also holds a modifiable list of other
 * {@link Throwable}s that are not connected to the direct {@linkplain
 * Throwable#getCause() causal chain}, but are affiliated nonetheless.
 * Instances of this class are particularly useful when dealing with
 * {@link Throwable}s in {@code finally} blocks.
 *
 * <p>A {@link ThrowableChain} always contains itself, so the return
 * value of its {@link #size()} method is always at least {@code
 * 1}.</p>
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.0
 */
public class ThrowableChain extends Exception implements Collection<Throwable> {

  /**
   * The {@link List} containing additional {@link Throwable}s.  This
   * field is never {@code null} and never {@linkplain List#isEmpty()
   * empty}.
   */
  private final CopyOnWriteArrayList<Throwable> list;

  /**
   * Creates a new {@link ThrowableChain}.
   */
  public ThrowableChain() {
    this(null, null);
  }

  /**
   * Creates a new {@link ThrowableChain} with the supplied {@code
   * message} and cause.
   *
   * @param message the message; may be {@code null}
   *
   * @param cause the cause; may be {@code null}
   */
  public ThrowableChain(final String message, final Throwable cause) {
    super(message);
    this.list = new CopyOnWriteArrayList<Throwable>();
    this.list.add(this);
    if (cause != null) {
      this.initCause(cause);
    }
  }

  /**
   * Creates a new {@link ThrowableChain} with the supplied {@code
   * message}.
   *
   * @param message the message; may be {@code null}
   */
  public ThrowableChain(final String message) {
    this(message, null);
  }

  /**
   * Creates a new {@link ThrowableChain} with the supplied {@code
   * cause} and cause.
   *
   * @param cause the cause; may be {@code null}
   */
  public ThrowableChain(final Throwable cause) {
    this(null, cause);
  }

  /**
   * Adds the supplied {@link Throwable} to this {@link
   * ThrowableChain} if it is non-{@code null} and not this {@link
   * ThrowableChain} (a {@link ThrowableChain} cannot add itself to
   * itself), or initializes this {@link ThrowableChain}'s {@linkplain
   * #getCause() cause} with the supplied {@link Throwable} if the
   * cause has not yet been {@linkplain Throwable#initCause(Throwable)
   * initialized}.
   *
   * <p>If this {@link ThrowableChain}'s {@linkplain #getCause()
   * cause} is {@code null}, then this {@link ThrowableChain}'s
   * {@linkplain #initCause(Throwable) cause is initialized} to the
   * supplied {@link Throwable}.  Otherwise, the supplied {@link
   * Throwable} is added to this {@link ThrowableChain}'s {@linkplain
   * #asList() list of affiliated <tt>Throwable</tt>s}.</p>
   *
   * <p>Under no circumstances are the supplied {@link Throwable}'s
   * {@linkplain Throwable#getCause() cause} or any transitive causes
   * added.</p>
   *
   * <p>If the supplied {@link Throwable} is already contained in this
   * {@link ThrowableChain}'s {@linkplain #asList() list of affiliated
   * <tt>Throwable</tt>s}, then no action is taken.</p>
   *
   * @param throwable the {@link Throwable} to add; may be {@code
   * null} in which case no action will be taken
   *
   * @return {@code true} if the supplied {@link Throwable} was
   * actually added; {@code false} in all other cases
   */
  @Override
  public final boolean add(final Throwable throwable) {
    boolean returnValue = false;
    assert this.list != null;
    if (throwable != null && throwable != this) {
      final Throwable cause = this.getCause();
      if (throwable != cause) {
        if (cause == null) {
          this.initCause(throwable);
        } else {
          returnValue = this.list.addIfAbsent(throwable);
        }
      }
    }
    return returnValue;
  }

  /**
   * Adds every non-{@code null} element contained by the supplied
   * {@link Collection} of {@link Throwable}s to this {@link
   * ThrowableChain}.
   *
   * @param c the {@link Collection} of {@link Throwable}s; may be
   * {@code null} in which case no action is taken
   *
   * @return {@code true} if at least one element was actually added
   */
  @Override
  public final boolean addAll(final Collection<? extends Throwable> c) {
    boolean returnValue = false;
    if (c != null && !c.isEmpty()) {
      for (final Throwable t : c) {
        if (t != null) {
          returnValue = returnValue || this.add(t);
        }
      }
    }
    return returnValue;
  }

  /**
   * Removes the first instance of the supplied {@link Throwable} from
   * this {@link ThrowableChain}'s list of affiliated {@link
   * Throwable}s, provided that it is not {@code null} or this {@link
   * ThrowableChain} itself.
   *
   * @param throwable the affiliated {@link Throwable} to remove
   * provided it meets the conditions described
   *
   * @return {@code true} if this {@link ThrowableChain} actually
   * removed the supplied {@link Throwable}; {@code false} otherwise
   *
   * @exception UnsupportedOperationException if {@code throwable} is
   * this {@link ThrowableChain}
   */
  @Override
  public final boolean remove(final Object throwable) {
    if (throwable == this) {
      throw new UnsupportedOperationException(new IllegalArgumentException("Cannot remove this ThrowableChain from itself"));
    }
    return this.list.remove(throwable);
  }

  /**
   * Causes this {@link ThrowableChain} to keep all of the elements
   * found in the supplied {@link Collection} and to discard the rest.
   *
   * @param c the {@link Collection} containing the elements to
   * retain; must not be {@code null} or {@linkplain
   * Collection#isEmpty() empty}; must contain this {@link
   * ThrowableChain} (because it is impossible for a {@link
   * ThrowableChain} to discard itself)
   *
   * @return {@code true} if the contents of this {@link
   * ThrowableChain} were actually affected by this method invocation
   *
   * @exception UnsupportedOperationException if {@code c} is {@code
   * null} or {@linkplain Collection#isEmpty() empty}, or if it does
   * <em>not</em> contain this {@link ThrowableChain} (a {@link
   * ThrowableChain} must always contain itself as an element)
   *
   * @see #size()
   */
  @Override
  public final boolean retainAll(final Collection<?> c) {
    if (c == null || c.isEmpty()) {
      throw new UnsupportedOperationException(new IllegalArgumentException("Cannot call retainAll() with a null or empty Collection"));
    }
    if (!c.contains(this)) {
      throw new UnsupportedOperationException(new IllegalArgumentException("Cannot effectively remove this ThrowableChain"));
    }
    return this.list.retainAll(c);
  }

  /**
   * Removes all elements contained by the supplied {@link Collection}
   * found in this {@link ThrowableChain}.
   *
   * @param c the {@link Collection}; may be {@code null} but, if
   * non-{@code null}, must not {@linkplain
   * Collection#contains(Object) contain} this {@link ThrowableChain}
   *
   * @return {@code true} if this {@link ThrowableChain} was changed
   * as a result of an invocation of this method
   *
   * @exception UnsupportedOperationException if the supplied {@link
   * Collection} is non-{@code null} and contains this {@link
   * ThrowableChain}
   */
  @Override
  public final boolean removeAll(final Collection<?> c) {
    if (c == null || c.isEmpty()) {
      return false;
    } else if (c.contains(this)) {
      throw new UnsupportedOperationException(new IllegalArgumentException("Cannot call removeAll() with a Collection that contains this ThrowableChain"));
    } else {
      return this.list.removeAll(c);
    }
  }
  
  /**
   * Throws an {@link UnsupportedOperationException}.
   *
   * @exception UnsupportedOperationException if invoked
   */
  @Override
  public final void clear() {
    throw new UnsupportedOperationException("clear is unsupported because a ThrowableChain always has itself as its first element");
  }

  /**
   * Returns {@code true} if this {@link ThrowableChain} contains the
   * supplied {@link Object}.
   *
   * @param o the {@link Object} to look for; may be {@code null}
   * 
   * @return {@code true} if this {@link ThrowableChain} contains the
   * supplied {@link Object}
   */
  @Override
  public final boolean contains(final Object o) {
    return o == this || (o != null && this.list.contains(o));
  }

  /**
   * Returns {@code true} if all the elements of the supplied {@link
   * Collection} are contained by this {@link ThrowableChain}.
   *
   * @param stuff the {@link Collection} to test; may be {@code null}
   *
   * @return {@code true} if all the elements of the supplied {@link
   * Collection} are contained by this {@link ThrowableChain}
   */
  @Override
  public final boolean containsAll(final Collection<?> stuff) {
    boolean returnValue = stuff == this;
    if (!returnValue && stuff != null && !stuff.isEmpty()) {
      returnValue = this.list.containsAll(stuff);
    }
    return returnValue;
  }

  /**
   * Returns {@code false}.
   *
   * @return {@code false} in all cases
   */
  @Override
  public final boolean isEmpty() {
    return false;
  }

  /**
   * Returns a new {@link Object} array of this {@link
   * ThrowableChain}'s contents.  This method never returns {@code
   * null}.
   *
   * @return a new {@link Object} array of this {@link
   * ThrowableChain}'s contents; never {@code null}
   *
   * @see Collection#toArray()
   */
  @Override
  public final Object[] toArray() {
    return this.list.toArray();
  }

  /**
   * Attempts to fill and return the supplied array with the contents
   * of this {@link ThrowableChain}.  If the contents of this {@link
   * ThrowableChain} will not fit into the supplied array, a new array
   * will be allocated.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param a the array to fill and return if possible; must not be
   * {@code null}
   *
   * @return the contents of this {@link ThrowableChain} as an array;
   * never {@code null}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   *
   * @see Collection#toArray(Object[])
   */
  @Override
  public final <T> T[] toArray(final T[] a) {
    return this.list.toArray(a);
  }

  /**
   * Returns a new {@linkplain Collections#unmodifiableList(List)
   * unmodifiable view} of this {@link ThrowableChain}.  The returned
   * {@link List} is non-{@code null}, is non-{@linkplain
   * Collection#isEmpty() empty}, safe for iteration by multiple
   * threads without synchronization or locking, contains this {@link
   * ThrowableChain} itself as the first element, and is behaviorally
   * identical to the {@link List} instances returned by the {@link
   * Collections#unmodifiableList(List)} method.
   *
   * @return a read-only {@link List} view of this {@link
   * ThrowableChain} and the underlying list of affiliated {@link
   * Throwable}s; never {@code null}
   */
  public final List<Throwable> asList() {
    return Collections.unmodifiableList(this.list);
  }

  /**
   * Returns a new {@link List} of this {@link ThrowableChain}'s
   * affiliate {@link Throwable}s.  The returned {@link List} is
   * guaranteed to be non-{@code null}, to be safe for iteration by
   * multiple threads without synchronization or locking, to be
   * behaviorally identical to the {@link List} instances returned by
   * the {@link Collections#unmodifiableList(List)} and to not contain
   * this {@link ThrowableChain}.
   *
   * @return an immutable {@link List} of this {@link
   * ThrowableChain}'s affiliates; never {@code null}
   */
  public final List<Throwable> getAffiliatedThrowables() {
    final int size = this.size();
    if (size < 1) {
      // Protect against bad size() overrides.
      throw new IllegalStateException(String.format("this.size() < 1: %d", size));
    } else if (size == 1) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(this.list.subList(1, size));
    }
  }

  /**
   * Returns the size of this {@link ThrowableChain}.  A {@link
   * ThrowableChain} always has a size of at least {@code 1}.
   *
   * @return the size of this {@link ThrowableChain}&mdash;a positive
   * integer greater than or equal to {@code 1}
   */
  @Override
  public int size() {
    return this.list.size();
  }

  /**
   * Returns an {@link Iterator} that can be used to iterate over all
   * {@linkplain #add(Throwable) contained <tt>Throwable</tt>s}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The {@link Iterator} returned does <em>not</em> iterate over
   * any given {@link Throwable}'s {@linkplain Throwable#getCause()
   * causal chain}.</p>
   *
   * @return an {@link Iterator}; never {@code null}; the first
   * element of the {@link Iterator} will always be this {@link
   * ThrowableChain}
   *
   * @see #asList()
   */
  @Override
  public final Iterator<Throwable> iterator() {
    return this.asList().iterator();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Prints the stack trace of this {@link ThrowableChain} and then
   * of every {@linkplain #iterator() <tt>Throwable</tt> contained by
   * it}.  Each stack trace is preceded by the following text (quoted
   * here; the quotation marks are not part of the text):
   * "<tt><i>d</i>. </tt>" <i>d</i> in the preceding text fragment is
   * substituted with the ordinal position, starting with {@code 1},
   * of the {@link Throwable} in question.</p>
   *
   * @param s the {@link PrintStream} to print to; must not be {@code
   * null}
   */
  @Override
  public void printStackTrace(final PrintStream s) {
    if (s != null) {
      final int size = this.size();
      if (size < 1) {
        throw new IllegalStateException(String.format("this.size() < 1: %d", size));
      } else if (size == 1) {
        // Just us
        super.printStackTrace(s);
      } else {
        synchronized (s) {
          int i = 1;
          for (final Throwable t : this) {
            if (t == this) {
              s.print(i++ + ". ");
              super.printStackTrace(s);
            } else if (t != null) {
              s.print(i++ + ". ");
              t.printStackTrace(s);
            }
          }
        }
      }
    }
  }

  /**
   * <p>Prints the stack trace of this {@link ThrowableChain} and then
   * of every {@linkplain #iterator() <tt>Throwable</tt> contained by
   * it}.  Each stack trace is preceded by the following text (quoted
   * here; the quotation marks are not part of the text):
   * "<tt><i>d</i>. </tt>" <i>d</i> in the preceding text fragment is
   * substituted with the ordinal position, starting with {@code 1},
   * of the {@link Throwable} in question.</p>
   *
   * @param w the {@link PrintWriter} to print to; must not be {@code
   * null}
   */
  @Override
  public void printStackTrace(final PrintWriter w) {
    if (w != null) {
      final int size = this.size();
      if (size < 1) {
        throw new IllegalStateException(String.format("this.size() < 1: %d", size));
      } else if (size == 1) {
        // Just us
        super.printStackTrace(w);
      } else {
        synchronized (w) {
          int i = 1;
          for (final Throwable t : this) {          
            if (t == this) {
              w.print(i++ + ". ");
              super.printStackTrace(w);
            } else if (t != null) {
              w.print(i++ + ". ");
              t.printStackTrace(w);
            }
          }
        }
      }
    }
  }

}