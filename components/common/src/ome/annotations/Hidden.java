/*
 * ome.annotations.Hideen
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Java imports

// Third-party libraries

// Application-internal dependencies

/**
 * annotation which specifies that a method parameter (e.g. a password) must be
 * hidden from logging output.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision: 1167 $, $Date: 2006-12-15 10:39:34 +0000 (Fri, 15 Dec 2006) $
 * @since 3.0-M3
 * @see <a href="https://trac.openmicroscopy.org.uk/omero/ticket/209">ticket:209</a>
 */
@RevisionDate("$Date: 2006-12-15 10:39:34 +0000 (Fri, 15 Dec 2006) $")
@RevisionNumber("$Revision: 1167 $")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Hidden {
    // no fields
}
