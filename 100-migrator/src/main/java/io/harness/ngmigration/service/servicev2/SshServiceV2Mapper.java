/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.servicev2;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.service.beans.SshServiceSpec;
import io.harness.cdng.service.beans.SshServiceSpec.SshServiceSpecBuilder;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.service.MigratorUtility;

import software.wings.beans.Service;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class SshServiceV2Mapper implements ServiceV2Mapper {
  @Override
  public ServiceDefinition getServiceDefinition(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, Service service, Map<CgEntityId, NGYamlFile> migratedEntities,
      List<ManifestConfigWrapper> manifests) {
    PrimaryArtifact primaryArtifact = getPrimaryArtifactStream(inputDTO, entities, graph, service, migratedEntities);
    SshServiceSpecBuilder sshServiceSpecBuilder = SshServiceSpec.builder();
    if (primaryArtifact != null) {
      sshServiceSpecBuilder.artifacts(ArtifactListConfig.builder().primary(primaryArtifact).build());
    }
    sshServiceSpecBuilder.variables(MigratorUtility.getVariables(service.getServiceVariables(), migratedEntities));
    return ServiceDefinition.builder()
        .type(ServiceDefinitionType.SSH)
        .serviceSpec(sshServiceSpecBuilder.build())
        .build();
  }
}
