package com.tencent.polaris.api.utils;

public class CharSequenceUtils {

	private static final int NOT_FOUND = -1;

	private static final int TO_STRING_LIMIT = 16;

	static int lastIndexOf(final CharSequence cs, final int searchChar, int start) {
		if (cs instanceof String) {
			return ((String) cs).lastIndexOf(searchChar, start);
		}
		final int sz = cs.length();
		if (start < 0) {
			return NOT_FOUND;
		}
		if (start >= sz) {
			start = sz - 1;
		}
		if (searchChar < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
			for (int i = start; i >= 0; --i) {
				if (cs.charAt(i) == searchChar) {
					return i;
				}
			}
			return NOT_FOUND;
		}
		//supplementary characters (LANG1300)
		//NOTE - we must do a forward traversal for this to avoid duplicating code points
		if (searchChar <= Character.MAX_CODE_POINT) {
			final char[] chars = Character.toChars(searchChar);
			//make sure it's not the last index
			if (start == sz - 1) {
				return NOT_FOUND;
			}
			for (int i = start; i >= 0; i--) {
				final char high = cs.charAt(i);
				final char low = cs.charAt(i + 1);
				if (chars[0] == high && chars[1] == low) {
					return i;
				}
			}
		}
		return NOT_FOUND;
	}

	static int lastIndexOf(final CharSequence cs, final CharSequence searchChar, int start) {
		if (searchChar == null || cs == null) {
			return NOT_FOUND;
		}
		if (searchChar instanceof String) {
			if (cs instanceof String) {
				return ((String) cs).lastIndexOf((String) searchChar, start);
			} else if (cs instanceof StringBuilder) {
				return ((StringBuilder) cs).lastIndexOf((String) searchChar, start);
			} else if (cs instanceof StringBuffer) {
				return ((StringBuffer) cs).lastIndexOf((String) searchChar, start);
			}
		}

		final int len1 = cs.length();
		final int len2 = searchChar.length();

		if (start > len1) {
			start = len1;
		}

		if (start < 0 || len2 < 0 || len2 > len1) {
			return NOT_FOUND;
		}

		if (len2 == 0) {
			return start;
		}

		if (len2 <= TO_STRING_LIMIT) {
			if (cs instanceof String) {
				return ((String) cs).lastIndexOf(searchChar.toString(), start);
			} else if (cs instanceof StringBuilder) {
				return ((StringBuilder) cs).lastIndexOf(searchChar.toString(), start);
			} else if (cs instanceof StringBuffer) {
				return ((StringBuffer) cs).lastIndexOf(searchChar.toString(), start);
			}
		}

		if (start + len2 > len1) {
			start = len1 - len2;
		}

		final char char0 = searchChar.charAt(0);

		int i = start;
		while (true) {
			while (cs.charAt(i) != char0) {
				i--;
				if (i < 0) {
					return NOT_FOUND;
				}
			}
			if (checkLaterThan1(cs, searchChar, len2, i)) {
				return i;
			}
			i--;
			if (i < 0) {
				return NOT_FOUND;
			}
		}
	}

	private static boolean checkLaterThan1(final CharSequence cs, final CharSequence searchChar, final int len2, final int start1) {
		for (int i = 1, j = len2 - 1; i <= j; i++, j--) {
			if (cs.charAt(start1 + i) != searchChar.charAt(i)
					||
					cs.charAt(start1 + j) != searchChar.charAt(j)
			) {
				return false;
			}
		}
		return true;
	}

	static int indexOf(final CharSequence cs, final CharSequence searchChar, final int start) {
		if (cs instanceof String) {
			return ((String) cs).indexOf(searchChar.toString(), start);
		} else if (cs instanceof StringBuilder) {
			return ((StringBuilder) cs).indexOf(searchChar.toString(), start);
		} else if (cs instanceof StringBuffer) {
			return ((StringBuffer) cs).indexOf(searchChar.toString(), start);
		}
		return cs.toString().indexOf(searchChar.toString(), start);
	}
}
