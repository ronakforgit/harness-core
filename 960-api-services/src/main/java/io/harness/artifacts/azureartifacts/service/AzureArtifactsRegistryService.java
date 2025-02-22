/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.azureartifacts.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.azureartifacts.beans.AzureArtifactsInternalConfig;

import software.wings.helpers.ext.azure.devops.AzureArtifactsFeed;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackage;
import software.wings.helpers.ext.azure.devops.AzureDevopsProject;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface AzureArtifactsRegistryService {
  boolean validateCredentials(AzureArtifactsInternalConfig toAzureArtifactsInternalConfig);

  List<BuildDetails> listPackageVersions(AzureArtifactsInternalConfig azureArtifactsInternalConfig, String packageType,
      String packageName, String versionRegex, String feed, String project);

  BuildDetails getBuild(AzureArtifactsInternalConfig azureArtifactsInternalConfig, String packageType, String packageId,
      String version, String feed, String project);

  BuildDetails getLastSuccessfulBuildFromRegex(AzureArtifactsInternalConfig azureArtifactsConfig, String packageType,
      String packageName, String versionRegex, String feed, String project, String scope);

  List<AzureDevopsProject> listProjects(AzureArtifactsInternalConfig azureArtifactsInternalConfig);

  List<AzureArtifactsPackage> listPackages(
      AzureArtifactsInternalConfig azureArtifactsInternalConfig, String project, String feed, String packageType);

  List<AzureArtifactsFeed> listFeeds(AzureArtifactsInternalConfig azureArtifactsInternalConfig, String project);
}
