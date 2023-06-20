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
import routing.community.ChitChat;
import routing.community.ChitChatEpidemic;
import routing.community.TransientEngine;

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
public class TranscientReport extends Report {

    private List<DTNHost> node;

    public TranscientReport() {
        init();
    }

    public void init() {
        super.init();
        this.node = new ArrayList<>();

        Random rng = new Random();
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (int i = 0; i < 3; i++) {
            DTNHost random = nodes.get(rng.nextInt(nodes.size()));
            if (!this.node.contains(random)) {
                this.node.add(random);
                System.out.println(random);
            }
        }
    }

    @Override
    public void done() {
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        for (DTNHost h : hosts) {
            MessageRouter mr = h.getRouter();
            DecisionEngineRouter de = (DecisionEngineRouter) mr;
            ChitChat dbr = (ChitChat) de.getDecisionEngine();
//                ChitChatEpidemic dbr = (ChitChatEpidemic) de.getDecisionEngine();
            TransientEngine hd = (TransientEngine) dbr;

            List<Tuple<DTNHost, Tuple<Map<String, Double>, Map<String, Double>>>> thisTrs = hd.getTrs();

            if (node.contains(h)) {
                String report;

                report = h + "\n";

                for (Tuple<DTNHost, Tuple<Map<String, Double>, Map<String, Double>>> history : thisTrs) {
                    report += history.getKey() + " --> " + history.getValue() + "\n";
                }

                write(report);
            }
        }
        super.done();
    }
}
