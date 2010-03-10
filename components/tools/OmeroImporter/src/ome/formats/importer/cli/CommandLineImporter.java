package ome.formats.importer.cli;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import loci.formats.meta.MetadataStore;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import omero.model.Dataset;
import omero.model.Screen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The base entry point for the CLI version of the OMERO importer.
 * 
 * @author Chris Allan <callan@glencoesoftware.com>
 * @author Josh Moore josh at glencoesoftware.com
 */
public class CommandLineImporter {
    /** Logger for this class. */
    private static Log log = LogFactory.getLog(CommandLineImporter.class);

    /** Name that will be used for usage() */
    private static final String APP_NAME = "importer-cli";

    /** Configuration used by all components */
    public final ImportConfig config;

    /** Base importer library, this is what we actually use to import. */
    public final ImportLibrary library;

    /** ErrorHandler which is also responsible for uploading files */
    public final ErrorHandler handler;

    /** Bio-Formats reader wrapper customized for OMERO. */
    private final OMEROWrapper reader;

    /** Bio-Formats {@link MetadataStore} implementation for OMERO. */
    private final OMEROMetadataStoreClient store;

    /** Candidates for import */
    private final ImportCandidates candidates;

    /** If true, then only a report on used files will be produced */
    private final boolean getUsedFiles;

    /**
     * Main entry class for the application.
     */
    public CommandLineImporter(final ImportConfig config, String[] paths,
            boolean getUsedFiles) throws Exception {
        this.config = config;
        config.loadAll();

        this.getUsedFiles = getUsedFiles;
        this.reader = new OMEROWrapper(config);
        this.handler = new ErrorHandler(config);
        candidates = new ImportCandidates(reader, paths, handler);
        this.reader.setMetadataCollected(true);

        if (paths == null || paths.length == 0 || getUsedFiles) {

            store = null;
            library = null;

        } else {

            // Ensure that we have all of our required login arguments
            if (!config.canLogin()) {
                // config.requestFromUser(); // stdin if anything missing.
                usage(); // EXITS TODO this should check for a "quiet" flag
            }
                       
            store = config.createStore();
            library = new ImportLibrary(store, reader);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    config.saveAll();
                } catch (Exception e) {
                    log.error("Error during config.saveAll", e);
                }
                cleanup();
            }
        });
    }

    public int start() {

        if (getUsedFiles) {
            try {
                candidates.print();
                report();
                return 0;
            } catch (Throwable t) {
                log.error("Error retrieving used files.", t);
                return 1;
            }
        }

        else if (candidates.size() < 1) {
            if (handler.errorCount() > 0) {
                System.err.println("No imports due to errors!");
                report();
            } else {
                System.err.println("No imports found");
                usage();
            }
        }

        else {       	
            library.addObserver(new LoggingImportMonitor());
            library.addObserver(new ErrorHandler(config));
            library.importCandidates(config, candidates);
            report();
        }

        return 0;

    }
    
    void report() {
        boolean report = config.sendReport.get();
        boolean files = config.sendFiles.get();
        if (report) {
           handler.update(null, new ImportEvent.DEBUG_SEND(files));
        }
    }

    /**
     * Cleans up after a successful or unsuccessful image import. This method
     * only does the minimum required cleanup, so that it can be called
     * during shutdown.
     */
    public void cleanup() {        
        if (store != null) {
            store.logout();
        }
    }

    /**
     * Prints usage to STDERR and exits with return code 1.
     */
    public static void usage() {
        System.err
                .println(String
                        .format(
                                "\n"
                                        + " Usage:  %s [OPTION]... [DIR|FILE]... \n"
                                        + "   or:   %s [OPTION]... - \n"
                                        + "\n"
                                        + "Import any number of files into an OMERO instance.\n"
                                        + "If \"-\" is the only path, a list of files or directories \n"
                                        + "is read from standard in. Directories will be searched for \n"
                                        + "all valid imports.\n"
                                        + "\n"
                                        + "Mandatory arguments:\n"
                                        + "  -s\tOMERO server hostname\n"
                                        + "  -u\tOMERO experimenter name (username)\n"
                                        + "  -w\tOMERO experimenter password\n"
                                        + "  -k\tOMERO session key (can be used in place of -u and -w)\n"
                                        + "  -f\tDisplay the used files (does not require other mandatory arguments)\n"
                                        + "\n"
                                        + "Optional arguments:\n"
                                        + "  -c\tContinue importing after errors\n"
                                        + "  -l\tUse the list of readers rather than the default\n"
                                        + "  -d\tOMERO dataset Id to import image into\n"
                                        + "  -r\tOMERO screen Id to import plate into\n"
                                        + "  -n\tImage name to use\n"
                                        + "  -x\tImage description to use\n"
                                        + "  -p\tOMERO server port [defaults to 4063]\n"
                                        + "  -h\tDisplay this help and exit\n"
                                        + "\n"
                                        + "  --debug[=0|1|2]\tTurn debug logging on (optional level)\n"
                                        + "  --report\tReport errors to the OME team\n"
                                        + "  --upload\tUpload broken files with report\n"
                                        + "  --email=...\tEmail for reported errors\n "
                                        + "\n"
                                        + "ex. %s -s localhost -u bart -w simpson -d 50 foo.tiff\n"
                                        + "\n"
                                        + "Report bugs to <ome-users@openmicroscopy.org.uk>",
                                APP_NAME, APP_NAME, APP_NAME));
        System.exit(1);
    }

    /**
     * Command line application entry point which parses CLI arguments and
     * passes them into the importer. Return codes for import are:
     * <ul>
     * <li>0 on success</li>
     * <li>1 on argument parsing failure</li>
     * <li>2 on exception during import</li>
     * </ul>
     *
     * Return codes for the "-f" option (getUsedFiles) are:
     * <ul>
     * <li>0 on success, even if errors exist in the files</li>
     * <li>1 only if an exception propagates up the stack</li>
     * </ul>
     * @param args
     *            Command line arguments.
     */
    public static void main(String[] args) {

        ImportConfig config = new ImportConfig();

        // Defaults
        config.cliEmail.set("");
        config.sendFiles.set(false);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);

        LongOpt debug = new LongOpt("debug", LongOpt.OPTIONAL_ARGUMENT, null, 1);
        LongOpt report = new LongOpt("report", LongOpt.NO_ARGUMENT, null, 2);
        LongOpt upload = new LongOpt("upload", LongOpt.NO_ARGUMENT, null, 3);
        LongOpt email = new LongOpt("email", LongOpt.REQUIRED_ARGUMENT, null, 4);
        Getopt g = new Getopt(APP_NAME, args, "cfl:s:u:w:d:r:k:x:n:p:h",
                new LongOpt[] { debug, report, upload, email });
        int a;

        boolean getUsedFiles = false;

        while ((a = g.getopt()) != -1) {
            switch (a) {
            case 1: {
                String s = g.getOptarg();
                Integer level = 0;
                try {
                    level = Integer.valueOf(s);
                } catch (Exception e) {
                    // ok
                }
                config.configureDebug(level);
                break;
            }
            case 2: {
                config.sendReport.set(true);
                break;
            }
            case 3: {
                config.sendFiles.set(true);
                break;
            }
            case 4: {
                config.cliEmail.set(g.getOptarg());
                break;
            }
            case 's': {
                config.hostname.set(g.getOptarg());
                break;
            }
            case 'u': {
                config.username.set(g.getOptarg());
                break;
            }
            case 'w': {
                config.password.set(g.getOptarg());
                break;
            }
            case 'k': {
                config.sessionKey.set(g.getOptarg());
                break;
            }
            case 'p': {
                config.port.set(Integer.parseInt(g.getOptarg()));
                break;
            }
            case 'd': {
                config.targetClass.set(Dataset.class.getName());
                config.targetId.set(Long.parseLong(g.getOptarg()));
                break;
            }
            case 'r': {
                config.targetClass.set(Screen.class.getName());
                config.targetId.set(Long.parseLong(g.getOptarg()));
                break;
            }
            case 'n': {
                config.name.set(g.getOptarg());
                break;
            }
            case 'x': {
                config.description.set(g.getOptarg());
                break;
            }
            case 'f': {
                getUsedFiles = true;
                break;
            }
            case 'c': {
                config.contOnError.set(true);
                break;
            }
            case 'l': {
                config.readersPath.set(g.getOptarg());
                break;
            }
            case 'h': {
                usage(); // exits
            }
            default: {
                usage(); // exits
            }
            }
        }

        // Start the importer and import the image we've been given
        String[] rest = new String[args.length - g.getOptind()];
        System.arraycopy(args, g.getOptind(), rest, 0, args.length
                - g.getOptind());

        CommandLineImporter c = null;
        int rc = 0;
        try {

            if (rest.length == 1 && "-".equals(rest[0])) {
                rest = stdin();
            }

            c = new CommandLineImporter(config, rest, getUsedFiles);
            rc = c.start();
        } catch (Throwable t) {
            log.error("Error during import process.", t);
            rc = 2;
        } finally {
            if (c != null) {
                c.cleanup();
            }
        }
        System.exit(rc);
    }

    /**
     * Reads a list of paths from stdin.
     * @return
     */
    static String[] stdin() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        List<String> files = new ArrayList<String>();
        while (true) {
            String str = in.readLine();
            if (str == null) {
                break;
            } else {
                str = str.trim();
                if (str.length() > 0) {
                    files.add(str);
                }
            }
        }
        return files.toArray(new String[0]);
    }

}
