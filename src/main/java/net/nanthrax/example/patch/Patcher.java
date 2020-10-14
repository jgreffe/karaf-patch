package net.nanthrax.example.patch;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Dictionary;
import java.util.Hashtable;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Patcher implements BundleActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Patcher.class);

    private final static String PATCH_VERSION = "1.1.0.CF1";

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        System.out.println("Patching with " + PATCH_VERSION);
        LOGGER.info("Patching with " + PATCH_VERSION);
        updateSystem(bundleContext);
        updateEtc(bundleContext);
        updateBundles(bundleContext);
        updateFeatures(bundleContext);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        // nothing to do
    }

    void updateSystem(BundleContext bundleContext) throws Exception {
        System.out.println("* Updating system repository");
        LOGGER.info("* Updating system repository");
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("/system/commons-lang-2.6.jar");
        File system = new File(System.getProperty("karaf.base"), "system");
        File target = new File(system, "commons-lang/commons-lang/2.6");
        target.mkdirs();
        File file = new File(target, "commons-lang-2.6.jar");
        Files.copy(stream, file.toPath(), REPLACE_EXISTING);
    }

    void updateEtc(BundleContext bundleContext) throws Exception {
        System.out.println("* Updating configuration");
        LOGGER.info("* Updating configuration");
        System.out.println("** Updating etc");
        LOGGER.info("** Updating etc");
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("/etc/my.new.config.cfg");
        File etc = new File(System.getProperty("karaf.etc"), "my.new.config.cfg");
        Files.copy(stream, etc.toPath(), REPLACE_EXISTING);
        if (bundleContext == null) {
            return;
        }
        System.out.println("** Updating configuration service");
        LOGGER.info("** Updating configuration service");
        ServiceReference<ConfigurationAdmin> configurationAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class);
        if (configurationAdminServiceReference == null) {
            System.err.println("No configuration service found");
            LOGGER.warn("No configuration service found");
            return;
        }
        ConfigurationAdmin configurationAdmin = bundleContext.getService(configurationAdminServiceReference);
        if (configurationAdmin == null) {
            System.err.println("No configuration service found");
            LOGGER.warn("No configuration service found");
            return;
        }
        Configuration configuration = configurationAdmin.getConfiguration("org.ops4j.pax.web", null);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        if (dictionary == null) {
            dictionary = new Hashtable<>();
        }
        dictionary.put("org.osgi.http.port.secure", "8443");
        dictionary.put("org.osgi.http.secure.enabled", "true");
        configuration.update(dictionary);
    }

    void updateBundles(BundleContext bundleContext) throws Exception {
        System.out.println("** Updating bundles");
        LOGGER.info("** Updating bundles");
        ServiceReference<BundleService> ref = bundleContext.getServiceReference(BundleService.class);
        if (ref == null) {
            System.err.println("No bundle service found");
            LOGGER.warn("No bundle service found");
            return;
        }
        BundleService bundleService = bundleContext.getService(ref);
        if (bundleService == null) {
            System.err.println("No bundle service found");
            LOGGER.warn("No bundle service found");
            return;
        }
        // Bundle bundle = bundleService.getBundle(..);
        System.out.println("*** Installing commons-lang 2.6");
        LOGGER.info("Installing commons-lang 2.6");
        bundleContext.installBundle("mvn:commons-lang/commons-lang/2.6");
    }

    void updateFeatures(BundleContext bundleContext) throws Exception {
        System.out.println("** Updating features");
        LOGGER.info("** Updating features");
        ServiceReference<FeaturesService> ref = bundleContext.getServiceReference(FeaturesService.class);
        if (ref == null) {
            System.err.println("No features service found");
            LOGGER.warn("No features service found");
            return;
        }
        FeaturesService featuresService = bundleContext.getService(ref);
        if (featuresService == null) {
            System.err.println("No features service found");
            LOGGER.warn("No features service found");
            return;
        }
        System.out.println("*** Installing webconsole feature");
        LOGGER.info("*** Installing webconsole feature");
        featuresService.installFeature("webconsole");
    }

}
