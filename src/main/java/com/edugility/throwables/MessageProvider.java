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

public class MessageProvider {

  public MessageProvider() {
    super();
  }

  private static class MessageParser {

    private enum State {
      NORMAL, COMMENT, IN_QUOTE, INDETERMINATE_GLOB, GLOB, GREEDY_GLOB, INDETERMINATE_PERIOD, ELLIPSIS, ESCAPE, IDENTIFIER, PROPERTY_BLOCK
    }

    private void identifierStart(final int c) {

    }

    private void identifier(final int c) {

    }

    private void greedyGlob() {

    }

    private void comment(final int c) {

    }

    private void ellipsis() {

    }

    public boolean matches(final Object o, final String pattern) throws IOException {
      final StringReader reader = new StringReader(pattern);

      State state = State.NORMAL;

      State priorState = null;

      int pos = -1;
      int periodCount = 0;
      int c;
      while ((c = reader.read()) != -1) {
        ++pos;
        switch (state) {


          // NORMAL
        case NORMAL:
          switch (c) {

          case ' ':
          case '\t':
          case '\r':
          case '\n':
          case '\f':
            break;

          case '.':
            state = State.INDETERMINATE_PERIOD;
            break;

          case '\u2026':
            periodCount = 0;
            state = State.ELLIPSIS;
            break;

          case '#':
            state = State.COMMENT;
            break;

          case '*':
            state = State.INDETERMINATE_GLOB;
            break;

          default:
            if (!Character.isJavaIdentifierStart(c)) {
              throw new IllegalStateException("Position: " + pos);
            }
            state = State.IDENTIFIER;
            identifierStart(c);
          }
          break;
          // end NORMAL


          // COMMENT
        case COMMENT:
          switch (c) {

          case '\r':
          case '\n':
            state = State.NORMAL;
            break;

          default:
            comment(c);

          }
          break;
          // end COMMENT


          // INDETERMINATE_PERIOD
        case INDETERMINATE_PERIOD:
          switch (c) {

          case '.':
            assert periodCount > 0 : "periodCount <= 0: " + periodCount;
            assert periodCount < 3 : "periodCount >= 3: " + periodCount;
            if (periodCount == 2) {
              periodCount = 0;
              state = State.ELLIPSIS;
            }
            break;

          default:
            throw new IllegalStateException("Position: " + pos);
          }
          break;
          // end INDETERMINATE_PERIOD


          // INDETERMINATE_GLOB
        case INDETERMINATE_GLOB:
          switch (c) {

          case ' ':
          case '\t':
          case '\r':
          case '\n':
          case '\f':
          case '/':
            state = State.GLOB;
            break;

          case '*':
            state = State.GREEDY_GLOB;
            greedyGlob();
            break;

          default:
            throw new IllegalStateException("Position: " + pos);
          }
          break;
          // end INDETERMINATE_GLOB


          // GREEDY_GLOB
        case GREEDY_GLOB:
          switch (c) {

          case '/':
            state = State.NORMAL;
            break;

          default:
            throw new IllegalStateException("Position: " + pos);
          }
          break;
          // end GREEDY GLOB


          // IDENTIFIER
        case IDENTIFIER:
          switch (c) {

          case '.':
            periodCount++;
            state = State.INDETERMINATE_PERIOD;
            break;

          case '/':
            break;

          case '\u2026':
            periodCount = 0;
            state = State.ELLIPSIS;
            break;

          case '(':
            state = State.PROPERTY_BLOCK;
            break;

          default:
            if (!Character.isJavaIdentifierPart(c)) {
              throw new IllegalStateException("Position: " + pos);
            }
            identifier(c);
          }
          break;
          // end IDENTIFIER


          // ELLIPSIS
        case ELLIPSIS:
          ellipsis();
          switch (c) {
          case '#':
            state = State.COMMENT;
            break;

          default:
            state = State.NORMAL;
            break;
          }
          break;
          // end ELLIPSIS

        default:

          break;
        }
      }
      
      // TODO: implement
      return false;

    }

  }

}