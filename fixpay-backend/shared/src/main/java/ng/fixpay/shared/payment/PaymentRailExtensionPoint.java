package ng.fixpay.shared.payment;

import org.pf4j.ExtensionPoint;

/**
 * PF4J extension point for external payment rail processor plugins.
 *
 * <p>Third-party processor developers implement this interface in a PF4J plugin JAR.
 * The plugin JAR is deployed to the configured {@code pf4j.plugins-dir} directory
 * and is automatically discovered at startup.
 *
 * <h3>Minimal plugin structure</h3>
 * <pre>
 * my-processor-plugin/
 *   src/main/java/com/example/MyAdapter.java   — implements PaymentRailExtensionPoint
 *   src/main/resources/META-INF/extensions.idx — pf4j extension index
 *   plugin.properties                          — plugin metadata (id, version, provider...)
 * </pre>
 *
 * <h3>Required plugin.properties keys</h3>
 * <pre>
 * plugin.id=my-processor-plugin
 * plugin.version=1.0.0
 * plugin.provider=YourCompany
 * plugin.dependencies=
 * </pre>
 */
public interface PaymentRailExtensionPoint extends ExtensionPoint, PaymentRailAdapter {}
