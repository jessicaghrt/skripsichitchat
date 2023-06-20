/*
 * @(#)CommunityDetectionReport.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package report;

import java.util.*;

import core.*;
import routing.*;
import routing.community.PeopleRankEngine;
import routing.community.PeopleRankRoutingDE;

/**
 * <p>
 * Reports the local communities at each node whenever the done() method is
 * called. Only those nodes whose router is a DecisionEngineRouter and whose
 * RoutingDecisionEngine implements the
 * routing.community.CommunityDetectionEngine are reported. In this way, the
 * report is able to output the result of any of the community detection
 * algorithms.</p>
 *
 * @author PJ Dillon, University of Pittsburgh
 */
public class PeopleRankReport extends Report {

    public PeopleRankReport() {
        init();
    }

    @Override
    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (DTNHost h : nodes) {
            MessageRouter mr = h.getRouter();
            DecisionEngineRouter de = (DecisionEngineRouter) mr;
            PeopleRankRoutingDE per = (PeopleRankRoutingDE) de.getDecisionEngine();
            PeopleRankEngine pre = (PeopleRankEngine) per;
            
            double rank = pre.getThisRank();
            
            String print = h.toString() + ": "+rank;
            write(print);
        }
        super.done();
    }

}
