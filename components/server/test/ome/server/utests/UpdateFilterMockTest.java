/*
 * ome.server.utests.UpdateFilterMockTest
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests;

// Java imports
import java.util.Collection;
import java.util.Map;

// Third-party libraries
import org.testng.annotations.*;

// Application-internal dependencies
import ome.model.IObject;
import ome.model.internal.Details;

/**
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Rev: 1167 $ $Date: 2006-12-15 10:39:34 +0000 (Fri, 15 Dec 2006) $) </small>
 * @since Omero 2.0
 */
public class UpdateFilterMockTest extends AbstractLoginMockTest {

    // ~ NON GRAPHS (single elements)
    // =========================================================================

    @Test
    public void test_filter_null() throws Exception {
        filter.filter(null, (Object) null);
        filter.filter(null, (IObject) null);
        filter.filter(null, (Details) null);
        filter.filter(null, (Map) null);
        filter.filter(null, (Collection) null);
    }

    // ~ GRAPHS (multiple levels)
    // =========================================================================

}
