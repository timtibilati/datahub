package com.linkedin.metadata.registry;

import static org.mockito.Mockito.when;

import com.linkedin.metadata.EventSchemaConstants;
import com.linkedin.metadata.EventUtils;
import com.linkedin.mxe.TopicConvention;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SchemaRegistryServiceTest {

  @Mock private TopicConvention mockTopicConvention;

  private SchemaRegistryService schemaRegistryService;

  private static final String MCP_TOPIC = "MetadataChangeProposal";
  private static final String FMCP_TOPIC = "FailedMetadataChangeProposal";
  private static final String MCL_TOPIC = "MetadataChangeLog";
  private static final String PE_TOPIC = "PlatformEvent";

  @BeforeMethod
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    // Mock topic convention methods
    when(mockTopicConvention.getMetadataChangeProposalTopicName()).thenReturn(MCP_TOPIC);
    when(mockTopicConvention.getFailedMetadataChangeProposalTopicName()).thenReturn(FMCP_TOPIC);
    when(mockTopicConvention.getMetadataChangeLogVersionedTopicName()).thenReturn(MCL_TOPIC);
    when(mockTopicConvention.getPlatformEventTopicName()).thenReturn(PE_TOPIC);
    when(mockTopicConvention.getMetadataChangeLogTimeseriesTopicName())
        .thenReturn("MetadataChangeLogTimeseries");
    when(mockTopicConvention.getDataHubUpgradeHistoryTopicName())
        .thenReturn("DataHubUpgradeHistory");
    when(mockTopicConvention.getMetadataChangeEventTopicName()).thenReturn("MetadataChangeEvent");
    when(mockTopicConvention.getFailedMetadataChangeEventTopicName())
        .thenReturn("FailedMetadataChangeEvent");
    when(mockTopicConvention.getMetadataAuditEventTopicName()).thenReturn("MetadataAuditEvent");

    // Create a test-specific implementation that doesn't try to load external resources
    schemaRegistryService = createTestSchemaRegistryService(mockTopicConvention);
  }

  private SchemaRegistryService createTestSchemaRegistryService(TopicConvention convention) {
    // Use the actual SchemaRegistryServiceImpl for testing with real schemas
    return new SchemaRegistryServiceImpl(convention);
  }

  @Test
  public void testGetSchemaForTopicAndVersion_MCP_Version1() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1);

    Assert.assertTrue(schema.isPresent());
    Assert.assertEquals(schema.get().getName(), "MetadataChangeProposal");
    Assert.assertEquals(schema.get().getNamespace(), "com.linkedin.pegasus2avro.mxe");
  }

  @Test
  public void testGetSchemaForTopicAndVersion_MCP_Version2() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 2);

    Assert.assertTrue(schema.isPresent());
    Assert.assertEquals(schema.get().getName(), "MetadataChangeProposal");
    Assert.assertEquals(schema.get().getNamespace(), "com.linkedin.pegasus2avro.mxe");
  }

  @Test
  public void testGetSchemaForTopicAndVersion_FMCP_Version1() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 1);
    Assert.assertTrue(schema.isPresent());
    Assert.assertEquals(schema.get().getNamespace(), "com.linkedin.mxe");
    Assert.assertEquals(schema.get().getName(), "FailedMetadataChangeProposal");
  }

  @Test
  public void testGetSchemaForTopicAndVersion_FMCP_Version2() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 2);

    Assert.assertTrue(schema.isPresent());
    Assert.assertEquals(schema.get().getName(), "FailedMetadataChangeProposal");
    Assert.assertEquals(schema.get().getNamespace(), "com.linkedin.pegasus2avro.mxe");
  }

  @Test
  public void testGetSchemaForTopicAndVersion_InvalidVersion() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 3);

    Assert.assertFalse(schema.isPresent());
  }

  @Test
  public void testGetSchemaForTopicAndVersion_InvalidTopic() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopicAndVersion("InvalidTopic", 1);

    Assert.assertFalse(schema.isPresent());
  }

  @Test
  public void testGetLatestSchemaVersionForTopic_MCP() {
    Optional<Integer> latestVersion =
        schemaRegistryService.getLatestSchemaVersionForTopic(MCP_TOPIC);

    Assert.assertTrue(latestVersion.isPresent());
    Assert.assertEquals(latestVersion.get().intValue(), 2);
  }

  @Test
  public void testGetLatestSchemaVersionForTopic_FMCP() {
    Optional<Integer> latestVersion =
        schemaRegistryService.getLatestSchemaVersionForTopic(FMCP_TOPIC);

    Assert.assertTrue(latestVersion.isPresent());
    Assert.assertEquals(latestVersion.get().intValue(), 2);
  }

  @Test
  public void testGetLatestSchemaVersionForTopic_SingleVersion() {
    Optional<Integer> latestVersion =
        schemaRegistryService.getLatestSchemaVersionForTopic(MCL_TOPIC);

    Assert.assertTrue(latestVersion.isPresent());
    Assert.assertEquals(latestVersion.get().intValue(), 1);
  }

  @Test
  public void testGetSupportedSchemaVersionsForTopic_MCP() {
    Optional<List<Integer>> versions =
        schemaRegistryService.getSupportedSchemaVersionsForTopic(MCP_TOPIC);

    Assert.assertTrue(versions.isPresent());
    List<Integer> versionList = versions.get();
    Assert.assertEquals(versionList.size(), 2);
    Assert.assertTrue(versionList.contains(1));
    Assert.assertTrue(versionList.contains(2));
  }

  @Test
  public void testGetSupportedSchemaVersionsForTopic_FMCP() {
    Optional<List<Integer>> versions =
        schemaRegistryService.getSupportedSchemaVersionsForTopic(FMCP_TOPIC);

    Assert.assertTrue(versions.isPresent());
    List<Integer> versionList = versions.get();
    Assert.assertEquals(versionList.size(), 2);
    Assert.assertTrue(versionList.contains(1));
    Assert.assertTrue(versionList.contains(2));
  }

  @Test
  public void testGetSupportedSchemaVersionsForTopic_SingleVersion() {
    Optional<List<Integer>> versions =
        schemaRegistryService.getSupportedSchemaVersionsForTopic(MCL_TOPIC);

    Assert.assertTrue(versions.isPresent());
    List<Integer> versionList = versions.get();
    Assert.assertEquals(versionList.size(), 1);
    Assert.assertTrue(versionList.contains(1));
  }

  @Test
  public void testGetSchemaForTopic_DefaultLatest() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopic(MCP_TOPIC);

    Assert.assertTrue(schema.isPresent());
    // Should return version 2 (latest) by default
    Assert.assertEquals(schema.get().getName(), "MetadataChangeProposal");
  }

  @Test
  public void testGetSchemaForTopic_SingleVersion() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForTopic(MCL_TOPIC);

    Assert.assertTrue(schema.isPresent());
    Assert.assertEquals(schema.get().getName(), "MetadataChangeLog");
  }

  @Test
  public void testGetSchemaIdForTopic() {
    Optional<Integer> id = schemaRegistryService.getSchemaIdForTopic(MCP_TOPIC);

    Assert.assertTrue(id.isPresent());
    Assert.assertEquals(id.get().intValue(), 0);
  }

  @Test
  public void testGetSchemaForId() {
    Optional<Schema> schema = schemaRegistryService.getSchemaForId(0);

    Assert.assertTrue(schema.isPresent());
    Assert.assertEquals(schema.get().getName(), "MetadataChangeProposal");
  }

  @Test
  public void testGetAllTopics() {
    List<String> allTopics = schemaRegistryService.getAllTopics();

    Assert.assertNotNull(allTopics);
    Assert.assertFalse(allTopics.isEmpty());

    // Should contain all the expected topics
    Assert.assertTrue(allTopics.contains(MCP_TOPIC));
    Assert.assertTrue(allTopics.contains(FMCP_TOPIC));
    Assert.assertTrue(allTopics.contains(MCL_TOPIC));
    Assert.assertTrue(allTopics.contains(PE_TOPIC));
    Assert.assertTrue(allTopics.contains("MetadataChangeLogTimeseries"));
    Assert.assertTrue(allTopics.contains("DataHubUpgradeHistory"));
    Assert.assertTrue(allTopics.contains("MetadataChangeEvent"));
    Assert.assertTrue(allTopics.contains("FailedMetadataChangeEvent"));
    Assert.assertTrue(allTopics.contains("MetadataAuditEvent"));

    // Should not contain any unexpected topics
    Assert.assertFalse(allTopics.contains("InvalidTopic"));

    // Verify the size matches expected count
    Assert.assertEquals(allTopics.size(), 9);
  }

  @Test
  public void testGetAllTopics_ConsistencyWithIndividualMethods() {
    List<String> allTopics = schemaRegistryService.getAllTopics();

    // Verify that each topic returned by getAllTopics can be accessed individually
    for (String topic : allTopics) {
      Optional<Integer> schemaId = schemaRegistryService.getSchemaIdForTopic(topic);
      Assert.assertTrue(schemaId.isPresent(), "Topic " + topic + " should have a schema ID");

      Optional<Schema> schema = schemaRegistryService.getSchemaForTopic(topic);
      Assert.assertTrue(schema.isPresent(), "Topic " + topic + " should have a schema");
    }
  }

  @Test
  public void testSchemaVersionsAreDifferent() {
    Optional<Schema> v1Schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1);
    Optional<Schema> v2Schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 2);

    Assert.assertTrue(v1Schema.isPresent());
    Assert.assertTrue(v2Schema.isPresent());

    // Both versions should be accessible and have the same name and namespace
    Assert.assertEquals(v1Schema.get().getName(), "MetadataChangeProposal");
    Assert.assertEquals(v2Schema.get().getName(), "MetadataChangeProposal");
    Assert.assertEquals(v1Schema.get().getNamespace(), "com.linkedin.pegasus2avro.mxe");
    Assert.assertEquals(v2Schema.get().getNamespace(), "com.linkedin.pegasus2avro.mxe");
  }

  @Test
  public void testBothTopicsUseDifferentSchemas() {
    // MCP and FMCP topics should use different schemas
    Optional<List<Integer>> mcpVersions =
        schemaRegistryService.getSupportedSchemaVersionsForTopic(MCP_TOPIC);
    Optional<List<Integer>> fmcpVersions =
        schemaRegistryService.getSupportedSchemaVersionsForTopic(FMCP_TOPIC);

    Assert.assertTrue(mcpVersions.isPresent());
    Assert.assertTrue(fmcpVersions.isPresent());

    // Both should support multiple versions but have different schema names
    Assert.assertTrue(mcpVersions.get().size() > 1);
    Assert.assertTrue(fmcpVersions.get().size() > 1);

    // Verify they have different schema names
    Optional<Schema> mcpSchema = schemaRegistryService.getSchemaForTopic(MCP_TOPIC);
    Optional<Schema> fmcpSchema = schemaRegistryService.getSchemaForTopic(FMCP_TOPIC);

    Assert.assertTrue(mcpSchema.isPresent());
    Assert.assertTrue(fmcpSchema.isPresent());
    Assert.assertNotEquals(mcpSchema.get().getName(), fmcpSchema.get().getName());
  }

  @Test
  public void testEventSchemaConstantsIntegration() {
    // Test that the service correctly uses EventSchemaConstants
    int mcpLatestVersion =
        EventSchemaConstants.getLatestSchemaVersion(
            EventUtils.METADATA_CHANGE_PROPOSAL_SCHEMA_NAME);
    Assert.assertEquals(mcpLatestVersion, 2);

    int mclLatestVersion =
        EventSchemaConstants.getLatestSchemaVersion(EventUtils.METADATA_CHANGE_LOG_SCHEMA_NAME);
    Assert.assertEquals(mclLatestVersion, 1);

    int duheLatestVersion =
        EventSchemaConstants.getLatestSchemaVersion(
            EventUtils.DATAHUB_UPGRADE_HISTORY_EVENT_SCHEMA_NAME);
    Assert.assertEquals(duheLatestVersion, 1);
  }

  @Test
  public void testMetadataChangeProposalVersion1MissingAspectCreatedField() {
    // Version 1 should be missing the aspectCreated field
    Optional<Schema> v1Schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1);
    Assert.assertTrue(v1Schema.isPresent());

    // Check that v1 schema doesn't have aspectCreated field in systemMetadata
    boolean hasAspectCreated = hasAspectCreatedField(v1Schema.get());
    Assert.assertFalse(
        hasAspectCreated, "Version 1 MCP schema should not have aspectCreated field");

    // Verify v1 schema has the expected basic fields
    List<String> v1FieldNames =
        v1Schema.get().getFields().stream().map(Schema.Field::name).collect(Collectors.toList());
    Assert.assertTrue(
        v1FieldNames.contains("entityType"), "Version 1 should have entityType field");
    Assert.assertTrue(
        v1FieldNames.contains("changeType"), "Version 1 should have changeType field");
  }

  @Test
  public void testMetadataChangeProposalVersion2IncludesAspectCreatedField() {
    // Version 2 should include the aspectCreated field
    Optional<Schema> v2Schema = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 2);
    Assert.assertTrue(v2Schema.isPresent());

    // Check that v2 schema has aspectCreated field in systemMetadata
    boolean hasAspectCreated = hasAspectCreatedField(v2Schema.get());
    Assert.assertTrue(hasAspectCreated, "Version 2 MCP schema should have aspectCreated field");

    // Verify v2 schema has the expected basic fields
    List<String> v2FieldNames =
        v2Schema.get().getFields().stream().map(Schema.Field::name).collect(Collectors.toList());
    Assert.assertTrue(
        v2FieldNames.contains("entityType"), "Version 2 should have entityType field");
    Assert.assertTrue(
        v2FieldNames.contains("changeType"), "Version 2 should have changeType field");
  }

  @Test
  public void testFailedMetadataChangeProposalVersion1MissingAspectCreatedField() {
    // Version 1 should be missing the aspectCreated field
    Optional<Schema> v1Schema = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 1);
    Assert.assertTrue(v1Schema.isPresent());

    // Check that v1 schema doesn't have aspectCreated field in
    // metadataChangeProposal.systemMetadata
    boolean hasAspectCreated = hasAspectCreatedField(v1Schema.get());
    Assert.assertFalse(
        hasAspectCreated, "Version 1 FMCP schema should not have aspectCreated field");

    // Verify v1 schema has the expected top-level fields
    List<String> v1FieldNames =
        v1Schema.get().getFields().stream().map(Schema.Field::name).collect(Collectors.toList());
    Assert.assertTrue(
        v1FieldNames.contains("metadataChangeProposal"),
        "Version 1 should have metadataChangeProposal field");
    Assert.assertTrue(v1FieldNames.contains("error"), "Version 1 should have error field");
  }

  @Test
  public void testFailedMetadataChangeProposalVersion2IncludesAspectCreatedField() {
    // Version 2 should include the aspectCreated field
    Optional<Schema> v2Schema = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 2);
    Assert.assertTrue(v2Schema.isPresent());

    // Check that v2 schema has aspectCreated field in metadataChangeProposal.systemMetadata
    boolean hasAspectCreated = hasAspectCreatedField(v2Schema.get());
    Assert.assertTrue(hasAspectCreated, "Version 2 FMCP schema should have aspectCreated field");

    // Verify v2 schema has the expected top-level fields
    List<String> v2FieldNames =
        v2Schema.get().getFields().stream().map(Schema.Field::name).collect(Collectors.toList());
    Assert.assertTrue(
        v2FieldNames.contains("metadataChangeProposal"),
        "Version 2 should have metadataChangeProposal field");
    Assert.assertTrue(v2FieldNames.contains("error"), "Version 2 should have error field");
  }

  @Test
  public void testAspectCreatedFieldVersioningConsistency() {
    // Test that both MCP and FMCP have consistent field presence across versions
    Optional<Schema> mcpV1 = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1);
    Optional<Schema> mcpV2 = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 2);
    Optional<Schema> fmcpV1 = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 1);
    Optional<Schema> fmcpV2 = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 2);

    Assert.assertTrue(mcpV1.isPresent());
    Assert.assertTrue(mcpV2.isPresent());
    Assert.assertTrue(fmcpV1.isPresent());
    Assert.assertTrue(fmcpV2.isPresent());

    // Check aspectCreated field presence in each version
    boolean mcpV1HasAspectCreated = hasAspectCreatedField(mcpV1.get());
    boolean mcpV2HasAspectCreated = hasAspectCreatedField(mcpV2.get());
    boolean fmcpV1HasAspectCreated = hasAspectCreatedField(fmcpV1.get());
    boolean fmcpV2HasAspectCreated = hasAspectCreatedField(fmcpV2.get());

    // All v1 schemas should be consistent (none should have aspectCreated)
    Assert.assertEquals(
        mcpV1HasAspectCreated,
        fmcpV1HasAspectCreated,
        "All v1 schemas should have consistent aspectCreated field presence");

    // All v2 schemas should be consistent (all should have same aspectCreated field presence)
    Assert.assertEquals(
        mcpV2HasAspectCreated,
        fmcpV2HasAspectCreated,
        "All v2 schemas should have consistent aspectCreated field presence");

    // Document current state
    System.out.println("MCP v1 has aspectCreated: " + mcpV1HasAspectCreated);
    System.out.println("MCP v2 has aspectCreated: " + mcpV2HasAspectCreated);
    System.out.println("FMCP v1 has aspectCreated: " + fmcpV1HasAspectCreated);
    System.out.println("FMCP v2 has aspectCreated: " + fmcpV2HasAspectCreated);
  }

  @Test
  public void testSchemaFieldCountsForVersioning() {
    // Test that schema field counts are consistent with versioning expectations
    Optional<Schema> mcpV1 = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 1);
    Optional<Schema> mcpV2 = schemaRegistryService.getSchemaForTopicAndVersion(MCP_TOPIC, 2);
    Optional<Schema> fmcpV1 = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 1);
    Optional<Schema> fmcpV2 = schemaRegistryService.getSchemaForTopicAndVersion(FMCP_TOPIC, 2);

    Assert.assertTrue(mcpV1.isPresent());
    Assert.assertTrue(mcpV2.isPresent());
    Assert.assertTrue(fmcpV1.isPresent());
    Assert.assertTrue(fmcpV2.isPresent());

    int mcpV1FieldCount = mcpV1.get().getFields().size();
    int mcpV2FieldCount = mcpV2.get().getFields().size();
    int fmcpV1FieldCount = fmcpV1.get().getFields().size();
    int fmcpV2FieldCount = fmcpV2.get().getFields().size();

    // Document current field counts
    System.out.println("MCP v1 field count: " + mcpV1FieldCount);
    System.out.println("MCP v2 field count: " + mcpV2FieldCount);
    System.out.println("FMCP v1 field count: " + fmcpV1FieldCount);
    System.out.println("FMCP v2 field count: " + fmcpV2FieldCount);

    // Verify that v1 and v2 have the same top-level field count
    // The aspectCreated field is nested within systemMetadata, not a top-level field
    Assert.assertEquals(
        mcpV1FieldCount, mcpV2FieldCount, "MCP v1 and v2 should have same top-level field count");
    Assert.assertEquals(
        fmcpV1FieldCount,
        fmcpV2FieldCount,
        "FMCP v1 and v2 should have same top-level field count");
  }

  /**
   * Helper method to check if a schema has the aspectCreated field nested within systemMetadata
   * Handles both MCP (direct systemMetadata) and FMCP (metadataChangeProposal.systemMetadata)
   * schemas
   */
  private boolean hasAspectCreatedField(Schema schema) {
    for (Schema.Field field : schema.getFields()) {
      if ("systemMetadata".equals(field.name())) {
        // MCP schema - direct systemMetadata field
        return checkSystemMetadataForAspectCreated(field.schema());
      } else if ("metadataChangeProposal".equals(field.name())) {
        // FMCP schema - nested metadataChangeProposal field
        Schema mcpSchema = field.schema();
        if (mcpSchema.getType() == Schema.Type.UNION) {
          // Handle union types (nullable fields)
          for (Schema unionSchema : mcpSchema.getTypes()) {
            if (unionSchema.getType() == Schema.Type.RECORD) {
              // Check if this record has systemMetadata field
              for (Schema.Field nestedField : unionSchema.getFields()) {
                if ("systemMetadata".equals(nestedField.name())) {
                  return checkSystemMetadataForAspectCreated(nestedField.schema());
                }
              }
            }
          }
        } else if (mcpSchema.getType() == Schema.Type.RECORD) {
          // Direct record type
          for (Schema.Field nestedField : mcpSchema.getFields()) {
            if ("systemMetadata".equals(nestedField.name())) {
              return checkSystemMetadataForAspectCreated(nestedField.schema());
            }
          }
        }
      }
    }
    return false;
  }

  /** Helper method to check if a systemMetadata schema has the aspectCreated field */
  private boolean checkSystemMetadataForAspectCreated(Schema systemMetadataSchema) {
    // Handle union types (nullable fields)
    if (systemMetadataSchema.getType() == Schema.Type.UNION) {
      for (Schema unionSchema : systemMetadataSchema.getTypes()) {
        if (unionSchema.getType() == Schema.Type.RECORD) {
          // Check if this record has aspectCreated field
          for (Schema.Field nestedField : unionSchema.getFields()) {
            if ("aspectCreated".equals(nestedField.name())) {
              return true;
            }
          }
        }
      }
    } else if (systemMetadataSchema.getType() == Schema.Type.RECORD) {
      // Direct record type
      for (Schema.Field nestedField : systemMetadataSchema.getFields()) {
        if ("aspectCreated".equals(nestedField.name())) {
          return true;
        }
      }
    }
    return false;
  }
}
