/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.cue;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface CueIterable<T> extends Iterable<T> {

  final class CueIterator<T> implements ListIterator<T> {

    private final ListIterator<? extends T> delegate;

    public CueIterator(List<? extends T> iterable) {
      delegate = iterable.listIterator();
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public T next() {
      return delegate.next();
    }

    @Override
    public boolean hasPrevious() {
      return delegate.hasPrevious();
    }

    @Override
    public T previous() {
      return delegate.previous();
    }

    @Override
    public int nextIndex() {
      return delegate.nextIndex();
    }

    @Override
    public int previousIndex() {
      return delegate.previousIndex();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    @Override
    public void set(T t) {
      throw new UnsupportedOperationException("set");
    }

    @Override
    public void add(T t) {
      throw new UnsupportedOperationException("add");
    }
  }

  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }
}
