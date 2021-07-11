package seats;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils_seats {

	private static Map<Long, Integer> customerIdCount;
	private static List<Long> flightIds;
	private static List<Long> r_f_id;
	private static List<Long> r_c_id;
	private static long resCount = -1;

	static AtomicBoolean atomicInitialized = new AtomicBoolean(false);
	static boolean waitForInit = true;

	// this function will be -dynamically- called from clojure at runtime
	public Utils_seats(int scale) {
		Utils_seats.initialize(scale);
	}

	public static long getRandomCustomerId() {
		long composite_id = -1;
		while (true) {
			long depart_airport_id = ThreadLocalRandom.current().nextLong(2, 284);
			Integer customerId = customerIdCount.get(depart_airport_id);
			if (customerId == null || customerId < 5)
				continue;
			long id = ThreadLocalRandom.current().nextLong(1, customerId);
			composite_id = encode(new long[] { id, depart_airport_id }, COMPOSITE_BITS, COMPOSITE_POWS);
			break;
		}

		return composite_id;

	}

	public static Timestamp getNextRandomDate() {
		long offset = Timestamp.valueOf("2018-12-24 04:34:19").getTime();
		long end = Timestamp.valueOf("2019-02-14 04:04:19").getTime();
		long diff = end - offset + 1;
		Timestamp rand = new Timestamp(offset + (long) (Math.random() * diff));
		return rand;
	}

	public static Timestamp getNextDateWithBegin(Timestamp beginDate) {
		long offset = beginDate.getTime();
		long end = Timestamp.valueOf("2019-02-14 04:04:19").getTime();
		long diff = end - offset + 1;
		diff = diff / 10;
		Timestamp rand = new Timestamp(offset + (long) (Math.random() * diff));
		return rand;
	}

	public static long getNextAirlineId() {
		return nextLong(281474976710656L, 80501843339247631L);
	}

	public static long getNewResId() {
		// System.out.println("getting a new res id -- current size: " + resCount);
		resCount++;
		return resCount;
	}

	public static long getRandomResId() {
		// System.out.println("getting a random res id -- current size: " + resCount);
		return ThreadLocalRandom.current().nextLong(resCount);
	}

	public static long getRandomFlightId() {
		int index = ThreadLocalRandom.current().nextInt(1, flightIds.size() - 1);
		return flightIds.get(index);
	}

	public static long getExistingResCustomerId(int index) {
		return r_c_id.get(index);
	}

	public static long getExistingResFlightId(int index) {
		return r_f_id.get(index);
	}

	public static int getRandomResIndex() {
		return ThreadLocalRandom.current().nextInt(r_f_id.size());
	}

	public static long[] getNewAttrs() {
		long[] result = new long[9];
		for (int i = 0; i < 9; i++)
			result[i] = ThreadLocalRandom.current().nextLong();
		return result;
	}

	public static long nextLong(long minimum, long maximum) {
		return ThreadLocalRandom.current().nextLong(minimum, maximum);
	}

	private static final int COMPOSITE_BITS[] = { 48, // ID
			16, // AIRPORT_ID
	};

	private static final long COMPOSITE_POWS[] = compositeBitsPreCompute(COMPOSITE_BITS);

	protected static final long[] compositeBitsPreCompute(int offset_bits[]) {
		long pows[] = new long[offset_bits.length];
		for (int i = 0; i < offset_bits.length; i++) {
			pows[i] = (long) (Math.pow(2, offset_bits[i]) - 1l);
		} // FOR
		return (pows);
	}

	public static long encode(long values[], int offset_bits[], long offset_pows[]) {
		assert (values.length == offset_bits.length);
		long id = 0;
		int offset = 0;
		for (int i = 0; i < values.length; i++) {
			id = (i == 0 ? values[i] : id | values[i] << offset);
			offset += offset_bits[i];
		} // FOR

		return (id);
	}

	public static void initialize(int scale) {
		if (atomicInitialized.compareAndSet(false, true)) {
			System.out.println("Utils_seats_"+scale+": intializing data structures....");
			initializeFLightIds(scale);
			initializeCustomerMap();
			initializeReservations(scale);
			resCount = r_c_id.size();
			waitForInit = false;
		} else {
			while (waitForInit)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			// System.out.println("~~~~~~~~>>>" + flightIds.size());
		}
	}

	private static void initializeFLightIds(int scale) {
		flightIds = new ArrayList<Long>();
		Scanner s = null;
		try {
			s = new Scanner(new File(
					"/home/ubuntu/jepsen.seats/snapshots/seats/" + (String.valueOf(scale)) + "/seats/flight.id"));
			while (s.hasNext()) {
				flightIds.add(Long.parseLong(s.next()));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		s.close();
	}

	// another data structure holding customer and flight id, extracted from valid
	// reservations
	private static void initializeReservations(int scale) {
		r_c_id = new ArrayList<Long>();
		r_f_id = new ArrayList<Long>();
		Scanner s1 = null, s2 = null;
		try {
			s1 = new Scanner(new File("/home/ubuntu/jepsen.seats/snapshots/seats/"+ (String.valueOf(scale)) +"/seats/r_c.id"));
			s2 = new Scanner(new File("/home/ubuntu/jepsen.seats/snapshots/seats/"+ (String.valueOf(scale)) +"/seats/r_f.id"));
			while (s1.hasNext() && s2.hasNext()) {
				r_c_id.add(Long.parseLong(s1.next()));
				r_f_id.add(Long.parseLong(s2.next()));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		s1.close();
		s2.close();
	}

	private static void initializeCustomerMap() {
		customerIdCount = new HashMap<Long, Integer>();
		customerIdCount.put(1L, 69);
		customerIdCount.put(2L, 39);
		customerIdCount.put(3L, 534);
		customerIdCount.put(4L, 10);
		customerIdCount.put(5L, 8);
		customerIdCount.put(6L, 40);
		customerIdCount.put(7L, 5);
		customerIdCount.put(9L, 3);
		customerIdCount.put(10L, 57);
		customerIdCount.put(11L, 83);
		customerIdCount.put(12L, 145);
		customerIdCount.put(13L, 86);
		customerIdCount.put(14L, 232);
		customerIdCount.put(15L, 91);
		customerIdCount.put(16L, 6252);
		customerIdCount.put(17L, 36);
		customerIdCount.put(18L, 703);
		customerIdCount.put(19L, 89);
		customerIdCount.put(20L, 17);
		customerIdCount.put(21L, 9);
		customerIdCount.put(22L, 369);
		customerIdCount.put(23L, 16);
		customerIdCount.put(24L, 57);
		customerIdCount.put(26L, 261);
		customerIdCount.put(27L, 61);
		customerIdCount.put(28L, 43);
		customerIdCount.put(29L, 7);
		customerIdCount.put(30L, 4);
		customerIdCount.put(31L, 55);
		customerIdCount.put(32L, 858);
		customerIdCount.put(33L, 214);
		customerIdCount.put(34L, 1812);
		customerIdCount.put(36L, 19);
		customerIdCount.put(37L, 16);
		customerIdCount.put(38L, 34);
		customerIdCount.put(39L, 12);
		customerIdCount.put(40L, 2);
		customerIdCount.put(41L, 131);
		customerIdCount.put(42L, 77);
		customerIdCount.put(43L, 363);
		customerIdCount.put(44L, 430);
		customerIdCount.put(45L, 1748);
		customerIdCount.put(46L, 64);
		customerIdCount.put(47L, 119);
		customerIdCount.put(48L, 136);
		customerIdCount.put(49L, 2);
		customerIdCount.put(50L, 19);
		customerIdCount.put(51L, 9);
		customerIdCount.put(52L, 70);
		customerIdCount.put(53L, 16);
		customerIdCount.put(54L, 187);
		customerIdCount.put(55L, 23);
		customerIdCount.put(56L, 73);
		customerIdCount.put(57L, 22);
		customerIdCount.put(58L, 826);
		customerIdCount.put(59L, 1);
		customerIdCount.put(60L, 2177);
		customerIdCount.put(61L, 442);
		customerIdCount.put(62L, 34);
		customerIdCount.put(63L, 8);
		customerIdCount.put(64L, 12);
		customerIdCount.put(65L, 178);
		customerIdCount.put(66L, 39);
		customerIdCount.put(67L, 128);
		customerIdCount.put(68L, 61);
		customerIdCount.put(69L, 414);
		customerIdCount.put(70L, 22);
		customerIdCount.put(71L, 3);
		customerIdCount.put(72L, 30);
		customerIdCount.put(73L, 760);
		customerIdCount.put(74L, 161);
		customerIdCount.put(75L, -1);
		customerIdCount.put(76L, 1309);
		customerIdCount.put(77L, 3800);
		customerIdCount.put(78L, 4361);
		customerIdCount.put(79L, 17);
		customerIdCount.put(80L, 12);
		customerIdCount.put(81L, 56);
		customerIdCount.put(82L, 165);
		customerIdCount.put(83L, 1404);
		customerIdCount.put(84L, 9);
		customerIdCount.put(85L, 100);
		customerIdCount.put(86L, 62);
		customerIdCount.put(87L, 27);
		customerIdCount.put(88L, 3);
		customerIdCount.put(89L, 360);
		customerIdCount.put(90L, 78);
		customerIdCount.put(91L, 48);
		customerIdCount.put(92L, 9);
		customerIdCount.put(93L, 1774);
		customerIdCount.put(94L, 18);
		customerIdCount.put(95L, 51);
		customerIdCount.put(96L, 88);
		customerIdCount.put(97L, 188);
		customerIdCount.put(98L, 76);
		customerIdCount.put(99L, 26);
		customerIdCount.put(100L, 33);
		customerIdCount.put(101L, 1152);
		customerIdCount.put(103L, 48);
		customerIdCount.put(104L, 83);
		customerIdCount.put(105L, 9);
		customerIdCount.put(106L, 62);
		customerIdCount.put(107L, 11);
		customerIdCount.put(108L, 193);
		customerIdCount.put(109L, 25);
		customerIdCount.put(110L, 3);
		customerIdCount.put(111L, 91);
		customerIdCount.put(112L, 27);
		customerIdCount.put(113L, 85);
		customerIdCount.put(114L, 64);
		customerIdCount.put(115L, 34);
		customerIdCount.put(116L, 173);
		customerIdCount.put(117L, 127);
		customerIdCount.put(118L, 110);
		customerIdCount.put(119L, 28);
		customerIdCount.put(120L, 19);
		customerIdCount.put(121L, 15);
		customerIdCount.put(122L, -3);
		customerIdCount.put(123L, 42);
		customerIdCount.put(124L, 31);
		customerIdCount.put(125L, 879);
		customerIdCount.put(126L, 933);
		customerIdCount.put(127L, 119);
		customerIdCount.put(128L, 69);
		customerIdCount.put(129L, 156);
		customerIdCount.put(130L, 1314);
		customerIdCount.put(131L, 2882);
		customerIdCount.put(132L, 149);
		customerIdCount.put(133L, 30);
		customerIdCount.put(134L, 50);
		customerIdCount.put(135L, 488);
		customerIdCount.put(136L, 3);
		customerIdCount.put(137L, 136);
		customerIdCount.put(138L, 118);
		customerIdCount.put(139L, 10);
		customerIdCount.put(140L, 60);
		customerIdCount.put(141L, 149);
		customerIdCount.put(142L, 431);
		customerIdCount.put(143L, 1523);
		customerIdCount.put(144L, 48);
		customerIdCount.put(145L, 205);
		customerIdCount.put(146L, 35);
		customerIdCount.put(147L, 18);
		customerIdCount.put(148L, 2431);
		customerIdCount.put(149L, 3375);
		customerIdCount.put(150L, 89);
		customerIdCount.put(151L, 18);
		customerIdCount.put(152L, 96);
		customerIdCount.put(153L, 92);
		customerIdCount.put(154L, 1671);
		customerIdCount.put(155L, 188);
		customerIdCount.put(156L, 169);
		customerIdCount.put(157L, 291);
		customerIdCount.put(158L, 10);
		customerIdCount.put(159L, 35);
		customerIdCount.put(160L, 31);
		customerIdCount.put(161L, 10);
		customerIdCount.put(162L, 4);
		customerIdCount.put(163L, 11);
		customerIdCount.put(165L, 116);
		customerIdCount.put(166L, 10);
		customerIdCount.put(167L, 800);
		customerIdCount.put(168L, 2110);
		customerIdCount.put(169L, 78);
		customerIdCount.put(170L, 1431);
		customerIdCount.put(171L, 11);
		customerIdCount.put(172L, 825);
		customerIdCount.put(173L, 46);
		customerIdCount.put(174L, 63);
		customerIdCount.put(175L, 74);
		customerIdCount.put(176L, 16);
		customerIdCount.put(177L, 220);
		customerIdCount.put(178L, 1347);
		customerIdCount.put(179L, 795);
		customerIdCount.put(180L, 10);
		customerIdCount.put(181L, 27);
		customerIdCount.put(182L, 84);
		customerIdCount.put(183L, 22);
		customerIdCount.put(184L, 0);
		customerIdCount.put(185L, 99);
		customerIdCount.put(186L, 25);
		customerIdCount.put(187L, 35);
		customerIdCount.put(188L, 8);
		customerIdCount.put(189L, 77);
		customerIdCount.put(190L, 118);
		customerIdCount.put(191L, 31);
		customerIdCount.put(192L, 1498);
		customerIdCount.put(193L, 40);
		customerIdCount.put(194L, 44);
		customerIdCount.put(195L, 31);
		customerIdCount.put(196L, 722);
		customerIdCount.put(197L, 310);
		customerIdCount.put(198L, 377);
		customerIdCount.put(199L, 345);
		customerIdCount.put(200L, 12);
		customerIdCount.put(201L, 381);
		customerIdCount.put(202L, 5061);
		customerIdCount.put(203L, 234);
		customerIdCount.put(204L, 8);
		customerIdCount.put(205L, 9);
		customerIdCount.put(206L, 8);
		customerIdCount.put(207L, 485);
		customerIdCount.put(208L, 823);
		customerIdCount.put(209L, 64);
		customerIdCount.put(210L, 1407);
		customerIdCount.put(211L, 3142);
		customerIdCount.put(212L, 41);
		customerIdCount.put(213L, 3);
		customerIdCount.put(214L, 23);
		customerIdCount.put(215L, 502);
		customerIdCount.put(216L, 174);
		customerIdCount.put(217L, 52);
		customerIdCount.put(218L, 7);
		customerIdCount.put(219L, 11);
		customerIdCount.put(220L, 232);
		customerIdCount.put(221L, 278);
		customerIdCount.put(222L, 61);
		customerIdCount.put(223L, 67);
		customerIdCount.put(224L, 14);
		customerIdCount.put(225L, 40);
		customerIdCount.put(226L, 675);
		customerIdCount.put(227L, 235);
		customerIdCount.put(228L, 30);
		customerIdCount.put(229L, 378);
		customerIdCount.put(230L, 43);
		customerIdCount.put(231L, 180);
		customerIdCount.put(232L, 14);
		customerIdCount.put(233L, 11);
		customerIdCount.put(234L, 607);
		customerIdCount.put(235L, 13);
		customerIdCount.put(236L, 1287);
		customerIdCount.put(237L, 588);
		customerIdCount.put(238L, 127);
		customerIdCount.put(239L, 135);
		customerIdCount.put(240L, 28);
		customerIdCount.put(241L, 82);
		customerIdCount.put(242L, 7);
		customerIdCount.put(243L, 266);
		customerIdCount.put(244L, 1493);
		customerIdCount.put(245L, 2314);
		customerIdCount.put(246L, 118);
		customerIdCount.put(247L, 28);
		customerIdCount.put(248L, 78);
		customerIdCount.put(249L, 14);
		customerIdCount.put(250L, 637);
		customerIdCount.put(251L, 390);
		customerIdCount.put(252L, 2023);
		customerIdCount.put(253L, 710);
		customerIdCount.put(254L, 15);
		customerIdCount.put(255L, 685);
		customerIdCount.put(256L, 27);
		customerIdCount.put(257L, 2);
		customerIdCount.put(258L, 1);
		customerIdCount.put(259L, 121);
		customerIdCount.put(260L, 1031);
		customerIdCount.put(261L, 55);
		customerIdCount.put(262L, 13);
		customerIdCount.put(263L, 38);
		customerIdCount.put(264L, 17);
		customerIdCount.put(265L, 127);
		customerIdCount.put(266L, 4);
		customerIdCount.put(267L, 86);
		customerIdCount.put(268L, 2);
		customerIdCount.put(269L, 1182);
		customerIdCount.put(270L, 53);
		customerIdCount.put(271L, 287);
		customerIdCount.put(272L, 357);
		customerIdCount.put(273L, 29);
		customerIdCount.put(274L, 23);
		customerIdCount.put(275L, 9);
		customerIdCount.put(276L, 187);
		customerIdCount.put(277L, 12);
		customerIdCount.put(278L, 102);
		customerIdCount.put(279L, 10);
		customerIdCount.put(280L, 211);
		customerIdCount.put(281L, 10);
		customerIdCount.put(282L, 66);
		customerIdCount.put(284L, 639);
		customerIdCount.put(285L, 25);
		customerIdCount.put(286L, 2);

	}

}
