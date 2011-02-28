/*
 *   $Id: SecurityFilter.java 3447 2009-02-04 15:55:18Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.tools.hibernate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ome.conditions.InternalException;
import ome.model.internal.Details;
import ome.model.internal.Permissions;
import ome.model.internal.Permissions.Right;
import ome.model.internal.Permissions.Role;
import static ome.model.internal.Permissions.Role.*;
import static ome.model.internal.Permissions.Right.*;
import ome.security.basic.OmeroInterceptor;
import ome.system.Roles;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.orm.hibernate3.FilterDefinitionFactoryBean;

/**
 * overrides {@link FilterDefinitionFactoryBean} in order to construct our
 * security filter in code and not in XML. This allows us to make use of the
 * knowledge within {@link Permissions}
 * 
 * With the addition of shares in 4.0, it is necessary to remove the security
 * filter is a share is active and allow loading to throw the necessary
 * exceptions.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0
 * @see <a
 *      href="https://trac.openmicroscopy.org.uk/omero/ticket/117">ticket117</a>
 * @see <a
 *      href="https://trac.openmicroscopy.org.uk/omero/ticket/1154">ticket1154</a>
 */
public class SecurityFilter extends FilterDefinitionFactoryBean {

    static public final String is_share = "is_share";

    static public final String is_admin = "is_admin";

    static public final String current_user = "current_user";

    static public final String current_groups = "current_groups";

    static public final String leader_of_groups = "leader_of_groups";

    static public final String filterName = "securityFilter";

    static Map<String, String> parameterTypes() {
        Map<String, String> parameterTypes = new HashMap<String, String>();
        parameterTypes.put(is_share, "int");
        parameterTypes.put(is_admin, "int");
        parameterTypes.put(current_user, "long");
        parameterTypes.put(current_groups, "long");
        parameterTypes.put(leader_of_groups, "long");
        return parameterTypes;
    }

    /**
     * Query-fragment to be used to determine if all bits in a
     * permissions column are set.
     */
    private final String bitand;

    /**
     * default constructor which calls all the necessary setters for this
     * {@link FactoryBean}. Also constructs the {@link #defaultFilterCondition }
     * This query clause must be kept in sync with
     * {@link #passesFilter(Details, Long, Collection, Collection, boolean)}
     *
     * @see #passesFilter(Details, Long, Collection, Collection, boolean)
     * @see FilterDefinitionFactoryBean#setFilterName(String)
     * @see FilterDefinitionFactoryBean#setParameterTypes(Properties)
     * @see FilterDefinitionFactoryBean#setDefaultFilterCondition(String)
     */
    public SecurityFilter(String bitand) {
        this.bitand = bitand;
        // This can't be done statically because we need the securitySystem.
        // and bitand
        String defaultFilterCondition = String.format("\n( "
                + "\n 1 = :is_share OR \n 1 = :is_admin OR "
                + "\n (group_id in (:leader_of_groups)) OR "
                + "\n (owner_id = :current_user AND %s) OR " + // 1st arg U
                "\n (group_id in (:current_groups) AND %s) OR " + // 2nd arg G
                "\n (%s) " + // 3rd arg W
                "\n)\n", isGranted(USER, READ), isGranted(GROUP, READ),
                isGranted(WORLD, READ));

        this.setFilterName(filterName);
        this.setParameterTypes(parameterTypes());
        this.setDefaultFilterCondition(defaultFilterCondition);
    }

    /**
     * tests that the {@link Details} argument passes the security test that
     * this filter defines. The two must be kept in sync. This will be used
     * mostly by the
     * {@link OmeroInterceptor#onLoad(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])}
     * method.
     * 
     * @param d
     *            Details instance. If null (or if its {@link Permissions} are
     *            null all {@link Right rights} will be assumed.
     * @return true if the object to which this
     */
    public static boolean passesFilter(Details d, Long currentUserId,
            Collection<Long> memberOfGroups, Collection<Long> leaderOfGroups,
            boolean admin, boolean share) {
        if (d == null || d.getPermissions() == null) {
            throw new InternalException("Details/Permissions null! "
                    + "Security system failure -- refusing to continue. "
                    + "The Permissions should be set to a default value.");
        }

        Permissions p = d.getPermissions();

        Long o = d.getOwner().getId();
        Long g = d.getGroup().getId();

        // most likely and fastest first
        if (p.isGranted(WORLD, READ)) {
            return true;
        }

        if (currentUserId.equals(o) && p.isGranted(USER, READ)) {
            return true;
        }

        if (memberOfGroups.contains(g)
                && d.getPermissions().isGranted(GROUP, READ)) {
            return true;
        }

        if (admin) {
            return true;
        }

        if (share) {
            return true;
        }

        if (leaderOfGroups.contains(g)) {
            return true;
        }

        return false;
    }

    // ~ Helpers
    // =========================================================================

    protected String isGranted(Role role, Right right) {
        String bit = "" + Permissions.bit(role, right);
        String isGranted = String
                .format(bitand, bit, bit);
        return isGranted;
    }

}
