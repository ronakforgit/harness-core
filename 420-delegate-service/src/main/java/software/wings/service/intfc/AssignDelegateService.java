/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.task.TaskFailureReason;

import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;

@OwnedBy(DEL)
public interface AssignDelegateService {
  boolean canAssign(String delegateId, DelegateTask task);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  boolean isDelegateGroupWhitelisted(DelegateTask task, String delegateGroupId);

  boolean shouldValidate(DelegateTask task, String delegateId);
  List<String> connectedWhitelistedDelegates(DelegateTask task);

  List<String> extractSelectors(DelegateTask task);

  void refreshWhitelist(DelegateTask task, String delegateId);

  void saveConnectionResults(List<DelegateConnectionResult> results);

  String getActiveDelegateAssignmentErrorMessage(TaskFailureReason reason, DelegateTask delegateTask);

  List<Delegate> getAccountDelegates(String accountId);

  List<String> retrieveActiveDelegates(String accountId, DelegateTask task);

  boolean noInstalledDelegates(String accountId);

  List<String> getEligibleDelegatesToExecuteTask(DelegateTask task);

  List<String> getConnectedDelegateList(List<String> delegates, DelegateTask delegateTask);

  boolean canAssignTask(String delegateId, DelegateTask task);

  List<Delegate> fetchActiveDelegates(String accountId);
}
