package ng.fixpay.core.payment.rail;

import ng.fixpay.shared.payment.PaymentRailExtensionPoint;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Manages the PF4J plugin lifecycle for hot-loadable payment rail processor plugins.
 *
 * <p>On {@link ApplicationReadyEvent}, scans the configured {@code pf4j.plugins-dir}
 * for plugin JARs. Any class implementing {@link PaymentRailExtensionPoint} found inside
 * a loaded plugin is exposed to {@link PaymentRailRegistry} which merges it with the
 * built-in Spring bean adapters.
 *
 * <h3>Deploying a new processor</h3>
 * <ol>
 *   <li>Drop the processor plugin JAR into the {@code pf4j.plugins-dir} directory.</li>
 *   <li>Restart the core service, <em>or</em> call the admin API to reload plugins.</li>
 *   <li>Create a {@code payment_rail_config} row via the admin wizard pointing to the
 *       new processor's ID.</li>
 * </ol>
 */
@Component
public class FixPayPluginManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(FixPayPluginManager.class);

    private final PluginManager pluginManager;
    private final String pluginsDir;

    public FixPayPluginManager(@Value("${pf4j.plugins-dir:./plugins}") String pluginsDir) {
        this.pluginsDir = pluginsDir;
        this.pluginManager = new JarPluginManager(Paths.get(pluginsDir));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadPlugins() {
        var dir = Paths.get(pluginsDir);
        if (!Files.isDirectory(dir)) {
            log.info("[FixPayPluginManager] Plugins directory '{}' not found — no external processor plugins loaded.", pluginsDir);
            return;
        }
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        int count = getExtensions().size();
        log.info("[FixPayPluginManager] Loaded {} payment-rail plugin extension(s) from '{}'", count, pluginsDir);
        getExtensions().forEach(ext -> log.info("[FixPayPluginManager]  ↳ plugin processorId='{}'", ext.processorId()));
    }

    /** Returns all {@link PaymentRailExtensionPoint} implementations from loaded plugins. */
    public List<PaymentRailExtensionPoint> getExtensions() {
        return pluginManager.getExtensions(PaymentRailExtensionPoint.class);
    }

    /** Returns metadata for all loaded plugin JARs. */
    public List<PluginWrapper> getLoadedPlugins() {
        return pluginManager.getPlugins();
    }

    /**
     * Unloads a plugin by its plugin ID. The adapter will be removed from the registry
     * after the next call to {@link PaymentRailRegistry#refreshPluginAdapters()}.
     *
     * @return {@code true} if unloaded successfully, {@code false} if the plugin was not found
     */
    public boolean unloadPlugin(String pluginId) {
        try {
            pluginManager.unloadPlugin(pluginId);
            log.info("[FixPayPluginManager] Unloaded plugin '{}'", pluginId);
            return true;
        } catch (Exception ex) {
            log.error("[FixPayPluginManager] Failed to unload plugin '{}': {}", pluginId, ex.getMessage());
            return false;
        }
    }

    @Override
    public void destroy() {
        try {
            pluginManager.stopPlugins();
            pluginManager.unloadPlugins();
        } catch (Exception ex) {
            log.warn("[FixPayPluginManager] Error during plugin shutdown: {}", ex.getMessage());
        }
    }
}
