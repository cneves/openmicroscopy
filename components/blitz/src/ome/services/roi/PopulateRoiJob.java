/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.roi;

import java.io.File;

import ome.services.scripts.ScriptUploader;
import ome.services.util.Executor;
import ome.system.Principal;

/**
 * Start-up task which guarantees that lib/python/populateroi.py is added as a
 * script to the server. Then, users like MetadataStoreI who would like to run
 * populateroi.py scripts, can use {@link #createJob()}

 * @since Beta4.1
 */
public class PopulateRoiJob extends ScriptUploader {

    private static File production() {
        File cwd = new File(".");
        File lib = new File(cwd, "lib");
        File py = new File(lib, "python");
        File populate = new File(py, "populateroi.py");
        return populate;
    }

    public PopulateRoiJob(String uuid, Executor executor) {
        super(uuid, executor, production());
    }

    public PopulateRoiJob(String uuid, Executor executor, File source) {
        super(new Principal(uuid, "system", "Internal"), executor, source);
    }

    public PopulateRoiJob(Principal principal, Executor executor, File source) {
        super(principal, executor, source);
    }
    
    @Override
    public String getName() {
        return "populateroi.py";
    }
}
