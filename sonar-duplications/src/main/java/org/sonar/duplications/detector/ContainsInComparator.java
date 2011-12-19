/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.detector;

import java.util.Comparator;

import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.utils.FastStringComparator;

/**
 * Allows to determine if ClonePart includes another ClonePart.
 * Inclusion is the partial order, so in fact this class violates contracts of {@link Comparator},
 * however it allows to use {@link org.sonar.duplications.utils.SortedListsUtils} for efficient filtering.
 */
public final class ContainsInComparator implements Comparator<ClonePart> {

  /**
   * Defines order by resourceId.
   */
  public static final Comparator<ClonePart> RESOURCE_ID_COMPARATOR = new Comparator<ClonePart>() {
    public int compare(ClonePart o1, ClonePart o2) {
      return FastStringComparator.INSTANCE.compare(o1.getResourceId(), o2.getResourceId());
    }
  };

  /**
   * Defines order by resourceId and by unitStart.
   */
  public static final Comparator<ClonePart> CLONEPART_COMPARATOR = new Comparator<ClonePart>() {
    public int compare(ClonePart o1, ClonePart o2) {
      int c = RESOURCE_ID_COMPARATOR.compare(o1, o2);
      if (c == 0) {
        return o1.getUnitStart() - o2.getUnitStart();
      }
      return c;
    }
  };

  private final int l1, l2;

  /**
   * Constructs new comparator for two parts with lengths {@code l1} and {@code l2} respectively.
   */
  public ContainsInComparator(int l1, int l2) {
    this.l1 = l1;
    this.l2 = l2;
  }

  /**
   * Compares two parts on inclusion.
   * part1 includes part2 if {@code (part1.resourceId == part2.resourceId) && (part1.unitStart <= part2.unitStart) && (part2.unitEnd <= part1.unitEnd)}.
   * 
   * @return 0 if part1 includes part2,
   *         1 if resourceId of part1 is greater than resourceId of part2 or if unitStart of part1 is greater than unitStart of part2,
   *         -1 in all other cases
   */
  public int compare(ClonePart part1, ClonePart part2) {
    int c = RESOURCE_ID_COMPARATOR.compare(part1, part2);
    if (c == 0) {
      if (part1.getUnitStart() <= part2.getUnitStart()) {
        if (part2.getUnitStart() + l2 <= part1.getUnitStart() + l1) {
          return 0; // part1 contains part2
        } else {
          return -1; // SortedListsUtils#contains should continue search
        }
      } else {
        return 1; // unitStart of part1 is less than unitStart of part2 - SortedListsUtils#contains should stop search
      }
    } else {
      return c;
    }
  }

}