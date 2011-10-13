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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link Throwable} that is also holds a modifiable list of other
 * {@link Throwable}s that are not connected to the direct {@linkplain
 * Throwable#getCause() causation chain}, but are affiliated
 * nonetheless.  Instances of this class are particularly useful when
 * dealing with {@link Throwable}s in {@code finally} blocks.
 *
 * <p>A {@link ThrowableChain} always contains itself, so the return
 * value of its {@link #size()} method is always at least {@code
 * 1}.</p>
 *
 * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
 *
 * @since 1.0
 */
public class ThrowableChain extends Throwable implements Iterable<Throwable> {

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
   * Overrides the {@link Throwable#initCause(Throwable)} method so
   * that the cause is added to this {@link ThrowableChain}'s list of
   * iterable {@link Throwable}s.
   *
   * @param throwable the cause; may be {@code null}
   *
   * @return this {@link ThrowableChain}
   */
  @Override
  public ThrowableChain initCause(final Throwable throwable) {
    super.initCause(throwable);
    if (throwable != null) {
      assert this.list.size() >= 1;
      assert this.list.get(0) == this;
      assert !this.list.contains(throwable);
      this.list.add(1, throwable);
    }
    return this;
  }

  /**
   * Adds the supplied {@link Throwable} to this {@link
   * ThrowableChain} if it is non-{@code null} and not this {@link
   * ThrowableChain} (a {@link ThrowableChain} cannot add itself to
   * itself).
   *
   * <p>If this {@link ThrowableChain}'s {@linkplain #getCause()
   * cause} is {@code null}, then this {@link ThrowableChain}'s
   * {@linkplain #initCause(Throwable) cause is initialized} to the
   * supplied {@link Throwable} as well.</p>
   *
   * <p>Neither he supplied {@link Throwable}'s {@linkplain
   * Throwable#getCause() cause} nor any transitive causes are
   * added.</p>
   *
   * @param throwable the {@link Throwable} to add; may be {@code
   * null} in which case no action will be taken
   *
   * @return {@code true} if the supplied {@link Throwable} was
   * actually added; {@code false} in all other cases
   */
  public final boolean add(final Throwable throwable) {
    boolean returnValue = false;
    if (throwable != null && throwable != this) {
      if (this.getCause() == null) {
        this.initCause(throwable);
        returnValue = true;
      } else {
        returnValue = this.list.addIfAbsent(throwable);
      }
    }
    return returnValue;
  }

  /**
   * Removes the first instance of the supplied {@link Throwable} from
   * this {@link ThrowableChain}'s list of affiliated {@link
   * Throwable}s, provided that it is not (a) this {@link
   * ThrowableChain}, (b) {@code null}, or (c) this {@link
   * ThrowableChain}'s {@linkplain #getCause() cause}.
   *
   * @param throwable the affiliated {@link Throwable} to remove
   * provided it meets the conditions described; may be {@code null}
   * in which case no action will be taken
   *
   * @return {@code true} if this {@link ThrowableChain} actually
   * removed the supplied {@link Throwable}; {@code false} otherwise
   */
  public final boolean remove(final Throwable throwable) {
    boolean returnValue = false;
    if (throwable != null && throwable != this) {
      if (throwable != this.getCause()) {
        returnValue = this.list.remove(throwable);
      }
    }
    return returnValue;
  }

  /**
   * Returns a new {@link List} instance that contains this {@link
   * ThrowableChain} as its first element, this {@link
   * ThrowableChain}'s {@linkplain #getCause() cause}, if any, as its
   * next element, followed by all other {@link Throwable}s that were
   * {@linkplain #add(Throwable) added to} this {@link
   * ThrowableChain}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The {@link List} returned by this method is never {@linkplain
   * Collection#isEmpty() empty}.</p>
   *
   * <p>The {@link List} returned by this method is mutable.</p>
   *
   * @return a new {@link List}; never {@code null}
   *
   * @see #iterator()
   *
   * @see #asList()
   */
  public List<Throwable> toList() {
    return new ArrayList<Throwable>(this.asList());
  }

  /**
   * Returns a new {@linkplain Collections#unmodifiableList(List)
   * unmodifiable view} of this {@link ThrowableChain}'s list of
   * affiliated {@link Throwable}s.  No copying occurs during this
   * operation.
   *
   * <p>The returned {@link List} is never {@code null}, never
   * {@linkplain Collection#isEmpty() empty},
   * <strong>immutable</strong> and safe for iteration by multiple
   * threads without synchronization or locking.</p>
   *
   * @return a read-only view of the underlying list of affiliated
   * {@link Throwable}s; never {@code null}
   */
  public List<Throwable> asList() {
    return Collections.unmodifiableList(this.list);
  }

  /**
   * Returns the size of this {@link ThrowableChain}.  A {@link
   * ThrowableChain} always has a size of at least {@code 1}.
   *
   * @return the size of this {@link ThrowableChain}&mdash;a positive
   * integer greater than or equal to {@code 1}
   */
  public int size() {
    return this.list.size();
  }

  /**
   * Returns an {@link Iterator} that can be used to iterate over all
   * {@linkplain #add(Throwable) contained <tt>Throwable</tt>s}.
   *
   * <p>This method never returns {@code null}.</p>
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
      if (this.size() == 2) {
        // Us and a cause
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
      if (this.size() == 2) {
        // Us and a cause, nothing else, so regular stack trace
        // printing is fine.
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