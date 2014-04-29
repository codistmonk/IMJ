/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package pixel3d;

import java.io.Serializable;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * This class implements the Dual-Pivot Quicksort algorithm by
 * Vladimir Yaroslavskiy, Jon Bentley, and Josh Bloch. The algorithm
 * offers O(n log(n)) performance on many data sets that cause other
 * quicksorts to degrade to quadratic performance, and is typically
 * faster than traditional (one-pivot) Quicksort implementations.
 *
 * @author Vladimir Yaroslavskiy
 * @author Jon Bentley
 * @author Josh Bloch
 * @author codistmonk (modifications since 2014-04-29)
 *
 * @version 2011.02.11 m765.827.12i:5\7pm
 */
public final class DualPivotQuicksort {

    /**
     * Prevents instantiation.
     */
    private DualPivotQuicksort() {
    	throw new IllegalInstantiationException();
    }

    /*
     * Tuning parameters.
     */

    /**
     * The maximum number of runs in merge sort.
     */
    private static final int MAX_RUN_COUNT = 67;

    /**
     * The maximum length of run in merge sort.
     */
    private static final int MAX_RUN_LENGTH = 33;

    /**
     * If the length of an array to be sorted is less than this
     * constant, Quicksort is used in preference to merge sort.
     */
    private static final int QUICKSORT_THRESHOLD = 286;

    /**
     * If the length of an array to be sorted is less than this
     * constant, insertion sort is used in preference to Quicksort.
     */
    private static final int INSERTION_SORT_THRESHOLD = 47;

    /*
     * Sorting methods for seven primitive types.
     */

    /**
     * Sorts the specified array.
     *
     * @param values the array to be sorted
     */
    public static final void sort(final int[] values, final IntComparator comparator) {
        sort(values, 0, values.length - 1, comparator);
    }

    /**
     * Sorts the specified range of the array.
     *
     * @param values the array to be sorted
     * @param left the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
     */
    public static final void sort(final int[] values, final int left, final int right, final IntComparator comparator) {
        // Use Quicksort on small arrays
        if (right - left < QUICKSORT_THRESHOLD) {
            sort(values, left, right, true, comparator);
            return;
        }

        /*
         * Index run[i] is the start of i-th run
         * (ascending or descending sequence).
         */
        int[] run = new int[MAX_RUN_COUNT + 1];
        int count = 0; run[0] = left;
        int[] array = values;
        int r = right;

        // Check if the array is nearly sorted
        for (int k = left; k < r; run[count] = k) {
            if (comparator.compare(array[k], array[k + 1]) < 0) { // ascending
                while (++k <= r && comparator.compare(array[k - 1], array[k]) <= 0);
            } else if (comparator.compare(array[k], array[k + 1]) > 0) { // descending
                while (++k <= r && comparator.compare(array[k - 1], array[k]) >= 0);
                for (int lo = run[count] - 1, hi = k; ++lo < --hi; ) {
                    int t = array[lo]; array[lo] = array[hi]; array[hi] = t;
                }
            } else { // equal
                for (int m = MAX_RUN_LENGTH; ++k <= r && comparator.compare(array[k - 1], array[k]) == 0; ) {
                    if (--m == 0) {
                        sort(array, left, r, true, comparator);
                        return;
                    }
                }
            }

            /*
             * The array is not highly structured,
             * use Quicksort instead of merge sort.
             */
            if (++count == MAX_RUN_COUNT) {
                sort(array, left, r, true, comparator);
                return;
            }
        }

        // Check special cases
        if (run[count] == r++) { // The last run contains one element
            run[++count] = r;
        } else if (count == 1) { // The array is already sorted
            return;
        }

        /*
         * Create temporary array, which is used for merging.
         * Implementation note: variable "right" is increased by 1.
         */
        int[] b; byte odd = 0;
        for (int n = 1; (n <<= 1) < count; odd ^= 1);

        if (odd == 0) {
            b = array; array = new int[b.length];
            for (int i = left - 1; ++i < r; array[i] = b[i]);
        } else {
            b = new int[array.length];
        }

        // Merging
        for (int last; count > 1; count = last) {
            for (int k = (last = 0) + 2; k <= count; k += 2) {
                int hi = run[k], mi = run[k - 1];
                for (int i = run[k - 2], p = i, q = mi; i < hi; ++i) {
                    if (q >= hi || p < mi && comparator.compare(array[p], array[q]) <= 0) {
                        b[i] = array[p++];
                    } else {
                        b[i] = array[q++];
                    }
                }
                run[++last] = hi;
            }
            if ((count & 1) != 0) {
                for (int i = r, lo = run[count - 1]; --i >= lo;
                    b[i] = array[i]
                );
                run[++last] = r;
            }
            int[] t = array; array = b; b = t;
        }
    }

    /**
     * Sorts the specified range of the array by Dual-Pivot Quicksort.
     *
     * @param values the array to be sorted
     * @param left the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
     * @param leftmost indicates if this part is the leftmost in the range
     */
    private static final void sort(final int[] values, final int left, final int right, final boolean leftmost, final IntComparator comparator) {
        final int length = right - left + 1;
        int l = left;
        int r = right;

        // Use insertion sort on tiny arrays
        if (length < INSERTION_SORT_THRESHOLD) {
            if (leftmost) {
                /*
                 * Traditional (without sentinel) insertion sort,
                 * optimized for server VM, is used in case of
                 * the leftmost part.
                 */
                for (int i = l, j = i; i < r; j = ++i) {
                    int ai = values[i + 1];
                    while (comparator.compare(ai, values[j]) < 0) {
                        values[j + 1] = values[j];
                        if (j-- == l) {
                            break;
                        }
                    }
                    values[j + 1] = ai;
                }
            } else {
                /*
                 * Skip the longest ascending sequence.
                 */
                do {
                    if (l >= r) {
                        return;
                    }
                } while (comparator.compare(values[++l], values[l - 1]) >= 0);

                /*
                 * Every element from adjoining part plays the role
                 * of sentinel, therefore this allows us to avoid the
                 * left range check on each iteration. Moreover, we use
                 * the more optimized algorithm, so called pair insertion
                 * sort, which is faster (in the context of Quicksort)
                 * than traditional implementation of insertion sort.
                 */
                for (int k = l; ++l <= r; k = ++l) {
                    int a1 = values[k], a2 = values[l];

                    if (comparator.compare(a1, a2) < 0) {
                        a2 = a1; a1 = values[l];
                    }
                    while (comparator.compare(a1, values[--k]) < 0) {
                        values[k + 2] = values[k];
                    }
                    values[++k + 1] = a1;

                    while (comparator.compare(a2, values[--k]) < 0) {
                        values[k + 1] = values[k];
                    }
                    values[k + 1] = a2;
                }
                int last = values[r];

                while (comparator.compare(last, values[--r]) < 0) {
                    values[r + 1] = values[r];
                }
                values[r + 1] = last;
            }
            return;
        }

        // Inexpensive approximation of length / 7
        int seventh = (length >> 3) + (length >> 6) + 1;

        /*
         * Sort five evenly spaced elements around (and including) the
         * center element in the range. These elements will be used for
         * pivot selection as described below. The choice for spacing
         * these elements was empirically determined to work well on
         * a wide variety of inputs.
         */
        int e3 = (l + r) >>> 1; // The midpoint
        int e2 = e3 - seventh;
        int e1 = e2 - seventh;
        int e4 = e3 + seventh;
        int e5 = e4 + seventh;

        // Sort these elements using insertion sort
        if (comparator.compare(values[e2], values[e1]) < 0) { int t = values[e2]; values[e2] = values[e1]; values[e1] = t; }

        if (comparator.compare(values[e3], values[e2]) < 0) { int t = values[e3]; values[e3] = values[e2]; values[e2] = t;
            if (comparator.compare(t, values[e1]) < 0) { values[e2] = values[e1]; values[e1] = t; }
        }
        if (comparator.compare(values[e4], values[e3]) < 0) { int t = values[e4]; values[e4] = values[e3]; values[e3] = t;
            if (comparator.compare(t, values[e2]) < 0) { values[e3] = values[e2]; values[e2] = t;
                if (comparator.compare(t, values[e1]) < 0) { values[e2] = values[e1]; values[e1] = t; }
            }
        }
        if (comparator.compare(values[e5], values[e4]) < 0) { int t = values[e5]; values[e5] = values[e4]; values[e4] = t;
            if (comparator.compare(t, values[e3]) < 0) { values[e4] = values[e3]; values[e3] = t;
                if (comparator.compare(t, values[e2]) < 0) { values[e3] = values[e2]; values[e2] = t;
                    if (comparator.compare(t, values[e1]) < 0) { values[e2] = values[e1]; values[e1] = t; }
                }
            }
        }

        // Pointers
        int less  = l;  // The index of the first element of center part
        int great = r; // The index before the first element of right part

        if (comparator.compare(values[e1], values[e2]) != 0
        		&& comparator.compare(values[e2], values[e3]) != 0
        		&& comparator.compare(values[e3], values[e4]) != 0
        		&& comparator.compare(values[e4], values[e5]) != 0) {
            /*
             * Use the second and fourth of the five sorted elements as pivots.
             * These values are inexpensive approximations of the first and
             * second terciles of the array. Note that pivot1 <= pivot2.
             */
            int pivot1 = values[e2];
            int pivot2 = values[e4];

            /*
             * The first and the last elements to be sorted are moved to the
             * locations formerly occupied by the pivots. When partitioning
             * is complete, the pivots are swapped back into their final
             * positions, and excluded from subsequent sorting.
             */
            values[e2] = values[l];
            values[e4] = values[r];

            /*
             * Skip elements, which are less or greater than pivot values.
             */
            while (comparator.compare(values[++less], pivot1) < 0);
            while (comparator.compare(values[--great], pivot2) > 0);

            /*
             * Partitioning:
             *
             *   left part           center part                   right part
             * +--------------------------------------------------------------+
             * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
             * +--------------------------------------------------------------+
             *               ^                          ^       ^
             *               |                          |       |
             *              less                        k     great
             *
             * Invariants:
             *
             *              all in (left, less)   < pivot1
             *    pivot1 <= all in [less, k)     <= pivot2
             *              all in (great, right) > pivot2
             *
             * Pointer k is the first index of ?-part.
             */
            outer:
            for (int k = less - 1; ++k <= great; ) {
                int ak = values[k];
                if (comparator.compare(ak, pivot1) < 0) { // Move a[k] to left part
                    values[k] = values[less];
                    /*
                     * Here and below we use "a[i] = b; i++;" instead
                     * of "a[i++] = b;" due to performance issue.
                     */
                    values[less] = ak;
                    ++less;
                } else if (comparator.compare(ak, pivot2) > 0) { // Move a[k] to right part
                    while (comparator.compare(values[great], pivot2) > 0) {
                        if (great-- == k) {
                            break outer;
                        }
                    }
                    if (comparator.compare(values[great], pivot1) < 0) { // a[great] <= pivot2
                        values[k] = values[less];
                        values[less] = values[great];
                        ++less;
                    } else { // pivot1 <= a[great] <= pivot2
                        values[k] = values[great];
                    }
                    /*
                     * Here and below we use "a[i] = b; i--;" instead
                     * of "a[i--] = b;" due to performance issue.
                     */
                    values[great] = ak;
                    --great;
                }
            }

            // Swap pivots into their final positions
            values[l]  = values[less  - 1]; values[less  - 1] = pivot1;
            values[r] = values[great + 1]; values[great + 1] = pivot2;

            // Sort left and right parts recursively, excluding known pivots
            sort(values, l, less - 2, leftmost, comparator);
            sort(values, great + 2, r, false, comparator);

            /*
             * If center part is too large (comprises > 4/7 of the array),
             * swap internal pivot values to ends.
             */
            if (less < e1 && e5 < great) {
                /*
                 * Skip elements, which are equal to pivot values.
                 */
                while (comparator.compare(values[less], pivot1) == 0) {
                    ++less;
                }

                while (comparator.compare(values[great], pivot2) == 0) {
                    --great;
                }

                /*
                 * Partitioning:
                 *
                 *   left part         center part                  right part
                 * +----------------------------------------------------------+
                 * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
                 * +----------------------------------------------------------+
                 *              ^                        ^       ^
                 *              |                        |       |
                 *             less                      k     great
                 *
                 * Invariants:
                 *
                 *              all in (*,  less) == pivot1
                 *     pivot1 < all in [less,  k)  < pivot2
                 *              all in (great, *) == pivot2
                 *
                 * Pointer k is the first index of ?-part.
                 */
                outer:
                for (int k = less - 1; ++k <= great; ) {
                    int ak = values[k];
                    if (comparator.compare(ak, pivot1) == 0) { // Move a[k] to left part
                        values[k] = values[less];
                        values[less] = ak;
                        ++less;
                    } else if (comparator.compare(ak, pivot2) == 0) { // Move a[k] to right part
                        while (comparator.compare(values[great], pivot2) == 0) {
                            if (great-- == k) {
                                break outer;
                            }
                        }
                        if (comparator.compare(values[great], pivot1) == 0) { // a[great] < pivot2
                            values[k] = values[less];
                            /*
                             * Even though a[great] equals to pivot1, the
                             * assignment a[less] = pivot1 may be incorrect,
                             * if a[great] and pivot1 are floating-point zeros
                             * of different signs. Therefore in float and
                             * double sorting methods we have to use more
                             * accurate assignment a[less] = a[great].
                             */
                            values[less] = pivot1;
                            ++less;
                        } else { // pivot1 < a[great] < pivot2
                            values[k] = values[great];
                        }
                        values[great] = ak;
                        --great;
                    }
                }
            }

            // Sort center part recursively
            sort(values, less, great, false, comparator);

        } else { // Partitioning with one pivot
            /*
             * Use the third of the five sorted elements as pivot.
             * This value is inexpensive approximation of the median.
             */
            int pivot = values[e3];

            /*
             * Partitioning degenerates to the traditional 3-way
             * (or "Dutch National Flag") schema:
             *
             *   left part    center part              right part
             * +-------------------------------------------------+
             * |  < pivot  |   == pivot   |     ?    |  > pivot  |
             * +-------------------------------------------------+
             *              ^              ^        ^
             *              |              |        |
             *             less            k      great
             *
             * Invariants:
             *
             *   all in (left, less)   < pivot
             *   all in [less, k)     == pivot
             *   all in (great, right) > pivot
             *
             * Pointer k is the first index of ?-part.
             */
            for (int k = less; k <= great; ++k) {
                if (comparator.compare(values[k], pivot) == 0) {
                    continue;
                }
                int ak = values[k];
                if (comparator.compare(ak, pivot) < 0) { // Move a[k] to left part
                    values[k] = values[less];
                    values[less] = ak;
                    ++less;
                } else { // a[k] > pivot - Move a[k] to right part
                    while (comparator.compare(values[great], pivot) > 0) {
                        --great;
                    }
                    if (comparator.compare(values[great], pivot) < 0) { // a[great] < pivot
                        values[k] = values[less];
                        values[less] = values[great];
                        ++less;
                    } else { // a[great] == pivot
                        /*
                         * Even though a[great] equals to pivot, the
                         * assignment a[k] = pivot may be incorrect,
                         * if a[great] and pivot are floating-point
                         * zeros of different signs. Therefore in float
                         * and double sorting methods we have to use
                         * more accurate assignment a[k] = a[great].
                         */
                        values[k] = pivot;
                    }
                    values[great] = ak;
                    --great;
                }
            }

            /*
             * Sort left and right parts recursively.
             * All elements from center part are equal
             * and, therefore, already sorted.
             */
            sort(values, l, less - 1, leftmost, comparator);
            sort(values, great + 1, r, false, comparator);
        }
    }
	
	/**
	 * @author codistmonk (creation 2014-04-29)
	 */
	public static abstract interface IntComparator extends Serializable {
		
		public abstract int compare(int value1, int value2);
		
	}
	
}
