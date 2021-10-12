package net.nanthrax.example.patch.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.nanthrax.example.patch.log.PatchLogger;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

/**
 * Handles all required services using trackers
 */
public class PatcherServices {

    private PatchLogger log = new PatchLogger(LoggerFactory.getLogger(getClass().getName()));

    /**
     * List of service trackers
     */
    private List<ServiceTracker<?, ?>> serviceTrackers = new ArrayList<>();

    /**
     * Map of latches to keep track of services
     */
    private Map<Class<?>, CountDownLatch> latches = new ConcurrentHashMap<>();

    private Map<Class<?>, Object> services = new HashMap<>();
    
    private BundleContext bundleContext = null;


    public PatcherServices(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        createServiceTracker(ConfigurationAdmin.class);
        createServiceTracker(FeaturesService.class);
        createServiceTracker(BundleService.class);
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return getService(ConfigurationAdmin.class);
    }

    public FeaturesService getFeaturesService() {
        return getService(FeaturesService.class);
    }

    public BundleService getBundleService() {
        return getService(BundleService.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T getService(Class<T> clazz) {
        try {
            boolean waitOk = latches.get(clazz).await(10, TimeUnit.SECONDS);
            if (waitOk) {
                return (T) services.get(clazz);
            } else {
                throw new IllegalStateException("Service " + clazz + " timed out");
            }
        } catch (InterruptedException ie) {
            throw new IllegalStateException("Service " + clazz + " unreacheable");
        } catch (NullPointerException npe) {
            throw new IllegalStateException("Service " + clazz + " has not latch");
        }
    }

    private <T> void createServiceTracker(Class<T> clazz) {
        log.info("Tracking service {}", clazz.getName());
        ServiceTracker<T, T> serviceTracker = new ServiceTracker<T, T>(bundleContext, clazz, null) {

            @Override
            public T addingService(ServiceReference<T> reference) {
                log.info("Service {} added", clazz.getName());
                T service = bundleContext.getService(reference);
                services.put(clazz, service);
                if (clazz.getClassLoader() != service.getClass().getClassLoader()) {
                    log.error("Classloader issue\n\t- expected : {}/{}\n\t- retrieved: {}/{}",
                            clazz.getClassLoader().hashCode(), clazz.getClassLoader(),
                            service.getClass().getClassLoader().hashCode(), service.getClass().getClassLoader());
                }
                latches.computeIfAbsent(clazz, k -> new CountDownLatch(1)).countDown();
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<T> reference, T service) {
                log.info("Service {} modified", clazz.getName());
                services.put(clazz, service);
                latches.put(clazz, new CountDownLatch(0));
            }

            @Override
            public void removedService(ServiceReference<T> reference, T service) {
                log.info("Service {} removed", clazz.getName());
                latches.put(clazz, new CountDownLatch(1));
            }
        };
        serviceTracker.open(true);
        serviceTrackers.add(serviceTracker);
    }

    public void stop() throws Exception {
        for (ServiceTracker<?, ?> serviceTracker : serviceTrackers) {
            serviceTracker.close();
        }
    }
}
