package eu.peppol.inbound.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import eu.peppol.util.GlobalConfiguration;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Configures the SLF4J logging configuration.
 * <p/>
 * User: steinar
 * Date: 04.10.12
 * Time: 13:43
 */
public class LoggingConfigurator {

    // First file we are looking for, this may be modified when creating objects of this class
    private String currentSimpleConfigFileName = "logback-oxalis-inbound.xml";

    // If locating the above file name fails, fall back to this file name
    private static String defaultSimpleConfigFilename = "logback-oxalis.xml";

    private File configFile = null;

    /** Simply uses the default configuration */
    public LoggingConfigurator() {
    }

    File locateLoggingConfigurationFileInClassPathBySimpleName(String fileName) {
        System.out.println("Attempting to locate logback configuration file: "+ fileName);
        URL url = LoggingConfigurator.class.getClassLoader().getResource(fileName);
        if (url != null) {
            try {
                System.out.println("Found " + fileName + " in class path.");
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Unable to convert " + url + " into URI for File object");
            }
        } else {
            return null;
        }
    }

    File locateConfigFile() {
        System.err.println("Attempting to locate the logging configuration file ...");
        // First we consult the Global configuration file
        String inboundLoggingConfiguration = GlobalConfiguration.getInstance().getInboundLoggingConfiguration();
        System.err.println("Trying with " + inboundLoggingConfiguration);

        File f = new File(inboundLoggingConfiguration);
        if (f.exists() && f.canRead() && f.isFile()) {
            return f;
        }

        // Second we try to find the built in defaults in the class path
        f = locateLoggingConfigurationFileInClassPathBySimpleName(currentSimpleConfigFileName);
        if (f == null) {
            if (!defaultSimpleConfigFilename.equals(currentSimpleConfigFileName)) {
                f = locateLoggingConfigurationFileInClassPathBySimpleName(defaultSimpleConfigFilename);
                if (f == null) {
                    throw new IllegalStateException("Unable to locate either " + currentSimpleConfigFileName + " or " + defaultSimpleConfigFilename + " in classpath");
                }
            } else {
                throw new IllegalStateException("Unable to locate " + currentSimpleConfigFileName + " in classpath");
            }
        }
        return f;
    }


    public void execute() {

        configFile = locateConfigFile();
        configWithFile(configFile);
    }

    void configWithFile(File logbackConfigFile) {
        System.out.println("Configuring Logback with configuration: " + logbackConfigFile.getAbsolutePath());
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        // Call context.reset() to clear any previous configuration, e.g. default
        // configuration. For multi-step configuration, omit calling context.reset().
        loggerContext.reset();
        try {
            configurator.doConfigure(logbackConfigFile);

            // Not needed as this is the default behaviour from logback
            // StatusPrinter.print(loggerContext);
        } catch (JoranException e) {
        }

        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }


    public File getConfigurationFile() {
        return configFile;
    }
}
