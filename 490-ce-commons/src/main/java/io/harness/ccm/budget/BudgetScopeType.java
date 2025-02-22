/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BudgetScopeType {
  public static final String APPLICATION = "APPLICATION";
  public static final String CLUSTER = "CLUSTER";
  public static final String PERSPECTIVE = "PERSPECTIVE";
}
