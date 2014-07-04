package de.uni_hildesheim.sse.monitoring.runtime.configuration.xml;

import java.util.StringTokenizer;

import de.uni_hildesheim.sse.monitoring.runtime.boot.ArrayList;
import de.uni_hildesheim.sse.monitoring.runtime.configuration.AnnotationBuilder;
import de.uni_hildesheim.sse.monitoring.runtime.configuration.Annotations;
import de.uni_hildesheim.sse.monitoring.runtime.configuration.Configuration;
import de.uni_hildesheim.sse.monitoring.runtime.configuration.
    ConfigurationEntry;
import de.uni_hildesheim.sse.monitoring.runtime.configuration.
    MonitoringGroupConfiguration;
import de.uni_hildesheim.sse.monitoring.runtime.utils.HashMap;
import de.uni_hildesheim.sse.monitoring.runtime.utils.xml.DocHandler;
import de.uni_hildesheim.sse.monitoring.runtime.utils.xml.QdParserException;

/**
 * Implements the XML handler.
 * 
 * @author Holger Eichelberger
 * @since 1.00
 * @version 1.00
 */
class XMLHandler implements DocHandler {
    
    /**
     * Stores the configuration, i.e. a mapping from signatures to annotation
     * types to annotation builders.
     */
    private HashMap<String, AnnotationBuilderMap> configuration 
        = new HashMap<String, AnnotationBuilderMap>();
    
    /**
     * Stores if this configuration is exclusive, i.e. authoritive or if it may
     * overlap with source code annotations. Default is <code>true</code>.
     */
    private boolean exclusive = true;

    /**
     * Stores the actual stack of annotation builders as read from the XML file.
     */
    private ArrayList<AnnotationBuilder<?>> templateStack 
        = new ArrayList<AnnotationBuilder<?>>();
    
    /**
     * Stores the current path of modules and namespaces.
     */
    private ArrayList<PathElement> currentPath = new ArrayList<PathElement>();

    /**
     * Stores the patterns collected during XML file reading.
     */
    private ArrayList<Pattern> patterns = new ArrayList<Pattern>();

    /**
     * Stores the monitoring group configurations collected during XML file 
     * reading.
     */
    private HashMap<String, MonitoringGroupConfiguration> groupConfigurations 
        = new HashMap<String, MonitoringGroupConfiguration>();
    
    /**
     * Stores referenced but unresolved configurations (temporary attribute,
     * to be resolved at the end of processing.
     */
    private HashMap<String, String> unresolvedConfigurations 
        = new HashMap<String, String>();
    
    /**
     * Returns whether this configuration is exclusive, i.e. source code 
     * annotations should not be considered.
     * 
     * @return <code>true</code> if this configuration is exclusive, 
     *    <code>false</code> else 
     * 
     * @since 1.00
     */
    boolean isExclusive() {
        return exclusive;
    }
    
    /**
     * Returns the configuration constructed while reading an XML file.
     * 
     * @return the configuration
     * 
     * @since 1.00
     */
    HashMap<String, AnnotationBuilderMap> getConfiguration() {
        return configuration;
    }

    /**
     * Returns the patterns identified while reading an XML file.
     * 
     * @return the patterns
     * 
     * @since 1.00
     */
    ArrayList<Pattern> getPatterns() {
        return patterns;
    }
    
    /**
     * Returns the monitoring group configurations identified while reading an
     * XML file.
     * 
     * @return the group configurations
     * 
     * @since 1.00
     */
    HashMap<String, MonitoringGroupConfiguration> getGroupConfigurations() {
        return groupConfigurations;
    }
    
    /**
     * Appends a name to the current path.
     * 
     * @param type the type of the path element
     * @param name the name to be appended
     * @param pattern the pattern or <b>null</b>
     * 
     * @since 1.00
     */
    private void appendToCurrentPath(PathElement.Type type, 
        String name, String pattern) {
        currentPath.add(new PathElement(type, name, pattern));
    }
    
    /**
     * Removes the last name from the current path.
     * 
     * @since 1.00
     */
    private void removeLastFromCurrentPath() {
        if (!currentPath.isEmpty()) {
            currentPath.remove(currentPath.size() - 1);
        }
    }
    
    /**
     * Returns the current path by calculating it from the path elements.
     * 
     * @param asRegEx should the result be given as a matching regular 
     *     expression
     * @return the current path
     * 
     * @since 1.00
     */
    private String getCurrentPath(boolean asRegEx) {
        StringBuilder builder = new StringBuilder();
        PathElement last = null;
        int size = currentPath.size();
        int size1 = size - 1;
        for (int e = 0; e < size; e++) {
            PathElement element = currentPath.get(e);
            boolean isLast = (e == size1);
            if (null != last) {
                // determine the correct separator char, consider
                // escaping meta characters inplace
                if (PathElement.Type.NAMESPACE == last.getType() 
                    || PathElement.Type.DATA == element.getType() 
                    || PathElement.Type.BEHVIOUR == element.getType()) {
                    if (asRegEx) {
                        builder.append("\\");
                    }
                    builder.append(".");
                } else {
                    if (asRegEx) {
                        builder.append("\\");
                    }
                    builder.append("$");
                }
            }
            if (asRegEx) {
                // if a regEx should be returned, append the pattern if 
                // available. Handle end of path separately in order to include
                // the element name if possible (simplifies XML)
                if (!isLast && null != element.getPattern()) {
                    builder.append(element.getPattern());
                } else {
                    builder.append(element.getName());
                }
                if (isLast && null != element.getPattern()) {
                    builder.append(element.getPattern());
                }
            } else {
                // no regEx, just add name
                builder.append(element.getName());
            }
            last = element;
        }
        return builder.toString();
    }

    /**
     * Modifies the path according to the name of the element and its
     * attributes.
     * @param tag the name of the element
     * @param attributes the attributes of <code>tag</code>, may be 
     *     <b>null</b> in order to trigger removal of the last path
     *     element if the current XML element allows this
     * @return the type of the element
     * 
     * @since 1.00
     */
    private PathElement.Type modifyPath(String tag, 
        HashMap<String, String> attributes) {
        PathElement.Type type = PathElement.Type.xmlValueOf(tag);

        if (null != type) {
            String pathElementName = null;
            if (null == pathElementName) {
                if (null != attributes) {
                    if (PathElement.Type.BEHVIOUR == type) {
                        pathElementName = attributes.get("signature");
                    } else {
                        pathElementName = attributes.get("name");
                    }
                }
            }
            // process the data
            if (null != type) {
                if (null != attributes) {
                    if (null != pathElementName) {
                        appendToCurrentPath(type, 
                            pathElementName, attributes.get("pattern"));
                    }
                } else {
                    // attributes == null is flag for removing the last
                    removeLastFromCurrentPath();
                }
            }
        }
        return type;
    }
    
    /**
      * Start of an XML Element.
      *
      * @param tag element name
      * @param attributes element attributes
      * @throws QdParserException parse error
      */
    @Override
    public void startElement(String tag, HashMap<String, String> attributes) 
        throws QdParserException {        
        if ("groupConfiguration".equals(tag)) {
            String id = attributes.get("id");
            if (null == id) {
                Configuration.LOG.config("group configuration without id");
            } else {
                String refId = attributes.get("refId");
                if (null != refId) {
                    unresolvedConfigurations.put(id, refId);
                } else {
                    MonitoringGroupConfiguration conf = 
                        MonitoringGroupConfiguration.create(
                            attributes.get("debug"),
                            attributes.get("groupAccounting"), 
                            attributes.get("resources"));
                    groupConfigurations.put(id, conf); 
                }
            }
        } else if ("configuration".equals(tag)) {
            String tmp = attributes.get("exclusive");
            if (null != tmp) {
                exclusive = Boolean.valueOf(tmp);
            }
            for (HashMap.Entry<String, String> ent : attributes.entries()) {
                String aName = ent.getKey();
                if (!"exclusive".equals(aName) && !aName.startsWith("xmlns")) {
                    ConfigurationEntry entry 
                        = ConfigurationEntry.getEntry(aName);
                    if (null != entry) {
                        try {
                            entry.setValue(ent.getValue());
                        } catch (IllegalArgumentException e) {
                            Configuration.LOG.severe(e.getMessage());
                        }
                    } else {
                        Configuration.LOG.config("attribute '" + aName 
                            + "' is not known");
                    }
                }
            }
        } else {
            PathElement.Type type = modifyPath(tag, attributes);
            AnnotationBuilder<?> template = Annotations.getTemplate(tag);
            boolean nested = false;
            if (null != template) {
                template = template.prepareForUse();
                String cPath = getCurrentPath(false);
                AnnotationBuilderMap builders = configuration.get(cPath);
                if (null == builders) {
                    builders = new AnnotationBuilderMap();
                    configuration.put(cPath, builders);
                }
                builders.put(template.getInstanceClass(), template);
                Configuration.LOG.config("registered: signature " + cPath 
                    + " annotation: " + template.getInstanceClass().getName());
                templateStack.add(template);
            } else {
                if (type == PathElement.Type.NAMESPACE 
                    || type == PathElement.Type.MODULE) {
                    String pattern = attributes.get("pattern");
                    String typeOf = attributes.get("typeOf");
                    if (null != pattern || null != typeOf) {
                        AnnotationBuilder<?> templ 
                            = Annotations.getTemplate("monitor");
                        if (null != templ) {
                            templ = templ.prepareForUse();
                            String cPath = getCurrentPath(true);
                            if (null == pattern || 0 == pattern.length()) {
                                cPath += ".*";
                            }
                            parsePatterns(cPath, typeOf, templ);
                        }
                    }
                } else if (!templateStack.isEmpty()) {
                    template = templateStack.get(templateStack.size() - 1);
                    nested = true;
                }
            }
            if (null != template) {
                template.startElement(tag, nested, attributes);
            }
        }
    }

    /**
     * Registers the given pattern for the current path and parses, if provided,
     * the type restrictions in <code>typeOf</code>.
     * 
     * @param currentPath the current path (to the specified elements)
     * @param typeOf the type restriction (may be multiple separated by commas, 
     *     may be one, may be empty, may be <b>null</b>)
     * @param template the annotation template
     * @throws QdParserException in case of any parsing/interpretation error
     * 
     * @since 1.00
     */
    private void parsePatterns(String currentPath, String typeOf, 
        AnnotationBuilder<?> template) throws QdParserException {
        if (null == typeOf) {
            registerPattern(currentPath, null, template);
        } else {
            StringTokenizer typeOfTokens = new StringTokenizer(typeOf, ",");
            while (typeOfTokens.hasMoreTokens()) {
                String tOf = typeOfTokens.nextToken();
                registerPattern(currentPath, tOf, template);
            }
        }
    }
    
    /**
     * Registers the given pattern (for the current path and if provided the 
     * type restriction).
     * 
     * @param currentPath the current path (to the specified elements)
     * @param typeOf a type restriction, i.e. the name of an interface or a 
     *     class or <b>null</b>
     * @param template the annotation template
     * @throws QdParserException in case of any parsing/interpretation error
     * 
     * @since 1.00
     */
    private void registerPattern(String currentPath, String typeOf, 
        AnnotationBuilder<?> template) throws QdParserException {
        Pattern pat = new Pattern(currentPath, typeOf);
        patterns.add(pat);
        pat.register(template.getInstanceClass(), template);
        Configuration.LOG.config("registered: pattern " 
            + typeOf + " annotation: " 
            + template.getInstanceClass().getName());
    }

    /**
     * Ends reading an element. Cleans up the stack and delegates to the 
     * annotation builder instance.
     * 
     * @param tag the element name
     * 
     * @throws QdParserException in case of reading errors
     */
    @Override
    public void endElement(String tag) throws QdParserException {        
        AnnotationBuilder<?> template = Annotations.getTemplate(tag);
        boolean nested = null == template && templateStack.size() > 0;
        if (null != template) {
            templateStack.remove(templateStack.size() - 1);
        } else {
            if (!templateStack.isEmpty()) {
                template = templateStack.get(templateStack.size() - 1);
            }
        }
        if (null != template) {
            template.endElement(tag, nested);
        }
        modifyPath(tag, null);
    }
    
    /**
     * Receive notification of the end of the document. Processes 
     * {@link #unresolvedConfigurations}.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end
     * of a document (such as finalising a tree or closing an output
     * file).</p>
     *
     * @throws QdParserException in case of reading errors
     */
    @Override
    //public void endDocument() throws SAXException
    public void endDocument() throws QdParserException {
        for (HashMap.Entry<String, String> entry 
            : unresolvedConfigurations.entries()) {
            String referring = entry.getKey();
            String referenced = entry.getValue();
            MonitoringGroupConfiguration configuration 
                = groupConfigurations.get(referenced);
            if (null == configuration) {
                Configuration.LOG.config("group configuration '" + referenced 
                    + "' referenced from '" + referring + "' does not exist. " 
                    + "Ignoring.");               
            } else {
                groupConfigurations.put(referring, configuration);
            }
        }
        unresolvedConfigurations.clear();
    }
    
    /**
     * Text node content.
     *
     * @param str node content
     * @throws QdParserException parse error
     */
    @Override
    public void text(String str) throws QdParserException {
    }
 
    /**
     * Start of an XML Document.
     *
     * @throws QdParserException parse error
     */
    public void startDocument() throws QdParserException {
    }
    
}