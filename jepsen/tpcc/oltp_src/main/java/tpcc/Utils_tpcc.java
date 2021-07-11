package tpcc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils_tpcc {

	static AtomicBoolean atomicInitialized = new AtomicBoolean(false);
	static boolean waitForInit = true;
	static int scale = -10;
	private static Random r = new Random();

	public final static int configCommitCount = 1000; // commit every n records
	public final static int configWhseCount = 1;
	public final static int configItemCount = 1; // tpc-c std = 100,000
	public final static int configDistPerWhse = 2; // tpc-c std = 10
	public final static int configCustPerDist = 3; // tpc-c std = 3,000

	public final static int h_amount = 10;
	public final static int w_ytd = h_amount * configCustPerDist * configDistPerWhse;
	public final static int d_ytd = h_amount * configCustPerDist;

	// this function will be -dynamically- called from clojure at runtime
	public Utils_tpcc(int scale) {
		Utils_tpcc.scale = scale;
		Utils_tpcc.initialize(scale);
	}

	/*
	 * 
	 * INITIALIZATION CODE
	 * 
	 */
	public static void initialize(int scale) {
		if (atomicInitialized.compareAndSet(false, true)) {
			System.out.println("Utils_tpcc_" + scale + ": intializing data structures....");

			/*
			 * 
			 * THIS IS WHERE YOUR INITIALIZATION CODE GOES
			 * 
			 */

			waitForInit = false;
		} else {
			while (waitForInit)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	/*
	 * 
	 * AUXILIARY CODE
	 * 
	 */

	public static int get_w_id() {
		// return ThreadLocalRandom.current().nextInt(1, scale + 1);
		return ThreadLocalRandom.current().nextInt(1, configWhseCount + 1);
	}

	public static int get_d_id() {
		// return ThreadLocalRandom.current().nextInt(1, 11);
		return ThreadLocalRandom.current().nextInt(1, configDistPerWhse + 1);
	}

	public static int get_c_id() {
		// return ThreadLocalRandom.current().nextInt(1, 3001);
		return ThreadLocalRandom.current().nextInt(1, configCustPerDist + 1);
	}

	public static int get_i_id() {
		// return ThreadLocalRandom.current().nextInt(1, 1001);
		return ThreadLocalRandom.current().nextInt(1, configItemCount + 1);
	}

	public static int get_num_items() {
		return ThreadLocalRandom.current().nextInt(5, 16);
	}

	public static int[] get_item_ids(int num_items) {
		int[] itemIDs = new int[num_items];
		for (int i = 0; i < num_items; i++)
			itemIDs[i] = Utils_tpcc.get_i_id();
		return itemIDs;
	}

	public static int[] get_order_quantities(int num_items) {
		int[] orderQuantities = new int[num_items];
		for (int i = 0; i < num_items; i++) {
			int j = ThreadLocalRandom.current().nextInt(1, 11);
			orderQuantities[i] = j;
			
		}
		
		return orderQuantities;
	}

	public static List<Object> get_sup_wh_and_o_all_local(int num_items, int terminal_w_id) {
		List<Object> result = new ArrayList<Object>();
		int allLocal = 1;
		int[] supplierWarehouseIDs = new int[num_items];
		for (int i = 0; i < num_items; i++)
			if (ThreadLocalRandom.current().nextInt(1, 101) > 1)
				supplierWarehouseIDs[i] = terminal_w_id;
			else {
				do
					supplierWarehouseIDs[i] = Utils_tpcc.get_w_id();
				while (supplierWarehouseIDs[i] == terminal_w_id && scale > 1);
				allLocal = 0;
			}

		result.add(supplierWarehouseIDs);
		result.add(allLocal);
		return result;
	}

	public static int get_paymentAmount() {
		return ThreadLocalRandom.current().nextInt(1, 5000);
	}

	public static List<Integer> get_customerinfo(int w_id, int d_id) {
		int customerWarehouseID = w_id;
		int customerDistrictID = d_id;
		List<Integer> result = new ArrayList<Integer>();
		if (ThreadLocalRandom.current().nextInt(1, 101) <= 15)
			do {
				customerWarehouseID = Utils_tpcc.get_w_id();
				customerDistrictID = Utils_tpcc.get_d_id();
			} while (customerWarehouseID == w_id && scale > 1);
		result.add(customerWarehouseID);
		result.add(customerDistrictID);
		return result;
	}

	/*
	 * imported from OLTPBench
	 */
	private final static String[] nameTokens = { "BAR", "OUGHT", "ABLE", "PRI", "PRES", "ESE", "ANTI", "CALLY", "ATION",
			"EING" };

	private static String getLastName(int num) {
		return nameTokens[num / 100] + nameTokens[(num / 10) % 10] + nameTokens[num % 10];
	}

	private static final int C_LAST_RUN_C = 223; // in range [0, 255]

	private static int randomNumber(int min, int max, Random r) {
		return (int) (r.nextDouble() * (max - min + 1) + min);
	}

	private static int nonUniformRandom(int A, int C, int min, int max, Random r) {
		return (((randomNumber(0, A, r) | randomNumber(min, max, r)) + C) % (max - min + 1)) + min;
	}

	private static String getNonUniformRandomLastNameForRun(Random r) {
		return getLastName(nonUniformRandom(255, C_LAST_RUN_C, 0, 999, r));
	}
	/*
	 * END of imported from OLTPBench
	 */

	public static String get_cust_last_name() {
		return getNonUniformRandomLastNameForRun(r);
	}

	public static List<Object> get_payment_cust() {
		// returns a 3-element list consisting of customerByName flag, c_id int and
		// c_last string
		List<Object> result = new ArrayList<Object>();
		if (ThreadLocalRandom.current().nextInt(1, 101) >= 60) {
			// by id
			result.add(false);
			result.add(Utils_tpcc.get_c_id());
			result.add("");
		} else {
			// by last name
			result.add(true);
			result.add(-1);
			result.add(Utils_tpcc.get_cust_last_name());
		}
		return result;
	}

	public static List<Object> get_orderStatus_cust() {
		// returns a 3-element list consisting of customerByName flag, c_id int and
		// c_last string
		List<Object> result = new ArrayList<Object>();
		if (ThreadLocalRandom.current().nextInt(1, 101) >= 60) {
			// by id
			result.add(false);
			result.add(Utils_tpcc.get_c_id());
			result.add("");
		} else {
			// by last name
			result.add(true);
			result.add(-1);
			result.add(Utils_tpcc.get_cust_last_name());
		}
		return result;
	}

	public static int get_o_carrier_id() {
		return ThreadLocalRandom.current().nextInt(1, 11);
	}

	public static double get_threshold() {
		return ThreadLocalRandom.current().nextInt(10, 21);
	}

	/*
	 * 
	 * Helping Functions
	 * 
	 */

	public static String get_in_clause(List<Integer> input_list) {
		String result = "(", delim = "";
		for (int i : input_list) {
			result += (delim + i);
			delim = ",";
		}
		return (result + ")");
	}

}
