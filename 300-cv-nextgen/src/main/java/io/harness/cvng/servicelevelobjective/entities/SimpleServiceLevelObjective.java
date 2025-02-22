/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.servicelevelobjective.entities;

import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveV2DTO;
import io.harness.cvng.servicelevelobjective.beans.slospec.SimpleServiceLevelObjectiveSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("Simple")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "SimpleServiceLevelObjectiveKeys")
@EqualsAndHashCode(callSuper = true)
public class SimpleServiceLevelObjective extends AbstractServiceLevelObjective {
  public SimpleServiceLevelObjective() {
    super.setType(ServiceLevelObjectiveType.SIMPLE);
  }
  String healthSourceIdentifier;
  String monitoredServiceIdentifier;
  List<String> serviceLevelIndicators;
  ServiceLevelIndicatorType serviceLevelIndicatorType;

  @Override
  public Optional<String> mayBeGetMonitoredServiceIdentifier() {
    return Optional.ofNullable(monitoredServiceIdentifier);
  }

  public static class SimpleServiceLevelObjectiveUpdatableEntity
      extends AbstractServiceLevelObjectiveUpdatableEntity<SimpleServiceLevelObjective, ServiceLevelObjectiveV2DTO> {
    @Override
    public void setUpdateOperations(UpdateOperations<SimpleServiceLevelObjective> updateOperations,
        ServiceLevelObjectiveV2DTO serviceLevelObjectiveV2DTO) {
      SimpleServiceLevelObjectiveSpec simpleServiceLevelObjectiveSpec =
          (SimpleServiceLevelObjectiveSpec) serviceLevelObjectiveV2DTO.getSpec();
      setCommonOperations(updateOperations, serviceLevelObjectiveV2DTO);
      updateOperations
          .set(SimpleServiceLevelObjectiveKeys.healthSourceIdentifier,
              simpleServiceLevelObjectiveSpec.getHealthSourceRef())
          .set(SimpleServiceLevelObjectiveKeys.monitoredServiceIdentifier,
              simpleServiceLevelObjectiveSpec.getMonitoredServiceRef())
          .set(SimpleServiceLevelObjectiveKeys.serviceLevelIndicatorType,
              simpleServiceLevelObjectiveSpec.getServiceLevelIndicatorType());
    }
  }
}
