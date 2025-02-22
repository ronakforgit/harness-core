/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.entities;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.notification.NotificationChannelType.SLACK;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.Expression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SLACK")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public class SlackConfig extends NotificationSettingConfig {
  @Expression(ALLOW_SECRETS) String slackWebhookUrl;

  @Builder
  public SlackConfig(String slackWebhookUrl) {
    this.slackWebhookUrl = slackWebhookUrl;
    this.type = SLACK;
  }
}
