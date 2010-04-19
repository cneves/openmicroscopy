/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.formats.importer;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import loci.common.LogTools;
import loci.formats.FormatTools;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.util.IniFileLoader;
import ome.system.UpgradeCheck;
import omero.model.Annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Utility class which configures the Import.
 * 
 * @since Beta4.1
 */
public class ImportConfig {

    private final static Log log = LogFactory.getLog(ImportConfig.class);

    /**
     * Delimiter used to encode multiple servers in one preferences value.
     */
    public final static String SERVER_NAME_SEPARATOR = ",";

    /**
     * Lookup key for {@link System#getProperty(String)}. Should be the path of
     * a readers.txt file.
     */
    public final static String READERS_KEY = "omero.import.readers";

    //
    // CONFIGURATION SOURCES: several configuration sources are defined below.
    // Each may or may not be used for a given {@link value}.
    //

    /**
     * Preferences node which will be used for all Preferences in the
     * ome.formats package. This must work in tandem with other sources such as
     * {@link IniFileLoader}
     */
    private final Preferences prefs;

    /**
     * Ini-file based configuration source which loads both a static
     * configuration file and a user-defined configuration file.
     */
    private final IniFileLoader ini;

    /**
     * {@link Properties} instance which will also be used for lookups. In the
     * default case, this is from {@link System#getProperties()}
     */
    private final Properties props;

    /**
     * Stores the omeroVersion from omero.properties
     */
    private String omeroVersion = "Unknown";    
    
    //
    // MUTABLE STATE : To prevent every class from having it's own
    // username/password/port/etc field, all are available here. On save, these
    // are committed to disk.
    //

    public final StrValue hostname;
    public final StrValue username;
    public final StrValue password;
    public final IntValue port;
    public final LongValue savedProject;
    public final LongValue savedDataset;
    public final LongValue savedScreen;

    public final StrValue sessionKey;
    public final StrValue email;
    public final StrValue cliEmail;
    public final StrValue serverList;
    public final StrValue name;
    public final StrValue description;
    public final StrValue targetClass;
    public final LongValue targetId;

    public final BoolValue debug;
    public final BoolValue contOnError;
    public final BoolValue sendReport;
    public final BoolValue sendFiles;

    public final BoolValue useFullPath;
    public final BoolValue savedFileNaming;
    public final BoolValue overrideImageName;
    public final IntValue numOfDirectories;

    public final FileValue savedDirectory;
    public final StrValue readersPath;
    
    public final AnnotationListValue annotations;

    /**
     * Static method for creating {@link Preferences} during construction if
     * necessary.
     */
    private static Preferences prefs() {
        Preferences prefs = Preferences.userNodeForPackage(ImportConfig.class);
        try {
            prefs.flush();
        } catch (Exception e) {
            log.error("Error flushing preferences");
        }
        return prefs;
    }

    /**
     * Simplest constructor which use calls
     * {@link ImportConfig#ImportConfig(File)} with null.
     */
    public ImportConfig() {
        this(null);
    }

    /**
     * Calls
     * {@link ImportConfig#ImportConfig(Preferences, PreferenceContext, IniFileLoader, Properties)}
     * with user preferences, a local {@link PreferenceContext}, an
     * {@link IniFileLoader} initialized with the given argument, and
     * {@link System#getProperties()}.
     * 
     * @param configFile
     *            Can be null.
     */
    public ImportConfig(final File configFile) {
        this(prefs(), new IniFileLoader(configFile),
                System.getProperties());
    }

    /**
     * Complete constructor. All values can be null.
     * 
     * @param prefs
     * @param ctx
     * @param ini
     * @param props
     */
    public ImportConfig(final Preferences prefs,
            IniFileLoader ini, Properties props) {

        this.prefs = prefs;
        this.props = props;
        this.ini = ini;

        // Various startup requirements
        isUpgradeRequired();
        if (ini != null) {
            ini.updateFlexReaderServerMaps();
        }

        log.info(String.format("Bioformats version: %s revision: %s date: %s",
             FormatTools.VERSION, FormatTools.SVN_REVISION, FormatTools.DATE));

        hostname     = new StrValue("hostname", this, "omero.host");
        username     = new StrValue("username", this, "omero.name");
        password     = new StrValue("password", this, "omero.pass");
        port         = new IntValue("port", this, 4063, "omero.port") {
            @Override
            public synchronized void load() {
                super.load();
                // Handle previous versions in which a null/"" got stored
                // to preferences.
                if (_current.compareAndSet(null, _default)) {
                    log.debug("Replacing port load value with default");
                }
            }
        };
        
        sessionKey   = new StrValue("session", this);
        email        = new StrValue("email", this);
        cliEmail     = new StrValue("cliEmail", this);
        serverList   = new StrValue("serverList", this);
        name         = new StrValue("imageName", this);
        description  = new StrValue("imageDescription", this);
        targetClass  = new StrValue("targetClass", this);
        targetId     = new LongValue("targetId", this, 0);

        savedProject = new LongValue("savedProject", this, 0L);
        savedDataset = new LongValue("savedDataset", this, 0L);
        savedScreen  = new LongValue("savedScreen", this, 0L);

        debug        = new BoolValue("debug", this, false);
        contOnError  = new BoolValue("contOnError", this, false);
        sendReport   = new BoolValue("sendFiles", this, false);
        sendFiles    = new BoolValue("sendFiles", this, false);

        useFullPath  = new BoolValue("useFullPath", this, true);
        savedFileNaming = new BoolValue("savedFileNaming", this, true);
        overrideImageName = new BoolValue("overrideImageName", this, true);
        numOfDirectories = new IntValue("numOfDirectories", this, 0);
        savedDirectory = new FileValue("savedDirectory", this);
        
        annotations = new AnnotationListValue(
        		"annotations", this, new ArrayList<Annotation>());

        readersPath = new StrValue("readersPath", this) {
            @Override
            public String get() {

                if (super.get() == null) {
                    set(System.getProperty(READERS_KEY));
                }

                if (super.get() == null) {

                    if (ini != null) {
                        String readersFile = ini.getUserSettingsDirectory()
                                + File.separator + "importer_readers.txt";
                        File rFile = new File(readersFile);
                        if (rFile.exists()) {
                            set(rFile.getAbsolutePath());
                        }
                    }

                    if (super.get() == null) {
                        set("importer_readers.txt");
                    }
                }
                return super.get();
            }
        };

    }

    /**
     * Check online to see if this is the current version
     */
    public boolean isUpgradeRequired() {
        ResourceBundle bundle = ResourceBundle.getBundle("omero");
        omeroVersion = bundle.getString("omero.version");
        log.info("OMERO Version: " + omeroVersion);
        String url = bundle.getString("omero.upgrades.url");
        UpgradeCheck check = new UpgradeCheck(url, omeroVersion, "importer");
        check.run();
        return check.isUpgradeNeeded();
    }
    
    /**
     * Modifes the Log4j logging level of everything under the ome.format
     * package hierarchically. Also enables Loci debugging.
     * 
     * @param level if null, then {@link #ini#getDebugLevel()} will be used.
     */
    public void configureDebug(Integer level) {
        
        if (level == null) {
            level = Integer.valueOf(ini.getDebugLevel()); 
        }
        
        if (level.intValue() < 0) {
            return;
        }
        
        log.info("Debugging at level " + level);
        debug.set(true);        
        
        Logger l = Logger.getLogger("ome.formats");
        l.setLevel(Level.DEBUG);
        
        // loci.* prints so much, that at 0 we're ignoring it
        // beyond that, we set loci's level to level - 1
        if (level.intValue() > 0) {
            final Logger loci = Logger.getLogger("loci.common.Log");
            LogTools.setDebug(true);
            loci.setLevel(Level.DEBUG);
    
            LogTools.setDebug(true);
            LogTools.setDebugLevel(level-1);
            LogTools.setLog(new loci.common.Log() {
                @Override
                public void print(String x) {
                    loci.debug(x);
                }
                @Override
                public void flush() {
                    // noop
                }
            } );
        }


    }
    
    //
    // Login methods
    //

    public OMEROMetadataStoreClient createStore() throws Exception {
        if (!canLogin()) {
            throw new RuntimeException("Can't create store. See canLogin()");
        }
        OMEROMetadataStoreClient client = new OMEROMetadataStoreClient();
        if (sessionKey.empty()) {
            client.initialize(username.get(), password.get(), hostname.get(),
                    port.get());

        } else {
            client.initialize(hostname.get(), port.get(), sessionKey.get());
        }
        return client;
    }

    public boolean canLogin() {
        if (((username.empty() || password.empty())
                && sessionKey.empty()) || hostname.empty()) {
            return false;
        }
        return true;
    }

    //
    // GUI related. Delegates to IniFileLoader
    //

    public String getLogFile() {
        return ini.getLogFile();
    }

    public String getHomeUrl() {
        return ini.getHomeUrl();
    }

    public String getAppTitle() {
        return ini.getAppTitle();
    }

    public String getVersionNumber() {
        return this.omeroVersion + " " + ini.getVersionNote();
    }

    public String getIniVersionNumber() {
    	return ini.getVersionNumber();
    }
    
    public String getUserSettingsDirectory() {
        return ini.getUserSettingsDirectory();
    }

    public boolean getUseQuaqua() {
        return ini.getUseQuaqua();
    }

    public void setUseQuaqua(boolean b) {
        ini.setUseQuaqua(b);
    }

    public Rectangle getUIBounds() {
        return ini.getUIBounds();
    }

    public void setUIBounds(Rectangle bounds) {
        ini.setUIBounds(bounds);
    }

    public String getFeedbackUrl() {
        return ini.getUploaderURL();
    }

    public String getTokenUrl() {
        return ini.getUploaderTokenURL();
    }

    public String getUploaderUrl() {
        return ini.getUploaderURL();
    }

    //
    // Server list
    //

    public List<String> getServerList() {
        if (serverList.empty() || serverList.get().trim().length() == 0) {
            return null;
        } else {
            List<String> list = new ArrayList<String>();
            String[] l = serverList.get().split(SERVER_NAME_SEPARATOR, 0);
            if (l == null || l.length == 0) {
                return null;
            } else {
                if (list != null)
                    list.clear();
                for (int index = 0; index < l.length; index++) {
                    if (list != null)
                        list.add(l[index].trim());
                }
            }
            return list;
        }
    }

    /**
     * Save the current serverList if the currentServer is not on the list. Make
     * sure that the server is a valid string and does not represent fake input
     * text like "--> Enter server"
     */
    public void updateServerList(String currentServer) {

        List<String> l = getServerList();
        if (l != null && l.contains(currentServer)) {
            return;
        }

        if (serverList.empty() || serverList.get().length() == 0) {
            serverList.set(currentServer.trim());
        } else {
            serverList.set(serverList + SERVER_NAME_SEPARATOR + currentServer);
        }
    }

    public void removeServer(String server) {
        List<String> l = getServerList();
        if (l == null)
            return;
        l.remove(server);
        Iterator<String> i = l.iterator();
        String list = "";
        int n = l.size() - 1;
        int index = 0;
        while (i.hasNext()) {
            list += (String) i.next();
            if (index != n)
                list += SERVER_NAME_SEPARATOR;
            index++;
        }
        serverList.set(list);
    }

    //
    // HELPERS
    //

    protected void prompt(Value value, String prompt, boolean hide) {

        String v = value.toString();
        if (hide) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < v.length(); i++) {
                sb.append("*");
            }
            v = sb.toString();
        }
        System.out.print(String.format("%s[%s]:", prompt, v));
        String input;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                input = br.readLine();
                if (input == null || input.trim().equals("")) {
                    continue;
                }
                value.set(value.fromString(input));
            } catch (IOException e) {
                continue;
            }
        }
    }

    public void requestFromUser() {
        if (!canLogin()) {
            loadAll();
            prompt(hostname, " Enter server name: ", false);
            prompt(username, " Enter user name: ", false);
            prompt(password, " Enter password: ", true);
        }
    }

    protected List<Value<?>> values() {
        List<Value<?>> rv = new ArrayList<Value<?>>();
        for (Field f : getClass().getFields()) {
            try {
                Object o = f.get(this);
                if (o instanceof Value) {
                    Value<?> cv = (Value<?>) o;
                    rv.add(cv);
                }
            } catch (Exception e) {
                log.debug("Error during field lookup: " + e);
            }
        }
        return rv;
    }

    public Map<String, String> map() {
        Map<String, String> rv = new HashMap<String, String>();
        for (Value<?> cv : values()) {
            rv.put(cv.key, cv.toString());
        }
        return rv;
    }

    
    /**
     * Loads gui specific values for which it makes sense to have a preferences values.
     *
     * @see #saveAll()
     */
    public void loadGui() {
         email.load();
    }

     /**
      * Saves gui specific values for which it makes sense to have a preferences values.
      *
      * @see #saveAll()
      */
     public void saveGui() {
          email.store();
     }
     
    /**
     * Loads all the values for which it makes sense to have a preferences values.
     *
     * @see #saveAll()
     */
    public void loadAll() {

        // Moving to expliti calls.
        // for (Value<?> cv : values()) {
        //    cv.load();
        // }
        savedProject.load();
        savedDataset.load();
        savedScreen.load();

        useFullPath.load();
        savedFileNaming.load();
        overrideImageName.load();
        numOfDirectories.load();
        savedDirectory.load();

        port.load();
    }

   
    /**
     * @see #loadAll()
     */
    public void saveAll() {

        // Moving to explicit calls
        // for (Value<?> cv : values()) {
        //    cv.store();
        // }

        savedProject.store();
        savedDataset.store();
        savedScreen.store();

        useFullPath.store();
        savedFileNaming.store();
        overrideImageName.store();
        numOfDirectories.store();
        savedDirectory.store();

        try {
            prefs.flush();
            ini.flushPreferences();
        } catch (BackingStoreException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Container which thread-safely makes a generic configuration value
     * available, without requiring getters and setters.
     * 
     * @param <T>
     */
    public static abstract class Value<T> {

        final AtomicReference<T> _current = new AtomicReference<T>();

        final String key, omeroKey;
        final Preferences prefs;
        final IniFileLoader ini;
        final Properties props;
        final T _default;

        /**
         * Records the load location
         */
        Object which = null;

        /**
         * Ctor taking an {@link ImportConfig} instance meaning that all the
         * context values are used.
         */
        Value(String key, ImportConfig config) {
            this(key, config, null, null);
        }

        Value(String key, ImportConfig config, T defValue) {
            this(key, config, defValue, null);
        }

        Value(String key, ImportConfig config, T defValue, String omeroKey) {
            this.key = key;
            this.omeroKey = omeroKey;
            this.ini = config.ini;
            this.prefs = config.prefs;
            this.props = config.props;
            _default = defValue;
            _current.set(null);
        }

        /**
         * Returns the generic type contained by this holder. This does not
         * touch the persistent stores, but only accesses the value in-memory.
         */
        public T get() {
            return _current.get();
        }

        /**
         * Sets the in-memory value, which will get persisted on
         * {@link #store()} when {@link ImportConfig#saveAll()} is called.
         */
        public void set(T t) {
            _current.set(t);
        }

        @Override
        public String toString() {
            T t = get();
            if (t == null) {
                return "";
            } else {
                return t.toString();
            }
        }

        /**
         * Stores the current value back to some medium. The decision of which
         * medium is based on the current value of {@link #which}. In each case,
         * the type-matching source is used <em>except</em> when the
         * {@link Properties} are used, since this is most likely not a
         * persistent store.
         */
        public synchronized void store() {
            if (which instanceof Properties || which instanceof Preferences) {
                prefs.put(key, toString());
                log.trace("Saved " + key + " to " + prefs);
            } else if (which instanceof IniFileLoader) {
                // FIXME ((IniFileLoader)which).set
                log.trace("Saved " + key + " to " + ini);
            } else if (which == null && prefs != null) { // Loaded from defaults
                prefs.put(key, toString());
                log.trace("Freshly saved " + key + " to " + prefs);
            } else {
                log.trace("WHICH:" + which); // Unknown state
            }

        }

        /**
         * Loads properties from various locations. In order, the
         * {@link Properties} argument, the {@link PreferenceContext}, the
         * {@link Preferences}, the {@link IniFileLoader}, and finally the
         * default value.
         */
        public synchronized void load() {
            if (empty() && props != null) {
                set(fromString(props.getProperty(key)));
                if (!empty()) {
                    which = props;
                    log.trace("Loaded " + key + " from " + props);
                    return;
                }
            }

            if (empty() && prefs != null) {
                set(fromString(prefs.get(key, "")));
                if (!empty()) {
                    which = prefs;
                    log.trace("Loaded " + key + " from " + prefs);
                    return;
                }
            }

            if (empty() && ini != null) {
                // set(fromString((ini.getProperty(key));
                log.trace("Loaded " + key + " from " + ini);
                // break; FIXME
            }

            if (empty()) {
                set(_default);
                log.trace("Loaded " + key + " from default");
                which = null;
            }
        }

        public boolean empty() {
            return get() == null;
        }

        protected abstract T fromString(String string);
    }

    public static class StrValue extends Value<String> {

        public StrValue(String key, ImportConfig config) {
            super(key, config);
        }

        public StrValue(String key, ImportConfig config, String defValue) {
            super(key, config, defValue);
        }

        public StrValue(String key, ImportConfig config, String defValue,
                String omeroKey) {
            super(key, config, defValue, omeroKey);
        }

        @Override
        protected String fromString(String arg0) {
            return arg0;
        }

        public boolean empty() {
            String s = get();
            return s == null || s.length() == 0;
        }
    }
    
    public static class AnnotationListValue extends Value<List<Annotation>> {
    	
    	public AnnotationListValue(String key, ImportConfig config, 
    			                   List<Annotation> defValue) {
    		super(key, config, defValue);
    	}

		@Override
		protected List<Annotation> fromString(String string) {
			throw new RuntimeException("Not implemented.");
		}
    }

    public static class PassValue extends StrValue {
        public PassValue(String key, ImportConfig config) {
            super(key, config);
        }

        @Override
        public synchronized void store() {
            log.trace("Skipping password storage");
        }
    }

    public static class BoolValue extends Value<Boolean> {

        public BoolValue(String key, ImportConfig config, boolean defValue) {
            super(key, config, defValue);
        }

        @Override
        protected Boolean fromString(String arg0) {
            if (arg0 == null) {
                return null;
            }
            return Boolean.parseBoolean(arg0);
        }
    }

    public static class IntValue extends Value<Integer> {
        public IntValue(String key, ImportConfig config, int defValue) {
            super(key, config, Integer.valueOf(defValue));
        }

        public IntValue(String key, ImportConfig config, int defValue,
                String omeroKey) {
            super(key, config, Integer.valueOf(defValue), omeroKey);
        }

        @Override
        protected Integer fromString(String arg0) {
            try {
                return Integer.valueOf(arg0);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }

    }

    public static class LongValue extends Value<Long> {
        public LongValue(String key, ImportConfig config, long defValue) {
            super(key, config, Long.valueOf(defValue));
        }

        @Override
        protected Long fromString(String arg0) {
            try {
                return Long.valueOf(arg0);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    public static class FileValue extends Value<File> {
        public FileValue(String key, ImportConfig config) {
            super(key, config);
        }

        @Override
        protected File fromString(String arg0) {
            if (arg0 == null) {
                return null;
            }
            return new File(arg0);
        }

        @Override
        public File get() {
            File f = super.get();
            if (f != null && f.exists()) {
                return f;
            } else {
                set(null);
                return null;
            }
        }
    }

}
