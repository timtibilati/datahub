package io.datahubproject.openapi.schema.registry;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.metadata.registry.SchemaRegistryService;
import com.linkedin.mxe.TopicConvention;
import io.datahubproject.schema_registry.openapi.generated.Schema;
import java.util.List;
import java.util.Optional;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SchemaRegistryControllerTest {

  @Mock private SchemaRegistryService mockSchemaRegistryService;

  @Mock private TopicConvention mockTopicConvention;

  private SchemaRegistryController schemaRegistryController;
  private MockHttpServletRequest mockRequest;

  private static final String MCP_TOPIC = "MetadataChangeProposal";
  private static final String MCP_SUBJECT = "MetadataChangeProposal-value";
  private static final String FMCP_TOPIC = "FailedMetadataChangeProposal";
  private static final String FMCP_SUBJECT = "FailedMetadataChangeProposal-value";
  private static final String MCL_TOPIC = "MetadataChangeLog";
  private static final String MCL_SUBJECT = "MetadataChangeLog-value";
  private static final String PE_TOPIC = "PlatformEvent";
  private static final String PE_SUBJECT = "PlatformEvent-value";

  @BeforeMethod
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    mockRequest = new MockHttpServletRequest();
    schemaRegistryController =
        new SchemaRegistryController(new ObjectMapper(), mockRequest, mockSchemaRegistryService);
  }

  @Test
  public void testGetSchemaByVersion_Latest() {
    // Mock the service to return version 2 as latest
    when(mockSchemaRegistryService.getLatestSchemaVersionForTopic(MCP_TOPIC))
        .thenReturn(Optional.of(2));
    when(mockSchemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 2))
        .thenReturn(Optional.of(createMockAvroSchema()));
    when(mockSchemaRegistryService.getSchemaIdForTopic(MCP_TOPIC)).thenReturn(Optional.of(1));

    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "latest", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertEquals(response.getBody().getVersion(), 2);
    Assert.assertEquals(response.getBody().getSubject(), MCP_SUBJECT);
  }

  @Test
  public void testGetSchemaByVersion_SpecificVersion() {
    // Mock the service to return version 1
    when(mockSchemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1))
        .thenReturn(Optional.of(createMockAvroSchema()));
    when(mockSchemaRegistryService.getSchemaIdForTopic(MCP_TOPIC)).thenReturn(Optional.of(1));

    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "1", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertEquals(response.getBody().getVersion(), 1);
    Assert.assertEquals(response.getBody().getSubject(), MCP_SUBJECT);
  }

  @Test
  public void testGetSchemaByVersion_InvalidVersion() {
    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "3", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
  }

  @Test
  public void testGetSchemaByVersion_InvalidVersionFormat() {
    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "invalid", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testGetSchemaByVersion_TopicNotFound() {
    when(mockSchemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1))
        .thenReturn(Optional.empty());

    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "1", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
  }

  @Test
  public void testListVersions() {
    List<Integer> supportedVersions = List.of(1, 2);
    when(mockSchemaRegistryService.getSupportedSchemaVersionsForTopic(MCP_TOPIC))
        .thenReturn(Optional.of(supportedVersions));

    ResponseEntity<List<Integer>> response =
        schemaRegistryController.listVersions(MCP_SUBJECT, false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertEquals(response.getBody(), supportedVersions);
  }

  @Test
  public void testListVersions_TopicNotFound() {
    when(mockSchemaRegistryService.getSupportedSchemaVersionsForTopic(MCP_TOPIC))
        .thenReturn(Optional.empty());

    ResponseEntity<List<Integer>> response =
        schemaRegistryController.listVersions(MCP_SUBJECT, false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
  }

  @Test
  public void testList_AllSubjects() {
    // Mock the service to return a list of topics
    List<String> mockTopics =
        List.of(
            "MetadataChangeProposal",
            "FailedMetadataChangeProposal",
            "MetadataChangeLog",
            "PlatformEvent");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 4);

    // Verify subjects have "-value" suffix
    Assert.assertTrue(subjects.contains("MetadataChangeProposal-value"));
    Assert.assertTrue(subjects.contains("FailedMetadataChangeProposal-value"));
    Assert.assertTrue(subjects.contains("MetadataChangeLog-value"));
    Assert.assertTrue(subjects.contains("PlatformEvent-value"));
  }

  @Test
  public void testList_WithSubjectPrefix() {
    // Mock the service to return a list of topics
    List<String> mockTopics =
        List.of(
            "MetadataChangeProposal",
            "FailedMetadataChangeProposal",
            "MetadataChangeLog",
            "PlatformEvent");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response = schemaRegistryController.list("Metadata", false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 2);

    // Should only contain subjects starting with "Metadata"
    Assert.assertTrue(subjects.contains("MetadataChangeProposal-value"));
    Assert.assertTrue(subjects.contains("MetadataChangeLog-value"));
    Assert.assertFalse(subjects.contains("FailedMetadataChangeProposal-value"));
    Assert.assertFalse(subjects.contains("PlatformEvent-value"));
  }

  @Test
  public void testList_WithExactSubjectPrefix() {
    // Mock the service to return a list of topics
    List<String> mockTopics =
        List.of(
            "MetadataChangeProposal",
            "FailedMetadataChangeProposal",
            "MetadataChangeLog",
            "PlatformEvent");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response =
        schemaRegistryController.list("MetadataChange", false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 2);

    // Should only contain subjects starting with "MetadataChange"
    Assert.assertTrue(subjects.contains("MetadataChangeProposal-value"));
    Assert.assertTrue(subjects.contains("MetadataChangeLog-value"));
    Assert.assertFalse(subjects.contains("FailedMetadataChangeProposal-value"));
    Assert.assertFalse(subjects.contains("PlatformEvent-value"));
  }

  @Test
  public void testList_WithNoMatchingPrefix() {
    // Mock the service to return a list of topics
    List<String> mockTopics =
        List.of(
            "MetadataChangeProposal",
            "FailedMetadataChangeProposal",
            "MetadataChangeLog",
            "PlatformEvent");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response =
        schemaRegistryController.list("NonExistent", false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 0);
  }

  @Test
  public void testList_DeletedTrue() {
    // Mock the service to return a list of topics
    List<String> mockTopics = List.of("MetadataChangeProposal", "FailedMetadataChangeProposal");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, true, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 0);
  }

  @Test
  public void testList_DeletedOnlyTrue() {
    // Mock the service to return a list of topics
    List<String> mockTopics = List.of("MetadataChangeProposal", "FailedMetadataChangeProposal");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, false, true);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 0);
  }

  @Test
  public void testList_DeletedTrueAndDeletedOnlyTrue() {
    // Mock the service to return a list of topics
    List<String> mockTopics = List.of("MetadataChangeProposal", "FailedMetadataChangeProposal");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, true, true);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 0);
  }

  @Test
  public void testList_EmptyTopicsList() {
    // Mock the service to return an empty list
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(List.of());

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 0);
  }

  @Test
  public void testList_WithSpecialCharactersInTopics() {
    // Mock the service to return topics with special characters
    List<String> mockTopics =
        List.of(
            "Metadata-Change.Proposal", "Failed_Metadata_Change_Proposal", "MetadataChangeLog123");
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(mockTopics);

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 3);

    // Verify subjects have "-value" suffix
    Assert.assertTrue(subjects.contains("Metadata-Change.Proposal-value"));
    Assert.assertTrue(subjects.contains("Failed_Metadata_Change_Proposal-value"));
    Assert.assertTrue(subjects.contains("MetadataChangeLog123-value"));
  }

  @Test
  public void testList_ServiceReturnsNull() {
    // Mock the service to return null (edge case)
    when(mockSchemaRegistryService.getAllTopics()).thenReturn(null);

    ResponseEntity<List<String>> response = schemaRegistryController.list(null, false, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertNotNull(response.getBody());

    List<String> subjects = response.getBody();
    Assert.assertEquals(subjects.size(), 0);
  }

  @Test
  public void testGetSchemaByVersion_FailedMCPTopic() {
    // Test that Failed MCP topic also works with versioning
    when(mockSchemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 1))
        .thenReturn(Optional.of(createMockAvroSchema()));
    when(mockSchemaRegistryService.getSchemaIdForTopic(FMCP_TOPIC)).thenReturn(Optional.of(2));

    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(FMCP_SUBJECT, "1", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    Assert.assertEquals(response.getBody().getVersion(), 1);
    Assert.assertEquals(response.getBody().getSubject(), FMCP_SUBJECT);
  }

  @Test
  public void testGetSchemaByVersion_UnsupportedVersion() {
    // Test with a version that's not in the supported set
    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "99", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
  }

  @Test
  public void testGetSchemaByVersion_EmptyVersion() {
    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, "", false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testGetSchemaByVersion_NullVersion() {
    ResponseEntity<Schema> response =
        schemaRegistryController.getSchemaByVersion(MCP_SUBJECT, null, false);

    Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
  }

  private org.apache.avro.Schema createMockAvroSchema() {
    // Create a simple mock Avro schema for testing
    return org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING);
  }
}
