/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNEXPECTED;

import io.harness.eraro.Level;

public class UnexpectedException extends WingsException {
  public UnexpectedException() {
    super("This should not be happening", null, UNEXPECTED, Level.ERROR, null, null);
  }

  public UnexpectedException(String message) {
    super(message, null, UNEXPECTED, Level.ERROR, null, null);
  }

  public UnexpectedException(String message, Throwable throwable) {
    super(message, throwable, UNEXPECTED, Level.ERROR, null, null);
  }
}
