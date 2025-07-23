package de.datev.refsys.aggregation.processing;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.core.options.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;

/**
 * Use junit-platform-suite to tell both IDE and maven where to find tests. The "features" folder in
 * src/test/resources contains all .feature files of this project.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@RunMe and not @Skip")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.datev.refsys.aggregation.processing")
public class RunCucumberTest {
}