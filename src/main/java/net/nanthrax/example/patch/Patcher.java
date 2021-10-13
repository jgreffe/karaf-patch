package net.nanthrax.example.patch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.nanthrax.example.patch.exception.FeaturesCoreException;
import net.nanthrax.example.patch.log.PatchLogger;
import net.nanthrax.example.patch.services.PatcherServices;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.util.bundles.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Patcher implements BundleActivator {
    private final static String PATCH_VERSION = "1.1.0.CF1";

    private PatchLogger log = new PatchLogger(LoggerFactory.getLogger(getClass().getName()));
    private PatcherServices services = null;
    private BundleContext bundleContext = null;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        try {
            this.bundleContext = bundleContext;
            services = new PatcherServices(bundleContext);

            log.info("Patching with " + PATCH_VERSION);
            setupCamelOnce();
            updateSystem();
            updateBundles();
            repoRefresh();
            updateFeatures();
            log.info("Patch applied successfully");
        } catch (FeaturesCoreException ignored) {
            log.warn("Restarting bundle");
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        services.stop();
        log.info("** Stopping");
    }

    void setupCamelOnce() throws Exception {
        if (getFeaturesService().getFeature("camel-core") == null) {
            log.info("Setting up camel once");
            copySystem("apache-camel-2.23.1-features.xml",
                    "org/apache/camel/karaf/apache-camel/2.23.1");
            getFeaturesService().addRepository(new URI("mvn:org.apache.camel.karaf/apache-camel/2.23.1/xml/features"));
            getFeaturesService().installFeature("camel-core");
        } else {
            log.info("No need to setup camel");
        }
    }

    void updateSystem() throws Exception {
        log.info("* Updating system repository");
        copySystem("org.apache.karaf.features.core-4.2.7.tesb1.jar",
                "org/apache/karaf/features/org.apache.karaf.features.core/4.2.7.tesb1");
        copySystem("camel-core-2.23.1.tesb2.jar",
                "org/apache/camel/camel-core/2.23.1.tesb2");
        copySystem("standard-4.2.7-features.xml", "org/apache/karaf/features/standard/4.2.7");
        copySystem("framework-4.2.7-features.xml", "org/apache/karaf/features/framework/4.2.7");
        copySystem("apache-camel-2.23.1.tesb1-features.xml", "apache-camel-2.23.1-features.xml",
                "org/apache/camel/karaf/apache-camel/2.23.1");
    }

    void copySystem(String classLoaderPath, String systemPath) throws Exception {
        copySystem(classLoaderPath, classLoaderPath, systemPath);
    }

    void copySystem(String classLoaderPath, String newName, String systemPath) throws Exception {
        InputStream stream = this.getClass().getClassLoader()
                .getResourceAsStream("system/" + classLoaderPath);
        File system = new File(System.getProperty("karaf.base"), "system");
        File target = new File(system, systemPath);
        target.mkdirs();
        File file = new File(target, newName);
        Files.copy(stream, file.toPath(), REPLACE_EXISTING);
    }

    void updateBundles() throws Exception {
        log.info("** Updating bundles");
        updateBundle("org.apache.karaf.features.core",
                "mvn:org.apache.karaf.features/org.apache.karaf.features.core/4.2.7.tesb1");
    }

    void updateBundle(String symbolicName, String location) throws Exception {
        log.info("*** Updating {} with {}", symbolicName, location);
        Bundle featuresCore = null;
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                featuresCore = bundle;
                break;
            }
        }

        if (featuresCore == null) {
            return;
        }

        String updateLocation = getBundleService().getInfo(featuresCore).getUpdateLocation();
        log.info("Current location {}", featuresCore.getLocation());
        log.info("Current update location {}", updateLocation);
        if (!location.equals(featuresCore.getLocation()) && !location.equals(updateLocation)) {
            URL locationUrl = new URL(location);
            try (InputStream is = locationUrl.openStream()) {

                File file = BundleUtils.fixBundleWithUpdateLocation(is, locationUrl.toString());
                try (FileInputStream fis = new FileInputStream(file)) {
                    featuresCore.update(fis);
                }
                file.delete();
            }
            FrameworkWiring wiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
            wiring.refreshBundles(Collections.singletonList(featuresCore));

            if ("org.apache.karaf.features.core".equals(symbolicName)) {
                throw new FeaturesCoreException();
            }

        } else {
            log.info("Already up-to-date");
        }
    }

    void repoRefresh() throws Exception {
        log.info("** Refreshing repository features");
        Set<URI> uris = new LinkedHashSet<>();

        Repository[] repos = getFeaturesService().listRepositories();
        for (Repository repo : repos) {
            uris.add(repo.getURI());
        }

        String uriString = uris.stream().map(URI::toString).collect(Collectors.joining("\n"));
        try {
            log.info("Refreshing repository features urls: {}", uriString);
            getFeaturesService().refreshRepositories(uris);
        } catch (Exception e) {
            log.error("Error refreshing {}: {}", uriString, e.getMessage());
        }
    }

    void updateFeatures() throws Exception {
        log.info("** Updating features");
        installFeature("camel-core");
    }
    
    void installFeature(String feature) throws Exception {
        log.info("*** Installing {} feature", feature);
        getFeaturesService().installFeature(feature);
    }

    String getBundleLocation(Bundle bundle) {
        try {
            return getBundleService().getInfo(bundle).getUpdateLocation();
        } catch (Exception e) {
            log.warn("Issue retrieving updateLocation for bundle {}/{}, falling back to OSGi location",
                    bundle.getSymbolicName(), bundle.getBundleId());
            return bundle.getLocation();
        }
    }

    private ConfigurationAdmin getConfigurationAdmin() {
        return services.getConfigurationAdmin();
    }

    private BundleService getBundleService() {
        return services.getBundleService();
    }

    private FeaturesService getFeaturesService() {
        return services.getFeaturesService();
    }
}
