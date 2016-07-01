package org.sitoolkit.wt.infra;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource(ignoreResourceNotFound = true, value = { "classpath:sit-wt-default.properties",
        "classpath:sit-wt.properties" })
public abstract class PropertyManager {

    @Value("${window.width}")
    private int windowWidth;

    @Value("${window.height}")
    private int windowHeight;

    @Value("${window.top}")
    private int windowTop;

    @Value("${window.left}")
    private int windowLeft;

    @Value("${window.shift.top}")
    private int windowShiftTop;

    @Value("${window.shift.left}")
    private int windowShiftLeft;

    @Value("${implicitlyWait}")
    private int implicitlyWait;

    @Value("${operationWait}")
    private int operationWait;

    @Value("${window.resize}")
    private boolean resizeWindow;

    @Value("${pageobj.dir}")
    private String pageObjectDir;

    @Value("${driver.type}")
    private String driverType;

    @Value("${appium.address}")
    private String appiumAddress;

    @Value("${screenshot.mode}")
    private String screenshotMode;

    @Value("${screenshot.resize}")
    private boolean screenshotResize;

    @Value("${screenshot.padding.width}")
    private int screenshotPaddingWidth;

    @Value("${screenshot.padding.height}")
    private int screenshotPaddingHeight;

    @Value("${selenium.screenshot.pattern}")
    private String seleniumScreenshotPattern;

    @Value("${baseUrl}")
    private String baseUrl;

    @Value("${hubUrl}")
    private String hubUrl;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        pspc.setIgnoreUnresolvablePlaceholders(true);
        return pspc;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getImplicitlyWait() {
        return implicitlyWait;
    }

    public boolean isResizeWindow() {
        return resizeWindow;
    }

    public String getPageObjectDir() {
        return pageObjectDir;
    }

    public String getDriverType() {
        return driverType;
    }

    public boolean isEdgeDriver() {
        return "edge".equalsIgnoreCase(driverType);
    }

    public boolean isIEDriver() {
        return "ie".equalsIgnoreCase(driverType);
    }

    public boolean isMsDriver() {
        return isEdgeDriver() || isIEDriver();
    }

    public boolean isSafariDriver() {
        return "safari".equalsIgnoreCase(driverType);
    }

    public URL getAppiumAddress() {
        try {
            return new URL(appiumAddress);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("appium.address", e);
        }
    }

    public boolean isScreenshotResize() {
        return screenshotResize;
    }

    public int getScreenshotPaddingWidth() {
        return screenshotPaddingWidth;
    }

    public int getScreenshotPaddingHeight() {
        return screenshotPaddingHeight;
    }

    public Pattern getSeleniumScreenshotPattern() {
        try {
            return Pattern.compile(seleniumScreenshotPattern);
        } catch (PatternSyntaxException e) {
            throw new ConfigurationException("selenium.screenshot.pattern", e);
        }
    }

    public String getScreenthotMode() {
        return screenshotMode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public int getWindowTop() {
        return windowTop;
    }

    public int getWindowLeft() {
        return windowLeft;
    }

    public int getOperationWait() {
        return operationWait;
    }

    public int getWindowShiftTop() {
        return windowShiftTop;
    }

    public int getWindowShiftLeft() {
        return windowShiftLeft;
    }
}
