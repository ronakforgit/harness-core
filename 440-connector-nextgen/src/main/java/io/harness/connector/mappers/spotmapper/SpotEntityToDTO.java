/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.spotmapper;

import static io.harness.delegate.beans.connector.spotconnector.SpotCredentialType.MANUAL_CREDENTIALS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.spotconnector.SpotConfig;
import io.harness.connector.entities.embedded.spotconnector.SpotManualCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.spotconnector.SpotConnectorDTO;
import io.harness.delegate.beans.connector.spotconnector.SpotCredentialDTO;
import io.harness.delegate.beans.connector.spotconnector.SpotCredentialDTO.SpotCredentialDTOBuilder;
import io.harness.delegate.beans.connector.spotconnector.SpotCredentialType;
import io.harness.delegate.beans.connector.spotconnector.SpotManualConfigSpecDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.DX)
@Singleton
public class SpotEntityToDTO implements ConnectorEntityToDTOMapper<SpotConnectorDTO, SpotConfig> {
  @Override
  public SpotConnectorDTO createConnectorDTO(SpotConfig connector) {
    final SpotCredentialType credentialType = connector.getCredentialType();
    SpotCredentialDTOBuilder spotCredentialDTOBuilder;
    if (credentialType == MANUAL_CREDENTIALS) {
      spotCredentialDTOBuilder = buildManualCredential((SpotManualCredential) connector.getCredential());
    } else {
      throw new InvalidRequestException("Invalid Credential type.");
    }
    return SpotConnectorDTO.builder()
        .credential(spotCredentialDTOBuilder.build())
        .delegateSelectors(connector.getDelegateSelectors())
        .build();
  }

  private SpotCredentialDTOBuilder buildManualCredential(SpotManualCredential credential) {
    final SecretRefData apiTokenRef = SecretRefHelper.createSecretRef(credential.getApiTokenRef());
    final SecretRefData accountIdRef = SecretRefHelper.createSecretRef(credential.getAccountIdRef());
    final SpotManualConfigSpecDTO spotManualConfigSpecDTO = SpotManualConfigSpecDTO.builder()
                                                                .accountId(credential.getAccountId())
                                                                .accountIdRef(accountIdRef)
                                                                .apiTokenRef(apiTokenRef)
                                                                .build();
    return SpotCredentialDTO.builder().spotCredentialType(MANUAL_CREDENTIALS).config(spotManualConfigSpecDTO);
  }
}
