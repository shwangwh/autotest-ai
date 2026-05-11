package com.testplatform.util;

public final class TestMetadata {
    public static final String TEST_TYPE_FUNCTIONAL = "FUNCTIONAL";
    public static final String TEST_TYPE_INTERFACE = "INTERFACE";
    public static final String TEST_TYPE_WEB = "WEB";
    public static final String TEST_TYPE_WEB_AUTOMATION = "WEB_AUTOMATION";

    public static final String EXECUTION_TYPE_MANUAL = "MANUAL";
    public static final String EXECUTION_TYPE_API_AUTOMATION = "API_AUTOMATION";
    public static final String EXECUTION_TYPE_WEB_AUTOMATION = "WEB_AUTOMATION";

    public static final String POINT_SOURCE_LLM_GENERATED = "LLM_GENERATED";
    public static final String POINT_SOURCE_MANUAL_CREATED = "MANUAL_CREATED";
    public static final String POINT_SOURCE_MIGRATED = "MIGRATED";

    private TestMetadata() {
    }

    public static String normalizeTestType(String testType) {
        if (TEST_TYPE_FUNCTIONAL.equalsIgnoreCase(testType)) {
            return TEST_TYPE_FUNCTIONAL;
        }
        return TEST_TYPE_INTERFACE;
    }

    public static String normalizeExecutionType(String executionType, String testType) {
        if (EXECUTION_TYPE_WEB_AUTOMATION.equalsIgnoreCase(executionType)) {
            return EXECUTION_TYPE_WEB_AUTOMATION;
        }
        if (EXECUTION_TYPE_MANUAL.equalsIgnoreCase(executionType)) {
            return EXECUTION_TYPE_MANUAL;
        }
        if (TEST_TYPE_FUNCTIONAL.equals(normalizeTestType(testType))) {
            return EXECUTION_TYPE_MANUAL;
        }
        return EXECUTION_TYPE_API_AUTOMATION;
    }

    public static String normalizePointSourceType(String sourceType) {
        if (POINT_SOURCE_LLM_GENERATED.equalsIgnoreCase(sourceType)) {
            return POINT_SOURCE_LLM_GENERATED;
        }
        if (POINT_SOURCE_MANUAL_CREATED.equalsIgnoreCase(sourceType)) {
            return POINT_SOURCE_MANUAL_CREATED;
        }
        return POINT_SOURCE_MIGRATED;
    }

    public static boolean isInterfaceTest(String testType, String executionType, String requestData) {
        if (EXECUTION_TYPE_API_AUTOMATION.equalsIgnoreCase(executionType)) {
            return true;
        }
        if (requestData != null && !requestData.isBlank()) {
            return true;
        }
        return TEST_TYPE_INTERFACE.equals(normalizeTestType(testType));
    }

    public static boolean isWebTest(String testType, String executionType) {
        return TEST_TYPE_WEB.equalsIgnoreCase(testType)
            || TEST_TYPE_WEB_AUTOMATION.equalsIgnoreCase(testType)
            || EXECUTION_TYPE_WEB_AUTOMATION.equalsIgnoreCase(executionType);
    }
}
