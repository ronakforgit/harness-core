/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.matchers;

import org.apache.commons.lang3.StringUtils;

public class NotNullMatcher implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    Boolean expectedVal = (Boolean) expected;
    Boolean actualVal = StringUtils.isNotBlank((String) actual);
    return expectedVal == actualVal;
  }
}
