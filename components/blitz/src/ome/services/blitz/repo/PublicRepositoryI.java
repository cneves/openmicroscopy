/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.services.blitz.repo;

import static omero.rtypes.rlong;
import static omero.rtypes.rstring;

import java.util.List;

import ome.services.util.Executor;
import ome.system.Principal;
import ome.system.ServiceFactory;
import omero.ServerError;
import omero.ValidationException;
import omero.api.RawFileStorePrx;
import omero.api.RawPixelsStorePrx;
import omero.api.RenderingEnginePrx;
import omero.api.ThumbnailStorePrx;
import omero.grid.RepositoryPrx;
import omero.grid._RepositoryDisp;
import omero.model.Format;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.util.IceMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import Ice.Current;

/**
 * 
 * @since Beta4.1
 */
public class PublicRepositoryI extends _RepositoryDisp {

    private final static Log log = LogFactory.getLog(PublicRepositoryI.class);

    private final long id;

    private final Executor executor;

    private final Principal principal;

    public PublicRepositoryI(long repoObjectId, Executor executor,
            Principal principal) throws Exception {
        this.id = repoObjectId;
        this.executor = executor;
        this.principal = principal;
    }

    public OriginalFile root(Current __current) throws ServerError {
        return new OriginalFileI(this.id, false); // SHOULD BE LOADED.
    }

    public OriginalFile register(String path, Format fmt, Current __current)
            throws ServerError {

        if (path == null || fmt == null
                || (fmt.getId() == null && fmt.getValue() == null)) {
            throw new ValidationException(null, null,
                    "path and fmt are required arguments");
        }

        OriginalFile file = new OriginalFileI();
        file.setPath(rstring(path));
        file.setFormat(fmt);

        // Overwrites
        omero.RTime creation = omero.rtypes.rtime(System.currentTimeMillis());
        file.setCtime(creation);
        file.setAtime(creation);
        file.setMtime(creation);
        file.setSha1(rstring("UNKNOWN"));
        file.setName(rstring(path));
        file.setPath(rstring(path)); // NEED SOME CHECKING HERE.
        file.setSize(rlong(0));

        IceMapper mapper = new IceMapper();
        final ome.model.core.OriginalFile f = (ome.model.core.OriginalFile) mapper
                .reverse(file);
        Long id = (Long) executor.execute(principal, new Executor.SimpleWork(
                this, "saveNewOriginalFile") {
            @Transactional(readOnly = false)
            public Object doWork(Session session, ServiceFactory sf) {
                return sf.getUpdateService().saveAndReturnObject(f).getId();
            }
        });

        file.setId(rlong(id));
        file.unload();
        return file;

    }

    public void delete(String path, Current __current) throws ServerError {
        // TODO Auto-generated method stub

    }

    public List<String> list(String path, Current __current) throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> listDirs(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> listFiles(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public OriginalFile load(String path, Current __current) throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public RawPixelsStorePrx pixels(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public RawFileStorePrx read(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public void rename(String path, Current __current) throws ServerError {
        // TODO Auto-generated method stub

    }

    public RenderingEnginePrx render(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public ThumbnailStorePrx thumbs(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public void transfer(String srcPath, RepositoryPrx target,
            String targetPath, Current __current) throws ServerError {
        // TODO Auto-generated method stub

    }

    public RawFileStorePrx write(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public List<OriginalFile> listKnown(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public List<OriginalFile> listKnownDirs(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

    public List<OriginalFile> listKnownFiles(String path, Current __current)
            throws ServerError {
        // TODO Auto-generated method stub
        return null;
    }

}
