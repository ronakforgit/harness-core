/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
@Builder
public class PipelineRequestInfoDTO {
  @Getter String identifier;
  @Getter String name;
  @Getter String yaml;
  @Getter String description;
  @Getter Map<String, String> tags;
}
