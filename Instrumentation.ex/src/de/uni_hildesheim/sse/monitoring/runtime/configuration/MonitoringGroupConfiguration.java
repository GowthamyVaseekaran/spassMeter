package de.uni_hildesheim.sse.monitoring.runtime.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import de.uni_hildesheim.sse.monitoring.runtime.boot.DebugState;
import de.uni_hildesheim.sse.monitoring.runtime.boot.GroupAccountingType;
import de.uni_hildesheim.sse.monitoring.runtime.boot.ResourceType;

/**
 * Stores the (consistent) configuration for a monitoring group.
 * 
 * @author Holger Eichelberger
 * @since 1.00
 * @version 1.00
 */
public class MonitoringGroupConfiguration {

    /**
     * Stores the unmodifiable default instance.
     */
    public static final MonitoringGroupConfiguration DEFAULT = 
        new MonitoringGroupConfiguration();

    /**
     * Stores the unmodifiable stub instance which signals that 
     * a monitoring group was generically defined only based on its name but
     * without clear configuration.
     */
    public static final MonitoringGroupConfiguration STUB = 
        new MonitoringGroupConfiguration();
    
    /**
     * Stores the group accounting type.
     */
    private GroupAccountingType accounting = GroupAccountingType.DEFAULT;

    /**
     * Stores the accountable resources.
     */
    private ResourceType[] resourceTypes = ResourceType.SET_DEFAULT;

    /**
     * Define any combination of debug states for additional information to
     * be emitted during monitoring.
     * 
     * @since 1.00
     */
    private DebugState[] debug = DebugState.DEFAULT;
    
    /**
     * Creates the default instance.
     * 
     * @since 1.00
     */
    private MonitoringGroupConfiguration() {
    }
    
    /**
     * Creates a new monitoring group configuration.
     * 
     * @param debug any combination of debug states for additional information 
     *   to be emitted during monitoring
     * @param accounting the group accounting strategy (may be <b>null</b>, then
     *   the default accounting specified in the configuration is applied)
     * @param resourceTypes the accountable resources (may be <b>null</b>, then
     *   all available resources are accounted).
     * 
     * @since 1.00
     */
    private MonitoringGroupConfiguration(DebugState[] debug, 
        GroupAccountingType accounting, ResourceType[] resourceTypes) {
        if (null != debug) {
            // just keep default in case of null
            this.debug = debug;
        }
        if (null != resourceTypes) {
            // just keep default in case of null
            this.resourceTypes = resourceTypes;
        }
        if (null != accounting) {
            // just keep default in case of null
            this.accounting = accounting;
        }
    }

    /**
     * Creates a new monitoring group configuration from equivalent string 
     * values.
     * 
     * @param debug the debug states (may be <b>null</b>, then no debug state
     *   is considered; comma separated)
     * @param accounting the group accounting strategy (may be <b>null</b>, then
     *   the default accounting specified in the configuration is applied)
     * @param resources the accountable resources (may be <b>null</b>, then
     *   all available resources are accounted; comma separated).
     * 
     * @since 1.00
     */
/*    public MonitoringGroupConfiguration(String debug, String accounting, 
        String resources) {
        if (null != debug) {
            StringTokenizer tokens = new StringTokenizer(debug, ",");
            List<DebugState> states = new ArrayList<DebugState>();
            while (tokens.hasMoreTokens()) {
                String state = tokens.nextToken();
                for (DebugState res : DebugState.values()) {
                    if (res.name().equals(state)) {
                        states.add(res);
                        break;
                    }
                }
            }
            if (!states.isEmpty()) {
                this.debug = new DebugState[states.size()];
                states.toArray(this.debug);
            }
        }
        if (null != accounting) {
            for (GroupAccountingType gValue : GroupAccountingType.values()) {
                if (gValue.name().equals(accounting)) {
                    this.accounting = gValue;
                    break;
                }
            }
            if (this.accounting == GroupAccountingType.DEFAULT) {
                this.accounting = Configuration.INSTANCE.
                    getGroupAccountingType();
            }
        }
        if (null != resources) {
            StringTokenizer tokens = new StringTokenizer(resources, ",");
            List<ResourceType> foundResources = new ArrayList<ResourceType>();
            while (tokens.hasMoreTokens()) {
                String resource = tokens.nextToken();
                for (ResourceType res : ResourceType.values()) {
                    if (res.name().equals(resource)) {
                        foundResources.add(res);
                        break;
                    }
                }
            }
            if (!foundResources.isEmpty()) {
                this.resourceTypes = new ResourceType[foundResources.size()];
                foundResources.toArray(this.resourceTypes);
            }
        }
    }*/
    
    /**
     * Returns the group accounting to be applied. 
     * 
     * @return the accounting type/strategy
     * 
     * @since 1.00
     */
    public GroupAccountingType getGroupAccounting() {
        return accounting;
    }
    
    /**
     * Defines the resources to be accounted. By default, all resources defined
     * in {@link ResourceType} are accounted.
     * 
     * @return the accountable resources
     * 
     * @since 1.00
     */
    public ResourceType[] getResources() { 
        return resourceTypes;
    }

    /**
     * Returns the combination of debug states for additional information 
     * to be emitted during monitoring.
     * 
     * @return the debug information
     * 
     * @since 1.00
     */
    public DebugState[] getDebug() { 
        return debug;
    }

    /**
     * Creates a new monitoring group configuration and checks for references
     * to {@link #DEFAULT}.
     * 
     * @param debug any combination of debug states for additional information 
     *   to be emitted during monitoring (must not be <b>null</b>)
     * @param accounting the group accounting strategy (must not be <b>null</b>)
     * @param resourceTypes the accountable resources (must not be <b>null</b>)
     * @return the created monitoring group configuration or {@link #DEFAULT}
     * 
     * @since 1.00
     */
    public static final MonitoringGroupConfiguration create(DebugState[] debug, 
        GroupAccountingType accounting, ResourceType[] resourceTypes) {
        Configuration conf = Configuration.INSTANCE;

        resourceTypes = ResourceType.ensureSubset(
            conf.getAccountableResources(), resourceTypes);
        if (0 == resourceTypes.length 
            || ResourceType.SET_DEFAULT == resourceTypes) {
            resourceTypes = Configuration.INSTANCE.getDefaultGroupResources();
        }
        
        if (GroupAccountingType.DEFAULT == DEFAULT.accounting) {
            // replace once by the current configuration value
            // better lazy here than at each read request
            DEFAULT.accounting = conf.getGroupAccountingType();
        }
        if (ResourceType.SET_DEFAULT == DEFAULT.resourceTypes) {
            // replace once by the current configuration value
            // better lazy here than at each read request
            DEFAULT.resourceTypes = conf.getDefaultGroupResources();
        }
        // we do not replace debug because there is no default in Configuration
        
        if (GroupAccountingType.LOCAL == conf.getGroupAccountingType()) {
            // take over LOCAL if set in config
            accounting = GroupAccountingType.LOCAL;
        } else if (GroupAccountingType.LOCAL == accounting) {
            // if local but not global, set to default
            accounting = GroupAccountingType.DEFAULT;
        }
        if (GroupAccountingType.DEFAULT == accounting) {
            // this is the only explicit default value which needs to be 
            // replaced
            accounting = conf.getGroupAccountingType();
        }
        
        MonitoringGroupConfiguration result;
        if (accounting == DEFAULT.accounting
            && Arrays.deepEquals(resourceTypes, DEFAULT.resourceTypes)
            && Arrays.deepEquals(debug, DEFAULT.debug)) {
            // unify references and safe memory
            result = DEFAULT;
        } else {
            // create new instance
            result = new MonitoringGroupConfiguration(debug, accounting, 
                resourceTypes);
        }
        return result;
    }
    
    /**
     * Creates a new monitoring group configuration from equivalent string 
     * values.
     * 
     * @param debug the debug states (may be <b>null</b>, then no debug state
     *   is considered; comma separated)
     * @param accounting the group accounting strategy (may be <b>null</b>, then
     *   the default accounting specified in the configuration is applied)
     * @param resources the accountable resources (may be <b>null</b>, then
     *   all available resources are accounted; comma separated).
     * @return the created monitoring group configuration or {@link #DEFAULT}
     * 
     * @since 1.00
     */
    public static MonitoringGroupConfiguration create(
        String debug, String accounting, String resources) {
        DebugState[] aDebug = DebugState.DEFAULT;
        GroupAccountingType aAccounting = GroupAccountingType.DEFAULT;
        ResourceType[] aResources = ResourceType.SET_DEFAULT;
        if (null != debug) {
            StringTokenizer tokens = new StringTokenizer(debug, ",");
            List<DebugState> states = new ArrayList<DebugState>();
            while (tokens.hasMoreTokens()) {
                String state = tokens.nextToken();
                for (DebugState res : DebugState.values()) {
                    if (res.name().equals(state)) {
                        states.add(res);
                        break;
                    }
                }
            }
            if (!states.isEmpty()) {
                aDebug = new DebugState[states.size()];
                states.toArray(aDebug);
            }
        }
        if (null != accounting) {
            for (GroupAccountingType gValue : GroupAccountingType.values()) {
                if (gValue.name().equals(accounting)) {
                    aAccounting = gValue;
                    break;
                }
            }
        }
        if (null != resources) {
            StringTokenizer tokens = new StringTokenizer(resources, ",");
            ArrayList<ResourceType> foundResources 
                = new ArrayList<ResourceType>();
            while (tokens.hasMoreTokens()) {
                String resource = tokens.nextToken();
                for (ResourceType res : ResourceType.values()) {
                    if (res.name().equals(resource)) {
                        foundResources.add(res);
                        break;
                    }
                }
            }
            if (!foundResources.isEmpty()) {
                aResources = new ResourceType[foundResources.size()];
                foundResources.toArray(aResources);
            }
        }
        return create(aDebug, aAccounting, aResources);
    }
    
    /**
     * Returns weather this configuration is consistent with the given one.
     * 
     * @param conf the configuration to check for
     * @return <code>true</code> if both configurations are consistent 
     *     (excluding {@link #debug}), <code>false</code> else
     * 
     * @since 1.00
     */
    public boolean isConsistent(MonitoringGroupConfiguration conf) {
        boolean consistent = true;
        if (null == conf) {
            consistent = false;
        } else {
            consistent &= accounting == conf.accounting;
            consistent &= resourceTypes.length == conf.resourceTypes.length;
            for (int i = 0; consistent && i < resourceTypes.length; i++) {
                consistent &= resourceTypes[i] == conf.resourceTypes[i];
            }
            // do not consider debug
        }
        return consistent;
    }

}
