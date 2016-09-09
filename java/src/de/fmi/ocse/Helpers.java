package de.fmi.ocse;

import java.util.ArrayList;

/**
 * Created by daniel on 22.09.16.
 */
public class Helpers {

	public class DebugCost {
		static final int none = 0;
		static final int cheap = 1;
		static final int normal = 2;
		static final int expensive = 4;
	}
	public class DebugType {
		static final int constant = 0;
		static final int modifies_output = 1;
		static final int print_debug_msg = 2;
	}

	public static boolean is_strongly_monotone_ascending(ArrayList<Integer> l) {
		if (l.size() > 0) {
			for (int i = 1, s = l.size(); i < s; ++i) {
				if (l.get(i - 1) >= l.get(i)) {
					return false;
				}
			}
		}
		return true;
	}
	//l must be sorted
	public static ArrayList<Integer> make_unique(ArrayList<Integer> l) {
		ArrayList<Integer> out = new ArrayList<Integer>();
		if (l.size() > 0) {
			int last = l.get(0);
			out.add(last);
			for(int i = 1, s = l.size(); i < s; ++i) {
				int next = l.get(i);
				if (last != next) {
					out.add(next);
					last = next;
				}
			}
		}
		return out;
	}
	public static boolean is_strongly_monotone_ascending(int[] l) {
		if (l.length > 0) {
			for(int i = 1; i < l.length; ++i) {
				if (l[i-1] >= l[i]) {
					return false;
				}
			}
		}
		return true;
	}
}
