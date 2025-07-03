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
     * <p>Finds the n-th index within a String, handling {@code null}.
     * This method uses {@link String#indexOf(String)} if possible.</p>
     * <p>Note that matches may overlap<p>
     *
     * <p>A {@code null} CharSequence will return {@code -1}.</p>
     *
     * @param str  the CharSequence to check, may be null
     * @param searchStr  the CharSequence to find, may be null
     * @param ordinal  the n-th {@code searchStr} to find, overlapping matches are allowed.
     * @param lastIndex true if lastOrdinalIndexOf() otherwise false if ordinalIndexOf()
     * @return the n-th index of the search CharSequence,
     *  {@code -1} ({@code INDEX_NOT_FOUND}) if no match or {@code null} string input
     */
    // Shared code between ordinalIndexOf(String, String, int) and lastOrdinalIndexOf(String, String, int)
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
                index = CharSequenceUtils.lastIndexOf(str, searchStr, index - 1); // step backwards thru string
            } else {
                index = CharSequenceUtils.indexOf(str, searchStr, index + 1); // step forwards through string
            }
            if (index < 0) {
                return index;
            }
            found++;
        } while (found < ordinal);
        return index;
    }

}
