package net.bluemind.backend.mail.replica.indexing;

import java.util.ListIterator;

public class IDRange implements Iterable<Long> {

	private long from;
	private long to;

	public static void main(String[] args) {
		IDRange range = new IDRange(1, 10);

		ListIterator<Long> it = range.iterator();
		it = range.iterator();
		while (it.hasNext()) {
			it.next();
			it.previous();
			System.out.println(it.next());
		}
	}

	public IDRange(long from, long to) {
		this.from = from;
		this.to = to;
	}

	public boolean isLimited() {
		return from > 0 && to > 0;
	}

	public boolean isUnique() {
		return from == to;
	}

	public long from() {
		return from;
	}

	public long to() {
		return to;
	}

	public String toString() {
		if (isUnique()) {
			return "" + from;
		} else {

			return (from > 0 ? "" + from : "*") + ":" + (to > 0 ? "" + to : "*");
		}
	}

	private class IDRangeIterator implements ListIterator<Long> {
		private Long nextValue;
		private Long previousValue;

		public IDRangeIterator() {
			this(false);
		}

		public IDRangeIterator(boolean beginAtEnd) {
			if (!beginAtEnd) {
				nextValue = from;
			} else {
				previousValue = to;
			}
		}

		@Override
		public boolean hasNext() {
			return nextValue != null;
		}

		@Override
		public Long next() {
			if (nextValue == null)
				return null;

			Long v = nextValue;
			if (to == -1) {
				nextValue = null;
				previousValue = null;
			} else {

				nextValue += 1;
				if (nextValue > to) {
					nextValue = null;
					previousValue = to;
				} else {
					previousValue = v;
				}
			}
			return v;
		}

		@Override
		public Long previous() {
			if (previousValue == null) {
				return null;
			}

			Long v = previousValue;
			previousValue -= 1;

			if (previousValue < from) {
				previousValue = null;
				nextValue = from;
			} else {
				nextValue = v;
			}
			return v;
		}

		@Override
		public boolean hasPrevious() {
			return previousValue != null;
		}

		@Override
		public void remove() {
		}

		@Override
		public void add(Long e) {
		}

		@Override
		public int nextIndex() {
			return 0;
		}

		@Override
		public int previousIndex() {
			return 0;
		}

		@Override
		public void set(Long e) {
		}

	}

	public ListIterator<Long> iteratorFromEnd() {
		return new IDRangeIterator(true);
	}

	@Override
	public ListIterator<Long> iterator() {
		return new IDRangeIterator();
	}
}
