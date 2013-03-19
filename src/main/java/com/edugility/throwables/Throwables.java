/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2011-2012 Edugility LLC.
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

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A class to assist with processing {@link Throwable} instances.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public final class Throwables {

  /**
   * Creates a new {@link Throwables} object.
   */
  private Throwables() {
    super();
  }

  /**
   * Returns the "deepest" {@linkplain Throwable#getCause() cause}
   * reachable from the supplied {@link Throwable}.
   *
   * @param t the {@link Throwable} to investigate; may be {@code null}
   *
   * @return the "deepest" {@linkplain Throwable#getCause() cause}
   * reachable from the supplied {@link Throwable}, or {@code null}
   */
  public static final Throwable getPrimordialCause(Throwable t) {
    if (t == null) {
      return null;
    }
    Throwable cause = t;
    while ((cause = cause.getCause()) != null) {
      t = cause;
    }
    return t;
  }

  /**
   * Creates and returns a somewhat esoteric view of the supplied
   * {@link Throwable} and its {@linkplain Throwable#getCause() causal
   * chain} as a {@link List}.
   *
   * <p>In the simplest case, where the supplied {@link Throwable}
   * itself does not implement the {@link Iterable} interface, the
   * {@link List} returned by this method is equal in behavior to
   * {@link ThrowableList}.</p>
   *
   * <p>In the more complicated case where the supplied {@link
   * Throwable} <em>does</em> implement {@link Iterable}, each {@link
   * Throwable} in the iteration is added to the {@link List} that
   * will be returned, followed immediately by {@linkplain
   * Throwable#getCause() its causal chain}.  {@link
   * java.sql.SQLException} is an example of a {@link Throwable} that
   * also implements {@link Iterable}; {@link ThrowableChain} is
   * another.</p>
   *
   * <p>In cases where the {@link Throwable}s returned by an {@link
   * Iterable} {@link Throwable} are also contained somewhere in a
   * {@linkplain Throwable#getCause() causal chain}, the returned
   * {@link List} may contain duplicate elements.</p>
   *
   * <p>In general, if you are interested only in viewing the
   * {@linkplain Throwable#getCause() causal chain} of a given {@link
   * Throwable} as a {@link List} then creating and using a {@link
   * ThrowableList} may fit your needs better.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>This method allocates a new {@link List} when called.</p>
   *
   * @param throwable the {@link Throwable} in question; may be {@code null}
   *
   * @return an {@linkplain Collections#unmodifiableList(List)
   * unmodifiable} {@link List} of {@link Throwable}s; never {@code
   * null}; the return value will always contain the supplied {@link
   * Throwable} as its first element unless the supplied {@link
   * Throwable} is {@code null}
   *
   * @see ThrowableList
   */
  public static final List<Throwable> toList(final Throwable throwable) {
    final List<Throwable> l = toList(throwable, null);
    if (l == null || l.isEmpty()) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(l);
    }
  }
  
  /**
   * Creates and returns a view of the supplied {@link Throwable} and
   * its {@linkplain Throwable#getCause() causal chain} as a {@link
   * List}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>This method mutates the supplied {@link List} when possible.
   * If the supplied {@link List} is {@code null}, a new {@link List}
   * is allocated.</p>
   *
   * @param throwable the {@link Throwable} in question; may be {@code
   * null}
   *
   * @param l the {@link List} to return; may be {@code null} in which
   * case a new {@link List} will be allocated
   *
   * @return a {@link List} of {@link Throwable}s; never {@code null};
   * the return value will always contain the supplied {@link
   * Throwable} as its first element unless the supplied {@link
   * Throwable} is {@code null}
   */
  private static final List<Throwable> toList(final Throwable throwable, List<Throwable> l) {
    final List<Throwable> returnValue;
    if (throwable == null) {
      returnValue = Collections.emptyList();
    } else {

      final Iterable<?> throwables;
      if (throwable instanceof Iterable) {
        throwables = (Iterable<?>)throwable;
      } else {
        throwables = Collections.singleton(throwable);
      }

      if (l == null) {
        l = new ArrayList<Throwable>();
      }

      final Iterator<?> iterator = throwables.iterator();
      boolean found = false;
      if (iterator != null) {

        while (iterator.hasNext()) {
          final Object o = iterator.next();
          if (o instanceof Throwable) {
            final Throwable t = (Throwable)o;
            found = found || t == throwable;

            // Add the Throwable itself to the list we'll return.
            l.add(t);

            // XXX TODO FIXME? MAYBE?: I don't like this.  If a
            // Throwable is encountered that implements
            // Iterable<Throwable> (like java.sql.SQLException, or
            // ThrowableChain) then it should be presumed that the
            // Throwable is fully in charge of its own
            // iteration...maybe.
            //
            // On the other hand, the contract for this method says
            // definitively and explicitly that iteration is over the
            // *causal chain* (which rules out how ThrowableChain
            // iterates).  So this might be doing the right thing
            // after all.
            //
            // This method basically takes chains of linked Throwables
            // and flattens them into a List:
            // 
            // ta --> ta' --> ta'' --> ta'''
            // |
            // tb --> tb' --> tb''
            // |
            // tc --> tc' --> tc'' --> tc'''
            //
            // ...becomes:
            //
            // ta, ta', ta'', ta''', tb, tb', tb'', tc, tc', tc'', tc'''

            final Throwable cause = t.getCause();
            if (cause != null) {
              assert cause != t; // prevented by Throwable contract

              // Now (recursively) perform this operation on the cause
              // and ITS cause and so on.
              final List<Throwable> list = toList(cause, l); // recursive call
              assert list == l;
              found = found || l.contains(throwable);
            }

          }
        }
      }
      if (!found) {
        l.add(0, throwable);
      }
      returnValue = l;
    }
    return returnValue;
  }

  /**
   * Returns the first {@link Throwable} in the {@linkplain
   * Throwable#getCause() causal chain} of the supplied {@link
   * Throwable} (or the supplied {@link Throwable} itself) that is an
   * instance of the supplied {@link Class}.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param t the {@link Throwable} whose causal chain should be
   * investigated; may be {@code null} in which case {@code null} will
   * be returned
   *
   * @param throwableClass the {@link Class} whose {@link
   * Class#isInstance(Object)} method will be called; if {@code null}
   * then {@code null} will be returned
   *
   * @return the first {@link Throwable} in the causal chain that is
   * an instance of the supplied {@link Class}, or {@code null}
   */
  public static final <T extends Throwable> T firstInstance(Throwable t, final Class<T> throwableClass) {
    @SuppressWarnings("unchecked")
    final T returnValue = (T)nthInstance(t, new InstanceOfPredicate(throwableClass), 0);
    return returnValue;
  }

  /**
   * Returns a {@link Throwable} in the {@linkplain
   * Throwable#getCause() causal chain} of the supplied {@link
   * Throwable} (or the supplied {@link Throwable} itself) that is the
   * appropriate instance of the supplied {@link Class}.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param throwable the {@link Throwable} whose causal chain should
   * be investigated; may be {@code null} in which case {@code null}
   * will be returned
   *
   * @param throwableClass the {@link Class} whose {@link
   * Class#isInstance(Object)} method will be called; if {@code null}
   * then {@code null} will be returned
   *
   * @param zeroBasedOccurrence the {@code 0}-based number that
   * identifies which of the potentially many matches to return.
   * {@code 0} will return the first {@link Throwable} that matches,
   * {@code 1} will return the second one, and so on.
   *
   * @return the <i>n</i><sup>th</sup> {@link Throwable} in the
   * causal chain that is an instance of the supplied {@link Class},
   * or {@code null}
   */
  public static final <T extends Throwable> T nthInstance(Throwable throwable, final Class<T> throwableClass, int zeroBasedOccurrence) {
    @SuppressWarnings("unchecked")
    final T returnValue = (T)nthInstance(throwable, new InstanceOfPredicate(throwableClass), zeroBasedOccurrence);
    return returnValue;
  }

  /**
   * Returns the <i>n</i><sup>th</sup> occurrence of a {@link
   * Throwable}&mdash;in the supplied {@link Throwable}'s {@linkplain
   * Throwable#getCause() causal chain}&mdash;that is {@linkplain
   * Throwables.Predicate#apply(Throwable) accepted by the supplied
   * <tt>Predicate</tt>}, where <i>n</i> is equal to the supplied
   * {@code zeroBasedOccurrence} parameter value plus one.
   *
   * <p>This method may return {@code null}.</p>
   * 
   * @param throwable the {@link Throwable} to investigate; may be
   * {@code null} in which case {@code null} will be returned
   * 
   * @param predicate the {@link Throwables.Predicate} to use to
   * evaluate fitness; may be {@code null} in which case this method
   * will behave as though a {@link Throwables.Predicate} had been
   * supplied to it that returns {@code true} in all cases from its
   * {@link Throwables.Predicate#apply(Throwable)} method
   *
   * @param zeroBasedOccurrence which occurrence to return, starting
   * at {@code 0}
   * 
   * @return the {@code zeroBasedOccurrence + 1}<sup>th</sup>
   * occurrence of a {@link Throwable}&mdash;in the supplied {@link
   * Throwable}'s {@linkplain Throwable#getCause() causal
   * chain}&mdash;that is {@linkplain
   * Throwables.Predicate#apply(Throwable) accepted by the supplied
   * <tt>Predicate</tt>}
   */
  public static final Throwable nthInstance(Throwable throwable, final Predicate predicate, int zeroBasedOccurrence) {
    zeroBasedOccurrence = Math.max(0, zeroBasedOccurrence);
    Throwable returnValue = null;
    final List<Throwable> list = toList(throwable);
    assert list != null;
    // Note: do not use getCause(); the work has already been done
    int numberOfOccurrences = 0;
    for (int i = 0; i < list.size(); i++) {
      final Throwable t = list.get(i);
      if (t != null && (predicate == null || predicate.apply(t)) && zeroBasedOccurrence == numberOfOccurrences++) {
        returnValue = t;
        break;
      }
    }
    return returnValue;
  }

  /**
   * Returns the last {@link Throwable} in the {@linkplain
   * Throwable#getCause() causal chain} of the supplied {@link
   * Throwable} (or the supplied {@link Throwable} itself) that is an
   * instance of the supplied {@link Class}.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param t the {@link Throwable} whose causal chain should be
   * investigated; may be {@code null} in which case {@code null} will
   * be returned
   *
   * @param throwableClass the {@link Class} whose {@link
   * Class#isInstance(Object)} method will be called; if {@code null}
   * then {@code null} will be returned
   *
   * @return the last {@link Throwable} in the causal chain that is
   * an instance of the supplied {@link Class}, or {@code null}
   */
  public static final <T extends Throwable> T lastInstance(Throwable t, final Class<T> throwableClass) {
    T returnValue = null;
    if (throwableClass != null) {
      while (t != null) {
        if (throwableClass.isInstance(t)) {
          returnValue = throwableClass.cast(t);
        }
        t = t.getCause();
      }
    }
    return returnValue;
  }

  /**
   * A predicate that evaluates a {@link Throwable} for some user-defined condition.
   *
   * @author <a href="http://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   */
  public static interface Predicate extends Serializable {

    /**
     * Evaluates the supplied {@link Throwable} for fitness and
     * returns {@code true} if the supplied {@link Throwable} is
     * deemed fit.
     *
     * @param t the {@link Throwable} in question; may be {@code null}
     *
     * @return {@code true} if the supplied {@link Throwable} is fit;
     * {@code false} otherwise
     */
    public boolean apply(final Throwable t);

  }

  /**
   * A {@link Predicate} that determines whether a {@link Throwable}
   * is an instance of a given {@link Class}.
   *
   * @author <a href="mailto:ljnelson@gmail.com">Laird Nelson</a>
   *
   * @since 1.0-SNAPSHOT
   */
  private static final class InstanceOfPredicate implements Predicate {

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
     * Creates a new {@link InstanceOfPredicate}.
     *
     * @param cls the {@link Class} to use for the predicate test; may
     * be {@code null}
     */
    private InstanceOfPredicate(final Class<? extends Throwable> cls) {
      super();
      this.cls = cls;
    }

    /**
     * Returns {@code true} if the supplied {@link Throwable} is an
     * instance of the {@link Class} supplied to this {@link
     * InstanceOfPredicate}'s {@linkplain
     * #Throwables.InstanceOfPredicate(Class) constructor}.
     *
     * @param t the {@link Throwable} to test; may be {@code null}
     *
     * @return {@code true} if the supplied {@link Throwable} is an
     * instance of the {@link Class} supplied to this {@link
     * InstanceOfPredicate}'s {@linkplain
     * #Throwables.InstanceOfPredicate(Class) constructor}; {@code
     * false} in all other cases
     */
    @Override
    public final boolean apply(final Throwable t) {
      if (t == null) {
        return false;
      }
      if (this.cls == null) {
        return false;
      }
      if (!this.cls.isInstance(t)) {
        return false;
      }
      return true;
    }

  }

}
