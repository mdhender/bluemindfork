package net.bluemind.backend.mail.replica.indexing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class IDSet implements Iterable<IDRange> {

	private class IDSetIterator implements ListIterator<Long> {

		private ListIterator<Long> currentRangeIterator = null;
		private ListIterator<IDRange> rangeIterator = iterator();

		public IDSetIterator() {
			rangeIterator = iterator();
			currentRangeIterator = rangeIterator.next().iterator();
		}

		@Override
		public boolean hasNext() {
			return (currentRangeIterator != null && currentRangeIterator.hasNext()) || rangeIterator.hasNext();
		}

		@Override
		public boolean hasPrevious() {
			return (currentRangeIterator != null && currentRangeIterator.hasPrevious()) || rangeIterator.hasPrevious();
		}

		@Override
		public Long next() {
			if (currentRangeIterator == null) {
				return null;
			}

			if (currentRangeIterator.hasNext()) {
				return currentRangeIterator.next();
			} else {
				if (rangeIterator.hasNext()) {
					currentRangeIterator = rangeIterator.next().iterator();
					return currentRangeIterator.next();
				} else {
					currentRangeIterator = null;
					return null;
				}
			}

		}

		@Override
		public Long previous() {
			if (currentRangeIterator == null) {
				currentRangeIterator = rangeIterator.previous().iteratorFromEnd();
			}

			if (currentRangeIterator.hasPrevious()) {
				return currentRangeIterator.previous();
			} else {
				if (rangeIterator.hasPrevious()) {
					currentRangeIterator = rangeIterator.previous().iterator();
					return currentRangeIterator.previous();
				} else {
					return null;
				}
			}
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

	private List<IDRange> ranges;

	private IDSet(List<IDRange> ranges) {
		this.ranges = ranges;
	}

	@Override
	public ListIterator<IDRange> iterator() {
		return ranges.listIterator();
	}

	public IDSetIterator iterateUid() {
		return new IDSetIterator();
	}

	private static final int SEQUENCE_STATE = 0;
	private static final int RANGE_STATE = 1;

	public static IDSet parse(String set) {
		// it's 0-9 or '*' or ',' or ':'
		// so byte == char
		byte[] value = set.getBytes();
		int begin = 0;

		long lastNumber = -1;
		int state = SEQUENCE_STATE;
		List<IDRange> ranges = new LinkedList<>();

		for (int i = 0; i < value.length; i++) {
			byte b = value[i];
			if (b == ':') {
				state = RANGE_STATE;
				lastNumber = parseNumber(value, begin, i - begin);
				begin = i + 1;
			} else if (b == ',') {

				if (state == RANGE_STATE) {
					ranges.add(new IDRange(lastNumber, parseNumber(value, begin, i - begin)));
					state = SEQUENCE_STATE;
				} else {
					long number = parseNumber(value, begin, i - begin);
					ranges.add(new IDRange(number, number));
				}

				begin = i + 1;
			}
		}

		if (state == RANGE_STATE) {
			ranges.add(new IDRange(lastNumber, parseNumber(value, begin, value.length - begin)));
			state = SEQUENCE_STATE;
		} else {
			long number = parseNumber(value, begin, value.length - begin);
			ranges.add(new IDRange(number, number));
		}

		return new IDSet(ranges);

	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (IDRange r : ranges) {
			sb.append(sep);
			sb.append(r.toString());
			sep = ",";
		}

		return sb.toString();
	}

	private static long parseNumber(byte[] value, int begin, int count) {
		if (count == 1 && value[begin] == '*') {
			return -1;
		} else {
			return Long.parseLong(new String(value, begin, count), 10);
		}
	}

	public static IDSet create(int[] uids) {
		return create(Arrays.stream(uids).iterator());
	}

	public static IDSet create(List<Integer> uids) {
		return create(uids.iterator());
	}

	public static IDSet create(Iterator<Integer> iterator) {
		return create(iterator, 5000);
	}

	public static IDSet create(Iterator<Integer> iterator, int sizelimit) {
		if (!iterator.hasNext()) {
			return new IDSet(Collections.emptyList());
		}
		LinkedList<IDRange> ranges = new LinkedList<>();
		int begin = iterator.next();
		int end = begin;

		while (iterator.hasNext()) {
			int uid = iterator.next();
			if (uid <= end + 1 && ((end - begin) < (sizelimit - 1))) {
				end = uid;
			} else {
				ranges.add(new IDRange(begin, end));
				begin = uid;
				end = uid;
			}
		}
		ranges.add(new IDRange(begin, end));
		return new IDSet(ranges);
	}

}
