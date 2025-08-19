/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.api.utils;

import java.util.*;

public class StringUtils {

    private static final int NOT_FOUND = -1;

    private static final int TO_STRING_LIMIT = 16;

    public static final String EMPTY = "";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final int INDEX_NOT_FOUND = -1;

    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String defaultString(String value) {
        if (null == value) {
            return "";
        }
        return value;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean equals(String str1, String str2) {
        return Objects.equals(str1, str2);
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        return equals(str1, str2) || (str1 != null && str1.equalsIgnoreCase(str2));
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isAllEmpty(String... str) {
        for (String s : str) {
            if (!isEmpty(s)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isAnyEmpty(String... str) {
        for (String s : str) {
            if (isEmpty(s)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static String replace(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    /**
     * 替换字符串
     *
     * @param text         text
     * @param searchString searchString
     * @param replacement  replacement
     * @param max          max
     * @return 结果
     */
    public static String replace(String text, String searchString, String replacement, int max) {
        if (!isEmpty(text) && !isEmpty(searchString) && replacement != null && max != 0) {
            int start = 0;
            int end = text.indexOf(searchString, start);
            if (end == -1) {
                return text;
            } else {
                int replLength = searchString.length();
                int increase = replacement.length() - replLength;
                increase = increase < 0 ? 0 : increase;
                increase *= max < 0 ? 16 : (max > 64 ? 64 : max);

                StringBuilder buf = new StringBuilder(text.length() + increase);
                for (; end != -1; end = text.indexOf(searchString, start)) {
                    buf.append(text.substring(start, end)).append(replacement);
                    start = end + replLength;
                    --max;
                    if (max == 0) {
                        break;
                    }
                }

                buf.append(text.substring(start));
                return buf.toString();
            }
        } else {
            return text;
        }
    }

    /**
     * Test whether the given string matches the given substring
     * at the given index.
     *
     * @param str       the original string (or StringBuilder)
     * @param index     the index in the original string to start matching against
     * @param substring the substring to match at the given index
     */
    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copy from spring-core.
     * {@link org.springframework.util.StringUtils#delimitedListToStringArray(java.lang.String, java.lang.String)}
     */
    public static String[] delimitedListToStringArray(String str, String delimiter) {
        return delimitedListToStringArray(str, delimiter, (String) null);
    }

    /**
     * Copy from spring-core.
     * {@link org.springframework.util.StringUtils#delimitedListToStringArray(java.lang.String, java.lang.String, java.lang.String)}
     */
    public static String[] delimitedListToStringArray(String str, String delimiter, String charsToDelete) {
        if (str == null) {
            return EMPTY_STRING_ARRAY;
        } else if (delimiter == null) {
            return new String[]{str};
        } else {
            List<String> result = new ArrayList();
            int pos;
            if (delimiter.isEmpty()) {
                for (pos = 0; pos < str.length(); ++pos) {
                    result.add(deleteAny(str.substring(pos, pos + 1), charsToDelete));
                }
            } else {
                int delPos;
                for (pos = 0; (delPos = str.indexOf(delimiter, pos)) != -1; pos = delPos + delimiter.length()) {
                    result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
                }

                if (str.length() > 0 && pos <= str.length()) {
                    result.add(deleteAny(str.substring(pos), charsToDelete));
                }
            }

            return toStringArray((Collection) result);
        }
    }

    /**
     * Copy from spring-core.
     * {@link org.springframework.util.StringUtils#toStringArray(java.util.Collection<java.lang.String>)}
     */
    public static String[] toStringArray(Collection<String> collection) {
        return !CollectionUtils.isEmpty(collection) ? (String[]) collection.toArray(EMPTY_STRING_ARRAY) :
                EMPTY_STRING_ARRAY;
    }

    /**
     * Copy from spring-core.
     * {@link org.springframework.util.StringUtils#deleteAny}
     */
    public static String deleteAny(String inString, String charsToDelete) {
        if (isNotBlank(inString) && isNotBlank(charsToDelete)) {
            int lastCharIndex = 0;
            char[] result = new char[inString.length()];

            for (int i = 0; i < inString.length(); ++i) {
                char c = inString.charAt(i);
                if (charsToDelete.indexOf(c) == -1) {
                    result[lastCharIndex++] = c;
                }
            }

            if (lastCharIndex == inString.length()) {
                return inString;
            } else {
                return new String(result, 0, lastCharIndex);
            }
        } else {
            return inString;
        }
    }

    /**
     * Copy from spring-core.
     * {@link org.springframework.util.StringUtils#arrayToDelimitedString}
     */
    public static String arrayToDelimitedString(Object[] arr, String delim) {
        if (CollectionUtils.isEmpty(arr)) {
            return "";
        } else if (arr.length == 1) {
            return nullSafeToString(arr[0]);
        } else {
            StringJoiner sj = new StringJoiner(delim);
            Object[] var3 = arr;
            int var4 = arr.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                Object elem = var3[var5];
                sj.add(String.valueOf(elem));
            }

            return sj.toString();
        }
    }

    /**
     * Copy from spring-core.
     * {@link org.springframework.util.ObjectUtils#nullSafeToString(java.lang.Object)}
     */
    public static String nullSafeToString(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Object[]) {
            return nullSafeToString((Object[]) ((Object[]) obj));
        } else if (obj instanceof boolean[]) {
            return nullSafeToString((boolean[]) ((boolean[]) obj));
        } else if (obj instanceof byte[]) {
            return nullSafeToString((byte[]) ((byte[]) obj));
        } else if (obj instanceof char[]) {
            return nullSafeToString((char[]) ((char[]) obj));
        } else if (obj instanceof double[]) {
            return nullSafeToString((double[]) ((double[]) obj));
        } else if (obj instanceof float[]) {
            return nullSafeToString((float[]) ((float[]) obj));
        } else if (obj instanceof int[]) {
            return nullSafeToString((int[]) ((int[]) obj));
        } else if (obj instanceof long[]) {
            return nullSafeToString((long[]) ((long[]) obj));
        } else if (obj instanceof short[]) {
            return nullSafeToString((short[]) ((short[]) obj));
        } else {
            String str = obj.toString();
            return str != null ? str : EMPTY;
        }
    }

    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }

    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if (end > str.length()) {
            end = str.length();
        }

        // if start is greater than end, return ""
        if (start > end) {
            return EMPTY;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }

    public static boolean startsWith(String str, String prefix) {
        return startsWith(str, prefix, false);
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return startsWith(str, prefix, true);
    }

    private static boolean startsWith(String str, String prefix, boolean ignoreCase) {
        if (str == null || prefix == null) {
            return (str == null && prefix == null);
        }
        if (prefix.length() > str.length()) {
            return false;
        }
        return str.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
    }

    public static boolean endsWith(String str, String suffix) {
        return endsWith(str, suffix, false);
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        return endsWith(str, suffix, true);
    }

    private static boolean endsWith(String str, String suffix, boolean ignoreCase) {
        if (str == null || suffix == null) {
            return (str == null && suffix == null);
        }
        if (suffix.length() > str.length()) {
            return false;
        }
        int strOffset = str.length() - suffix.length();
        return str.regionMatches(ignoreCase, strOffset, suffix, 0, suffix.length());
    }

    public static String upperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }

    public static String[] split(String original, String separator) {
        if (original == null) {
            return null;
        }
        return original.split(separator);
    }

    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    public static int ordinalIndexOf(final CharSequence str, final CharSequence searchStr, final int ordinal) {
        return ordinalIndexOf(str, searchStr, ordinal, false);
    }

    /**
     * Copy from commons-lang3.
     * {@link org.apache.commons.lang3.StringUtils#ordinalIndexOf(java.lang.CharSequence, java.lang.CharSequence, int, boolean)}
     */
    private static int ordinalIndexOf(final CharSequence str, final CharSequence searchStr, final int ordinal, final boolean lastIndex) {
        if (str == null || searchStr == null || ordinal <= 0) {
            return INDEX_NOT_FOUND;
        }
        if (searchStr.length() == 0) {
            return lastIndex ? str.length() : 0;
        }
        int found = 0;
        // set the initial index beyond the end of the string
        // this is to allow for the initial index decrement/increment
        int index = lastIndex ? str.length() : INDEX_NOT_FOUND;
        do {
            if (lastIndex) {
                index = lastIndexOf(str, searchStr, index - 1); // step backwards thru string
            } else {
                index = indexOf(str, searchStr, index + 1); // step forwards through string
            }
            if (index < 0) {
                return index;
            }
            found++;
        } while (found < ordinal);
        return index;
    }

    /**
     * Copy from commons-lang3.
     * {@link org.apache.commons.lang3.CharSequenceUtils#lastIndexOf(java.lang.CharSequence, java.lang.CharSequence, int)}
     */
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

    /**
     * copy from commons-lang3.
     * {@link org.apache.commons.lang3.CharSequenceUtils#lastIndexOf(java.lang.CharSequence, int, int)}
     */
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

    /**
     * copy from commons-lang3.
     * {link org.apache.commons.lang3.CharSequenceUtils#checkLaterThan1(java.lang.CharSequence, java.lang.CharSequence, int, int)}
     */
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

    /**
     * copy from commons-lang3.
     * {@link org.apache.commons.lang3.CharSequenceUtils#indexOf(java.lang.CharSequence, java.lang.CharSequence, int)}
     */
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


    public static boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.indexOf(searchStr) >= 0;
    }

    /**
     * copy from commons-lang3.
     * {@link org.apache.commons.lang3.StringUtils#split(java.lang.String, java.lang.String, int)}
     */
    public static String[] split(final String str, final String separatorChars, final int max) {
        return splitWorker(str, separatorChars, max, false);
    }

    /**
     * copy from commons-lang3.
     * {@link org.apache.commons.lang3.StringUtils#splitWorker(java.lang.String, java.lang.String, int, boolean)}
     */
    private static String[] splitWorker(final String str, final String separatorChars, final int max, final boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)
        // Direct code is quicker than StringTokenizer.
        // Also, StringTokenizer uses isSpace() not isWhitespace()

        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<>();
        int sizePlus1 = 1;
        int i = 0;
        int start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null) {
            // Null separator means use whitespace
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else if (separatorChars.length() == 1) {
            // Optimise 1 character case
            final char sep = separatorChars.charAt(0);
            while (i < len) {
                if (str.charAt(i) == sep) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else {
            // standard case
            while (i < len) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        if (match || preserveAllTokens && lastMatch) {
            list.add(str.substring(start, i));
        }
        return list.toArray(EMPTY_STRING_ARRAY);
    }

}
