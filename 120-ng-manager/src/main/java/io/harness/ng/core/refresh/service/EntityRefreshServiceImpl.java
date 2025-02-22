/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.refresh.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.refresh.helper.InputsValidationHelper;
import io.harness.ng.core.refresh.helper.RefreshInputsHelper;
import io.harness.ng.core.template.refresh.v2.InputsValidationResponse;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDC)
public class EntityRefreshServiceImpl implements EntityRefreshService {
  @Inject InputsValidationHelper inputsValidationHelper;
  @Inject RefreshInputsHelper refreshInputsHelper;

  @Override
  public InputsValidationResponse validateInputsForYaml(
      String accountId, String orgId, String projectId, String yaml, String resolvedTemplatesYaml) {
    return inputsValidationHelper.validateInputsForYaml(accountId, orgId, projectId, yaml, resolvedTemplatesYaml);
  }

  @Override
  public String refreshLinkedInputs(
      String accountId, String orgId, String projectId, String yaml, String resolvedTemplatesYaml) {
    return refreshInputsHelper.refreshInputs(accountId, orgId, projectId, yaml, resolvedTemplatesYaml);
  }
}
