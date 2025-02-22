/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.apis.resource;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.helper.ConnectorRbacHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ConnectorNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.spec.server.ng.model.Connector;
import io.harness.spec.server.ng.model.ConnectorRequest;
import io.harness.spec.server.ng.model.ConnectorResponse;
import io.harness.spec.server.ng.model.ConnectorSpec;
import io.harness.spec.server.ng.model.ConnectorTestConnectionResponse;
import io.harness.spec.server.ng.model.GitHttpConnectorSpec;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@OwnedBy(HarnessTeam.PL)
public class ProjectConnectorApiImplTest extends CategoryTest {
  @Mock private ConnectorService connectorService;
  @Mock private AccessControlClient accessControlClient;
  @Mock private ConnectorRbacHelper connectorRbacHelper;
  private ProjectConnectorApiImpl projectConnectorApi;
  ConnectorResponseDTO connectorResponseDTO;
  ConnectorInfoDTO connectorInfo;
  String account = "account";
  String slug = "example_connector";
  String name = "example_connector";
  String org = "default";
  String project = "example_project";
  String username = "git-username";
  String passwordRef = "account.git-password";
  String url = "http://github.com/";
  String delegateId = "delegate-id";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    connectorInfo = ConnectorInfoDTO.builder()
                        .name(name)
                        .identifier(slug)
                        .orgIdentifier(org)
                        .projectIdentifier(project)
                        .connectorType(ConnectorType.GIT)
                        .connectorConfig(GitConfigDTO.builder()
                                             .url("http://github.com/")
                                             .gitAuthType(GitAuthType.HTTP)
                                             .gitConnectionType(GitConnectionType.REPO)
                                             .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                          .username(username)
                                                          .passwordRef(new SecretRefData(passwordRef))
                                                          .build())
                                             .build())
                        .build();
    connectorResponseDTO = ConnectorResponseDTO.builder().connector(connectorInfo).build();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    projectConnectorApi = new ProjectConnectorApiImpl(
        accessControlClient, connectorService, new ConnectorApiUtils(factory.getValidator()), connectorRbacHelper);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void createProjectScopedConnector() {
    doReturn(connectorResponseDTO).when(connectorService).create(any(), any());
    ConnectorRequest connectorRequest = getConnectorRequest(org, project);

    Response response = projectConnectorApi.createProjectScopedConnector(connectorRequest, org, project, account);

    assertThat(response.getStatus()).isEqualTo(201);

    Mockito.verify(connectorService, times(1)).create(any(), any());

    ConnectorResponse connectorResponse = (ConnectorResponse) response.getEntity();

    assertThat(connectorResponse).isNotNull();
    assertThat(connectorResponse.getConnector()).isNotNull();
    assertThat(connectorResponse.getConnector().getName()).isEqualTo(name);
    assertThat(connectorResponse.getConnector().getSlug()).isEqualTo(slug);
    assertThat(connectorResponse.getConnector().getOrg()).isEqualTo(org);
    assertThat(connectorResponse.getConnector().getProject()).isEqualTo(project);
    assertThat(connectorResponse.getConnector().getSpec()).isNotNull();
    GitHttpConnectorSpec gitHttpConnectorSpec = (GitHttpConnectorSpec) connectorResponse.getConnector().getSpec();
    assertThat(gitHttpConnectorSpec.getType()).isEqualTo(ConnectorSpec.TypeEnum.GITHTTP);
    assertThat(gitHttpConnectorSpec.getUsername()).isEqualTo(username);
    assertThat(gitHttpConnectorSpec.getPasswordRef()).isEqualTo(passwordRef);
    assertThat(gitHttpConnectorSpec.getUrl()).isEqualTo(url);
    assertThat(gitHttpConnectorSpec.getConnectionType()).isEqualTo(GitHttpConnectorSpec.ConnectionTypeEnum.REPO);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testCreateProjectScopedConnectorInvalidExceptionNullProjectInRequest() {
    Throwable thrown = catchThrowableOfType(
        ()
            -> projectConnectorApi.createProjectScopedConnector(getConnectorRequest(org, null), org, project, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_PROJECT_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testCreateProjectScopedConnectorInvalidExceptionNullOrgInRequest() {
    Throwable thrown = catchThrowableOfType(()
                                                -> projectConnectorApi.createProjectScopedConnector(
                                                    getConnectorRequest(null, project), org, project, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_ORG_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testCreateProjectScopedConnectorInvalidException() {
    Throwable thrown = catchThrowableOfType(()
                                                -> projectConnectorApi.createProjectScopedConnector(
                                                    getConnectorRequest(org, project), "another_org", project, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_ORG_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testCreateProjectScopedConnectorInvalidExceptionForHarnessSecretManagerIdentifier() {
    ConnectorRequest connectorRequest = getConnectorRequest(org, project);
    connectorRequest.getConnector().setSlug(HARNESS_SECRET_MANAGER_IDENTIFIER);
    Throwable thrown = catchThrowableOfType(
        ()
            -> projectConnectorApi.createProjectScopedConnector(connectorRequest, org, project, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage("harnessSecretManager cannot be used as connector identifier");
  }

  @Test(expected = NotFoundException.class)
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetProjectScopedConnectorNotFoundException() {
    projectConnectorApi.getProjectScopedConnector(org, project, slug, account);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetProjectScopedConnector() {
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorResponseDTO));
    Response response = projectConnectorApi.getProjectScopedConnector(org, project, slug, account);

    assertThat(response.getStatus()).isEqualTo(200);

    ConnectorResponse connectorResponse = (ConnectorResponse) response.getEntity();
    assertThat(connectorResponse.getConnector().getSlug()).isEqualTo(slug);
    assertThat(connectorResponse.getConnector().getName()).isEqualTo(name);
    assertThat(connectorResponse.getConnector().getOrg()).isEqualTo(org);
    assertThat(connectorResponse.getConnector().getProject()).isEqualTo(project);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testProjectScopedConnectorTestConnection() {
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorResponseDTO));
    when(connectorRbacHelper.checkSecretRuntimeAccessWithConnectorDTO(any(), any())).thenReturn(true);
    when(connectorService.testConnection(account, org, project, slug))
        .thenReturn(
            ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).delegateId(delegateId).build());

    Response response = projectConnectorApi.testProjectScopedConnector(org, project, slug, account);

    assertThat(response.getStatus()).isEqualTo(200);

    ConnectorTestConnectionResponse connectorTestConnectionResponse =
        (ConnectorTestConnectionResponse) response.getEntity();
    assertThat(connectorTestConnectionResponse.getStatus())
        .isEqualTo(ConnectorTestConnectionResponse.StatusEnum.SUCCESS);
    assertThat(connectorTestConnectionResponse.getDelegateId()).isEqualTo(delegateId);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testProjectScopedConnectorTestConnectionNotFoundException() {
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.empty());

    Throwable thrown =
        catchThrowableOfType(()
                                 -> projectConnectorApi.testProjectScopedConnector(org, project, slug, account),
            ConnectorNotFoundException.class);

    assertThat(thrown).hasMessage(String.format("No connector found with identifier %s", slug));
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetProjectScopedConnectors() {
    Page<ConnectorResponseDTO> pages = new PageImpl<>(Collections.singletonList(connectorResponseDTO));
    when(connectorService.list(eq(0), eq(10), eq(account), any(), eq(org), any(), any(), any(), eq(false), any()))
        .thenReturn(pages);

    Response response = projectConnectorApi.getProjectScopedConnectors(org, project, false, null, 0, 10, account);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getLinks()).isNotNull();
    assertThat(response.getLinks().size()).isEqualTo(1);

    List<ConnectorResponse> connectorResponses = (List<ConnectorResponse>) response.getEntity();
    ConnectorResponse connectorResponse = connectorResponses.get(0);
    assertThat(connectorResponse.getConnector().getSlug()).isEqualTo(slug);
    assertThat(connectorResponse.getConnector().getName()).isEqualTo(name);
    assertThat(connectorResponse.getConnector().getOrg()).isEqualTo(org);
    assertThat(connectorResponse.getConnector().getProject()).isEqualTo(project);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetProjectScopedConnectorsEmpty() {
    Page<ConnectorResponseDTO> pages = Page.empty();
    when(connectorService.list(eq(0), eq(10), eq(account), any(), any(), any(), any(), any(), eq(false), any()))
        .thenReturn(pages);

    Response response = projectConnectorApi.getProjectScopedConnectors(org, project, false, null, 0, 10, account);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getLinks()).isNotNull();
    assertThat(response.getLinks().size()).isEqualTo(1);

    List<ConnectorResponse> connectorResponses = (List<ConnectorResponse>) response.getEntity();
    assertThat(connectorResponses.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateProjectScopedConnector() {
    doReturn(connectorResponseDTO).when(connectorService).update(any(), any());
    ConnectorRequest connectorRequest = getConnectorRequest(org, project);

    Response response = projectConnectorApi.updateProjectScopedConnector(connectorRequest, org, project, slug, account);

    assertThat(response.getStatus()).isEqualTo(200);

    Mockito.verify(connectorService, times(1)).update(any(), any());

    ConnectorResponse connectorResponse = (ConnectorResponse) response.getEntity();

    assertThat(connectorResponse).isNotNull();
    assertThat(connectorResponse.getConnector()).isNotNull();
    assertThat(connectorResponse.getConnector().getName()).isEqualTo(name);
    assertThat(connectorResponse.getConnector().getSlug()).isEqualTo(slug);
    assertThat(connectorResponse.getConnector().getOrg()).isEqualTo(org);
    assertThat(connectorResponse.getConnector().getProject()).isEqualTo(project);
    assertThat(connectorResponse.getConnector().getSpec()).isNotNull();
    GitHttpConnectorSpec gitHttpConnectorSpec = (GitHttpConnectorSpec) connectorResponse.getConnector().getSpec();
    assertThat(gitHttpConnectorSpec.getType()).isEqualTo(ConnectorSpec.TypeEnum.GITHTTP);
    assertThat(gitHttpConnectorSpec.getUsername()).isEqualTo(username);
    assertThat(gitHttpConnectorSpec.getPasswordRef()).isEqualTo(passwordRef);
    assertThat(gitHttpConnectorSpec.getUrl()).isEqualTo(url);
    assertThat(gitHttpConnectorSpec.getConnectionType()).isEqualTo(GitHttpConnectorSpec.ConnectionTypeEnum.REPO);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateProjectScopedConnectorInvalidExceptionForNullProjectInRequest() {
    Throwable thrown = catchThrowableOfType(()
                                                -> projectConnectorApi.updateProjectScopedConnector(
                                                    getConnectorRequest(org, null), org, project, slug, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_PROJECT_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateProjectScopedConnectorInvalidExceptionForNullOrgInRequest() {
    Throwable thrown = catchThrowableOfType(()
                                                -> projectConnectorApi.updateProjectScopedConnector(
                                                    getConnectorRequest(null, project), org, project, slug, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_ORG_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateProjectScopedConnectorInvalidException() {
    Throwable thrown =
        catchThrowableOfType(()
                                 -> projectConnectorApi.updateProjectScopedConnector(
                                     getConnectorRequest(org, project), "another_org", project, slug, account),
            InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_ORG_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateProjectScopedConnectorForDifferentSlugInPathAndPayload() {
    Throwable thrown =
        catchThrowableOfType(()
                                 -> projectConnectorApi.updateProjectScopedConnector(
                                     getConnectorRequest(org, project), org, project, "another_slug", account),
            InvalidRequestException.class);

    assertThat(thrown).hasMessage(NGCommonEntityConstants.DIFFERENT_SLUG_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateProjectScopedConnectorInvalidExceptionForHarnessSecretManagerIdentifier() {
    ConnectorRequest connectorRequest = getConnectorRequest(org, project);
    connectorRequest.getConnector().setSlug(HARNESS_SECRET_MANAGER_IDENTIFIER);
    Throwable thrown = catchThrowableOfType(()
                                                -> projectConnectorApi.updateProjectScopedConnector(connectorRequest,
                                                    org, project, HARNESS_SECRET_MANAGER_IDENTIFIER, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage("Update operation not supported for Harness Secret Manager");
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testDeleteProjectScopedConnector() {
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());
    doReturn(true).when(connectorService).delete(any(), any(), any(), any());

    Response response = projectConnectorApi.deleteProjectScopedConnector(org, project, slug, account);

    verify(connectorService, times(1)).delete(eq(account), eq(org), eq(project), eq(slug));

    assertThat(response.getStatus()).isEqualTo(200);

    ConnectorResponse connectorResponse = (ConnectorResponse) response.getEntity();

    assertThat(connectorResponse).isNotNull();
    assertThat(connectorResponse.getConnector()).isNotNull();
    assertThat(connectorResponse.getConnector().getName()).isEqualTo(name);
    assertThat(connectorResponse.getConnector().getSlug()).isEqualTo(slug);
    assertThat(connectorResponse.getConnector().getOrg()).isEqualTo(org);
    assertThat(connectorResponse.getConnector().getProject()).isEqualTo(project);
    assertThat(connectorResponse.getConnector().getSpec()).isNotNull();
    GitHttpConnectorSpec gitHttpConnectorSpec = (GitHttpConnectorSpec) connectorResponse.getConnector().getSpec();
    assertThat(gitHttpConnectorSpec.getType()).isEqualTo(ConnectorSpec.TypeEnum.GITHTTP);
    assertThat(gitHttpConnectorSpec.getUsername()).isEqualTo(username);
    assertThat(gitHttpConnectorSpec.getPasswordRef()).isEqualTo(passwordRef);
    assertThat(gitHttpConnectorSpec.getUrl()).isEqualTo(url);
    assertThat(gitHttpConnectorSpec.getConnectionType()).isEqualTo(GitHttpConnectorSpec.ConnectionTypeEnum.REPO);
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testDeleteProjectScopedConnectorInvalidExceptionForHarnessSecretManagerIdentifier() {
    Throwable thrown = catchThrowableOfType(()
                                                -> projectConnectorApi.deleteProjectScopedConnector(
                                                    org, project, HARNESS_SECRET_MANAGER_IDENTIFIER, account),
        InvalidRequestException.class);

    assertThat(thrown).hasMessage("Delete operation not supported for Harness Secret Manager");
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testDeleteProjectScopedConnectorNotFoundException() {
    Throwable thrown = catchThrowableOfType(
        () -> projectConnectorApi.deleteProjectScopedConnector(org, project, slug, account), NotFoundException.class);

    assertThat(thrown).hasMessage(String.format("Connector with identifier [%s] not found", slug));
  }

  @Test
  @Owner(developers = OwnerRule.ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testDeleteProjectScopedConnectorNotDeleted() {
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorResponseDTO));
    when(connectorService.delete(any(), any(), any(), any())).thenReturn(false);

    Throwable thrown =
        catchThrowableOfType(()
                                 -> projectConnectorApi.deleteProjectScopedConnector(org, project, slug, account),
            InvalidRequestException.class);

    assertThat(thrown).hasMessage(String.format("Connector with slug [%s] could not be deleted", slug));
  }

  private ConnectorRequest getConnectorRequest(String orgSlug, String projectSlug) {
    ConnectorRequest connectorRequest = new ConnectorRequest();

    Connector connector = new Connector();
    connector.setSlug(slug);
    connector.setName(name);
    connector.setOrg(orgSlug);
    connector.setProject(projectSlug);
    GitHttpConnectorSpec spec = new GitHttpConnectorSpec();
    spec.setType(ConnectorSpec.TypeEnum.GITHTTP);
    spec.setConnectionType(GitHttpConnectorSpec.ConnectionTypeEnum.REPO);
    spec.setUrl(url);
    spec.setUsername(username);
    spec.setPasswordRef(passwordRef);

    connector.setSpec(spec);

    connectorRequest.setConnector(connector);

    return connectorRequest;
  }
}
