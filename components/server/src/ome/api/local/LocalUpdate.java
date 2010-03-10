/*
 * ome.api.local.LocalUpdate
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.api.local;

import ome.model.IObject;

/**
 * Provides local (internal) extensions for updating
 * 
 * @author <br>
 *         Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de"> josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Revision: 4964 $ $Date: 2009-09-14 19:01:34 +0100 (Mon, 14 Sep 2009) $)
 *          </small>
 * @since OMERO3.0
 */
public interface LocalUpdate extends ome.api.IUpdate {

    void flush();

}
