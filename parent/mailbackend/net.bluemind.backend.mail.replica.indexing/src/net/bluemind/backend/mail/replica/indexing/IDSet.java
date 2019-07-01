package net.bluemind.backend.mail.replica.indexing;

import java.util.Arrays;
import java.util.Collection;
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

	public static void main(String[] args) {
		int[] list = new int[] { 1, 2, 3, 4, 6, 8, 9, 10, -1 };
		testCreate(list);
		Collection<Integer> cols = new LinkedList<Integer>();
		for (int i = 0; i < list.length; i++) {
			cols.add(list[i]);
		}
		testCreate(cols.iterator());

		String longSet = "22618,22943,23261:30169,30171:31183,31185:31661,31741:31746,31761:31809,31817:31883,31952:32223,32383:32477,32593:32614,32709:32710,32712:32713,33036:33293,33331:33343,33355:33386,33399:33491,33571:33661,33771:33882,33964:34001,34015,34081:34086,34109:34271,34316:34377,34382:34383,34385,34390:34391,34405,34408,34600:34616,34712:34760,34854:34857,34921:34937,34989:35129,35149:35229,35385:35513,35744:35813,35876:35877,35907:35944,35949:36081,36145:36227,36315:36375,36412:36475,36560:36592,36618:36681,36698:36894,36926:37007,37061:37115,37160:37210,37258:37259,37315,37317:37401,37403:37498,37554:37628,37786:37873,37956:37985,38048:38052,38111:38113,38133:38243,38246:38381,38393:38480,38535:38646,38729:38797,38823,38825:38826,38828,38830,38873:38885,38965:38974,38978:39056,39085,39087,39140:39295,39331,39447:39593,39659:39665,39772:39779,39799:39820,39960:40174,40291:40417,40482:40504,40599:40603,40612:40678,40744:40932,41049:41120,41196:41225,41328:41346,41359:41399,41418:41629,41699:41792,41886:41980,42230,42252,42272,42296,42395:42843,43016:43082,43204:43265,43391:43419,43498:43501,43512:43574,43577:43663,43771:43806,43942:43975,44059:44108,44195:44218,44270:44286,44290:44320,44341:44393,44534:44577,44681,44731:44777,44905:44959,45021,45069,45073:45127,45131:45287,45353:45434,45507:45568,45633:45690,45764:45782,45847:45848,45852:45862,45870:45921,46012:46088,46217:46283,46365:46447,46521:46524,46591:46598,46601:46671,46679:46732,46794:46885,46934:46978,46980:47000,47171:47275,47358:47377,47461:47474,47476:47600,47604:47674,47754:47819,47916:47972,48045:48117,48145:48156,48189:48199,48203:48256,48258:48374,48415:48487,48519:48575,48616:48661,48669:48681,48705:48721,48728:48797,48804:48896,48975:49080,49164:49245,49361:49439,49496:49514,49584:49875,49930:50049,50186:50308,50383:50460,50531:50540,50600:50601,50607:50713,50718:50852,50965:51046,51262:51347,51461:51511,51625:51627,51708:51727,51735:51873,51878:52027,52105:52231,52296:52356,52434:52480,52550:52552,52620:52622,52626:52690,52692:52763,52865:52928,53114:53186,53287:53350,53461:53463,53522:53617,53627:53754,53836:53916,54020:54092,54173:54238,54318:54319,54346:54352,54358:54418,54433:54480,54578:54747,54889:54970,55031:55113,55173,55215:55217,55219:55297,55303:55398,55449:55520,55628:55654,55767:55798,55859:55861,55897,55913:55983,56029:56068,56282:56288,56386:56408,56456:56461,56542:56547,56552:56712,56784:56898,57020:57075,57157:57225,57387:57440,57454:57589,57702:57775,58039:58109,58429:58486,58677:58681,58701:58704,58717:58828,58841:59055,59235:59347,59502:59614,59791:59870,59953:59959,60031:60111,60115:60178,60184:60260,60464:60532,60654:60720,60827:60829,61000,61038:61138,61178:61362,61507:61606,61904:62012,62168:62225,62449:62457,62478:62558,62570:62672,62780:62859,62959:63037,63132:63155,63288:63292,63379:63392,63403:63499,63508:63646,63759:63818,64001:64059,64238:64278,64456,64567:64571,64607:64656,64674:64780,64888:64999,65231:65290,65402:65436,65586:65600,65766:65810,65836:66048,66139:66235,66501:66547,66681:66760,66946:66949,67125:67195,67225:67345,67438:67517,67593:67669,67679:67748,67824:67837,67890:67902,67905:68016,68032:68294,68360:68678,69767:70100,70102:70845,70847:70880,71066:71180,71370:71475,71626:71795,71868:71904,72039:72077,72085:72228,72235:72531,72656:72810,73224:73363,73534:73616,73723:73742,73848:73857,73879:73986,74008:74105,74261:74349,74473:74604,74781:74866,75281:75290,75444:75451,75463:75574,75578:75759,75880:76057,76204:76380,76534:76642,76792:76826,76985:77001,77009:77115,77127:77357,77464:77574,77807:77955,78084:78216,78341:78372,78436:80259,80261:80265,80267:80704,80706:80797,80799:81068,81071:81173,81175:81396,81399:81740,81742:82297,82299:82385,82387:82532,82534:82953,82955:83352,83354:84253,84255:84301,84303:84918,84920:85720,85722:90448,90450:91413";
		testParse(longSet);

		IDSet set = parse(longSet);
		IDSetIterator it = set.iterateUid();
		while (it.hasNext()) {
			Long value = it.next();
			Long p = it.previous();
			Long pn = it.next();
			if (!p.equals(value)) {
				System.out.println("should not happen ! " + p + " value " + value);
				break;
			}

			if (!pn.equals(value)) {
				System.out.println("should not happen on next ! " + pn + " value " + value + " and p " + p);
				break;
			}
		}
	}

	private static void testCreate(Iterator<Integer> iterator) {
		System.out.println(create(iterator));
	}

	private static void testCreate(int[] is) {
		System.out.println(create(is));
	}

	private static void testParse(String test) {
		long t = System.nanoTime();
		for (int i = 0; i < 100000; i++) {
			IDSet set = parse(test);
			Iterator<Long> it = set.iterateUid();
			while (it.hasNext()) {
				it.next();
			}
		}
		t = System.nanoTime() - t;
		System.out.println("fast: " + t + "ns.");

	}

	public static IDSet create(int[] uids) {
		return create(Arrays.stream(uids).iterator());
	}

	public static IDSet create(List<Integer> uids) {
		return create(uids.iterator());
	}

	public static IDSet create(Iterator<Integer> iterator) {
		if (!iterator.hasNext()) {
			return new IDSet(Collections.emptyList());
		}
		LinkedList<IDRange> ranges = new LinkedList<>();
		int begin = iterator.next();
		int end = begin;

		while (iterator.hasNext()) {
			int uid = iterator.next();
			if (uid <= end + 1) {
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
