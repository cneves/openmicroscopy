/*
 *   $Id$
 *
 *   Copyright 2011 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.chgrp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ome.model.IObject;
import ome.services.graphs.GraphEntry;
import ome.services.graphs.GraphException;
import ome.services.graphs.GraphOpts;
import ome.services.graphs.GraphSpec;
import ome.services.graphs.GraphStep;
import ome.services.messages.EventLogMessage;
import ome.services.sharing.ShareBean;
import ome.system.OmeroContext;
import ome.system.Roles;
import ome.tools.hibernate.ExtendedMetadata;
import ome.tools.hibernate.QueryBuilder;
import ome.tools.spring.InternalServiceFactory;
import ome.util.SqlAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Post-processing action produced by {@link ChgrpStepFactory},
 * one for each {@link ChgrpStep} in order to check group permission
 * constraints.
 *
 * @author Josh Moore, josh at glencoesoftware.com
 * @since Beta4.3.2
 * @see ticket:6422
 */
public class ChgrpValidation extends GraphStep {

    final private static Log log = LogFactory.getLog(ChgrpValidation.class);

    final private OmeroContext ctx;

    final private ExtendedMetadata em;

    final private long userGroup;

    final private long grp;

    final private ShareBean share;

    public ChgrpValidation(OmeroContext ctx, ExtendedMetadata em, Roles roles,
            int idx, List<GraphStep> stack,
            GraphSpec spec, GraphEntry entry, long[] ids, long grp) {
        super(idx, stack, spec, entry, ids);
        this.ctx = ctx;
        this.em = em;
        this.grp = grp;
        this.userGroup = roles.getUserGroupId();
        this.share = (ShareBean) new InternalServiceFactory(ctx).getShareService();
    }

    @Override
    public void action(Callback cb, Session session, SqlAction sql, GraphOpts opts)
    throws GraphException {

        // ticket:6422 - validation of graph, phase 2
        // =====================================================================
        final String[][] locks = em.getLockCandidateChecks(iObjectType, true);

        int total = 0;
        for (String[] lock : locks) {
            Long bad = findImproperOutgoingLinks(session, lock);
            if (bad != null && bad > 0) {
                log.warn(String.format("%s:%s improperly links to %s.%s: %s",
                        iObjectType.getSimpleName(), id, lock[0], lock[1],
                        bad));
                total += bad;
            }
        }
        if (total > 0) {
            throw new GraphException(String.format("%s:%s improperly linked by %s objects",
                    iObjectType.getSimpleName(), id, total));
        }

    }

    private Long findImproperOutgoingLinks(Session session, String[] lock) {
        Long old = share.setShareId(-1L);
        share.resetReadFilter(session);
        try {

            String str = String.format(
                    "select count(*) from %s target, %s source " +
                    "where target.id = source.%s.id and source.id = ? " +
                    "and not (target.details.group.id = ? " +
                    "  or target.details.group.id = ?)",
                    lock[0], iObjectType.getName(), lock[1]);

            Query q = session.createQuery(str);
            q.setLong(0, id);
            q.setLong(1, grp);
            q.setLong(2, userGroup);

            return (Long) q.list().get(0);

        } finally {
            share.setShareId(old);
        }
    }

    @Override
    public void onRelease(Class<IObject> k, Set<Long> ids)
            throws GraphException {
        EventLogMessage elm = new EventLogMessage(this, "CHGRP-VALIDATION", k,
                new ArrayList<Long>(ids));

        try {
            ctx.publishMessage(elm);
        } catch (Throwable t) {
            GraphException de = new GraphException("EventLogMessage failed.");
            de.initCause(t);
            throw de;
        }

    }

}
