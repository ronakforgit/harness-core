/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.entity;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.MigratedEntityMapping;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.ngmigration.beans.BaseEntityInput;
import io.harness.ngmigration.beans.BaseInputDefinition;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.MigratorInputType;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.client.NGClient;
import io.harness.ngmigration.client.PmsClient;
import io.harness.ngmigration.dto.ImportError;
import io.harness.ngmigration.dto.MigrationImportSummaryDTO;
import io.harness.ngmigration.expressions.MigratorExpressionUtils;
import io.harness.ngmigration.service.MigratorMappingService;
import io.harness.ngmigration.service.MigratorUtility;
import io.harness.ngmigration.service.NgMigrationService;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.pms.yaml.YamlUtils;
import io.harness.remote.client.NGRestUtils;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.ngmigration.CgBasicInfo;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.StateType;

import com.google.api.client.util.ArrayMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class PipelineMigrationService extends NgMigrationService {
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowMigrationService workflowMigrationService;
  @Inject private MigratorExpressionUtils migratorExpressionUtils;

  @Override
  public MigratedEntityMapping generateMappingEntity(NGYamlFile yamlFile) {
    CgBasicInfo basicInfo = yamlFile.getCgBasicInfo();
    PipelineInfoConfig pipelineInfoConfig = ((PipelineConfig) yamlFile.getYaml()).getPipelineInfoConfig();
    return MigratedEntityMapping.builder()
        .appId(basicInfo.getAppId())
        .accountId(basicInfo.getAccountId())
        .cgEntityId(basicInfo.getId())
        .entityType(NGMigrationEntityType.PIPELINE.name())
        .accountIdentifier(basicInfo.getAccountId())
        .orgIdentifier(pipelineInfoConfig.getOrgIdentifier())
        .projectIdentifier(pipelineInfoConfig.getProjectIdentifier())
        .identifier(pipelineInfoConfig.getIdentifier())
        .scope(MigratorMappingService.getScope(
            pipelineInfoConfig.getOrgIdentifier(), pipelineInfoConfig.getProjectIdentifier()))
        .fullyQualifiedIdentifier(MigratorMappingService.getFullyQualifiedIdentifier(basicInfo.getAccountId(),
            pipelineInfoConfig.getOrgIdentifier(), pipelineInfoConfig.getProjectIdentifier(),
            pipelineInfoConfig.getIdentifier()))
        .build();
  }

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    if (entity == null) {
      return null;
    }
    Pipeline pipeline = (Pipeline) entity;
    String entityId = pipeline.getUuid();
    CgEntityId pipelineEntityId = CgEntityId.builder().type(NGMigrationEntityType.PIPELINE).id(entityId).build();
    CgEntityNode pipelineNode = CgEntityNode.builder()
                                    .id(entityId)
                                    .appId(pipeline.getAppId())
                                    .type(NGMigrationEntityType.PIPELINE)
                                    .entityId(pipelineEntityId)
                                    .entity(pipeline)
                                    .build();

    Set<CgEntityId> children = new HashSet<>();
    if (isNotEmpty(pipeline.getPipelineStages())) {
      List<PipelineStage> stages = pipeline.getPipelineStages();
      stages.stream().flatMap(stage -> stage.getPipelineStageElements().stream()).forEach(stageElement -> {
        // Handle Approval State
        if (StateType.ENV_STATE.name().equals(stageElement.getType())) {
          String workflowId = (String) stageElement.getProperties().get("workflowId");
          if (isNotEmpty(workflowId)) {
            children.add(CgEntityId.builder().type(NGMigrationEntityType.WORKFLOW).id(workflowId).build());
          }
        }
      });
    }

    return DiscoveryNode.builder().children(children).entityNode(pipelineNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    Pipeline pipeline = pipelineService.getPipeline(appId, entityId);
    if (pipeline == null) {
      throw new InvalidRequestException(
          format("Pipeline with id:[%s] in application with id:[%s] doesn't exist", entityId, appId));
    }
    return discover(pipeline);
  }

  @Override
  public NGMigrationStatus canMigrate(NGMigrationEntity entity) {
    Pipeline pipeline = (Pipeline) entity;
    boolean possible = true;
    List<String> errorReasons = new ArrayList<>();
    if (isNotEmpty(pipeline.getPipelineStages())) {
      List<PipelineStageElement> stageElements = pipeline.getPipelineStages()
                                                     .stream()
                                                     .flatMap(stage -> stage.getPipelineStageElements().stream())
                                                     .collect(Collectors.toList());
      for (PipelineStageElement stageElement : stageElements) {
        if (!StateType.ENV_STATE.name().equals(stageElement.getType())) {
          possible = false;
          errorReasons.add(String.format(
              "%s stage in %s pipeline is not possible to migrate", stageElement.getName(), pipeline.getName()));
        }
      }
    }
    return NGMigrationStatus.builder().status(possible).reasons(errorReasons).build();
  }

  @Override
  public MigrationImportSummaryDTO migrate(String auth, NGClient ngClient, PmsClient pmsClient,
      MigrationInputDTO inputDTO, NGYamlFile yamlFile) throws IOException {
    if (yamlFile.isExists()) {
      log.info("Skipping creation of Pipeline entity as it already exists");
      return MigrationImportSummaryDTO.builder()
          .errors(Collections.singletonList(ImportError.builder()
                                                .message("Pipeline was not migrated as it was already imported before")
                                                .entity(yamlFile.getCgBasicInfo())
                                                .build()))
          .build();
    }
    try {
      NGRestUtils.getResponse(pmsClient.createPipeline(auth, inputDTO.getAccountIdentifier(),
          inputDTO.getOrgIdentifier(), inputDTO.getProjectIdentifier(),
          RequestBody.create(MediaType.parse("application/yaml"), YamlUtils.write(yamlFile.getYaml()))));
      log.info("Pipeline creation successful");
      return MigrationImportSummaryDTO.builder().success(true).errors(Collections.emptyList()).build();
    } catch (Exception ex) {
      log.error("Pipeline creation failed - ", ex);
      return MigrationImportSummaryDTO.builder()
          .errors(Collections.singletonList(ImportError.builder()
                                                .message("There was an error creating the pipeline")
                                                .entity(yamlFile.getCgBasicInfo())
                                                .build()))
          .build();
    }
  }

  @Override
  public List<NGYamlFile> generateYaml(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NGYamlFile> migratedEntities) {
    Pipeline pipeline = (Pipeline) entities.get(entityId).getEntity();
    migratorExpressionUtils.render(pipeline, inputDTO.getCustomExpressions());
    String name = MigratorUtility.generateName(inputDTO.getOverrides(), entityId, pipeline.getName());
    String identifier = MigratorUtility.generateIdentifierDefaultName(inputDTO.getOverrides(), entityId, name);
    String projectIdentifier = MigratorUtility.getProjectIdentifier(Scope.PROJECT, inputDTO);
    String orgIdentifier = MigratorUtility.getOrgIdentifier(Scope.PROJECT, inputDTO);

    List<StageElementWrapperConfig> ngStages = new ArrayList<>();
    if (isNotEmpty(pipeline.getPipelineStages())) {
      pipeline.getPipelineStages()
          .stream()
          .flatMap(stage -> stage.getPipelineStageElements().stream())
          .forEach(stageElement -> {
            if (StateType.ENV_STATE.name().equals(stageElement.getType())) {
              String workflowId = (String) stageElement.getProperties().get("workflowId");
              if (isNotEmpty(workflowId)) {
                // Every CG pipeline stage to NG convert
                ngStages.add(workflowMigrationService.getNgStage(inputDTO, entities, graph,
                    CgEntityId.builder().type(NGMigrationEntityType.WORKFLOW).id(workflowId).build(),
                    migratedEntities));
              }
            }
            // TODO: Handle Approval State
          });
    }

    List<NGYamlFile> allFiles = new ArrayList<>();

    // TODO: @puthraya Fix tags

    PipelineConfig pipelineConfig = PipelineConfig.builder()
                                        .pipelineInfoConfig(PipelineInfoConfig.builder()
                                                                .name(name)
                                                                .identifier(identifier)
                                                                .projectIdentifier(projectIdentifier)
                                                                .orgIdentifier(orgIdentifier)
                                                                .stages(ngStages)
                                                                .tags(new ArrayMap<>())
                                                                .build())
                                        .build();

    NGYamlFile yamlFile = NGYamlFile.builder()
                              .type(NGMigrationEntityType.PIPELINE)
                              .filename("pipelines/" + identifier + ".yaml")
                              .yaml(pipelineConfig)
                              .ngEntityDetail(NgEntityDetail.builder()
                                                  .identifier(identifier)
                                                  .orgIdentifier(orgIdentifier)
                                                  .projectIdentifier(projectIdentifier)
                                                  .build())
                              .cgBasicInfo(CgBasicInfo.builder()
                                               .id(pipeline.getUuid())
                                               .accountId(pipeline.getAccountId())
                                               .appId(pipeline.getAppId())
                                               .name(pipeline.getName())
                                               .type(NGMigrationEntityType.PIPELINE)
                                               .build())
                              .build();
    allFiles.add(yamlFile);

    migratedEntities.putIfAbsent(entityId, yamlFile);

    return allFiles;
  }

  @Override
  protected YamlDTO getNGEntity(NgEntityDetail ngEntityDetail, String accountIdentifier) {
    // TODO: get pipeline
    return null;
  }

  @Override
  protected boolean isNGEntityExists() {
    return true;
  }

  @Override
  public BaseEntityInput generateInput(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    Pipeline pipeline = (Pipeline) entities.get(entityId).getEntity();
    return BaseEntityInput.builder()
        .migrationStatus(MigratorInputType.CREATE_NEW)
        .identifier(BaseInputDefinition.buildIdentifier(MigratorUtility.generateIdentifier(pipeline.getName())))
        .name(BaseInputDefinition.buildName(pipeline.getName()))
        .spec(null)
        .build();
  }
}
