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
import routing.community.ConnectionHistoryEngine;
import routing.community.DistributedBubbleRap;
import routing.community.Duration;

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
public class ConnectionHistoryReport extends Report {

    public ConnectionHistoryReport() {
        init();
    }

    @Override
    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (DTNHost h : nodes) {
            MessageRouter mr = h.getRouter();
            DecisionEngineRouter de = (DecisionEngineRouter) mr;
            DistributedBubbleRap dbr = (DistributedBubbleRap) de.getDecisionEngine();
            ConnectionHistoryEngine hd = (ConnectionHistoryEngine) dbr;
            
            Map<DTNHost, List<Duration>> connHis = hd.getConnHistory();
            
//            MessageRouter r = h.getRouter();
//            if (!(r instanceof DecisionEngineRouter)) {
//                continue;
//            }
//            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
//            if (!(de instanceof ConnectionHistoryEngine)) {
//                continue;
//            }
//            ConnectionHistoryEngine cd = (ConnectionHistoryEngine) de;
//
//            Map<DTNHost, List<Duration>> connHis = cd.getConnHistory();
            String print = h.toString() + ": ";
//            for (Map.Entry<DTNHost, List<Duration>> conn : connHis.entrySet()) {
//                double avg = 0;
//                double totalDuration = 0;
//                if (!conn.getValue().isEmpty()) {
//                    for (Duration duration : conn.getValue()) {
//                        double total = duration.end - duration.start;
//                        totalDuration = totalDuration + total;
//                    }
//                    avg = totalDuration/conn.getValue().size();
//                }
//                print = print + avg;
//            }
            for (Map.Entry<DTNHost, List<Duration>> conn : connHis.entrySet()) {
                List<Duration> dur = conn.getValue();
                Iterator<Duration> i = dur.iterator();
                int frek = 0;
                double totalDuration = 0;
                double total = 0;
                
                Duration d = new Duration(0,0);
                while(i.hasNext()) {
                    if (frek == 0) {
                        d = i.next();
                        total = d.start;
                    } else {
                        double end = d.end;
                        d = i.next();
                        total = d.start - end;
                    }
                    totalDuration += total;
                    frek++;
                }
                
                if (!dur.isEmpty()) {
                    double avg = totalDuration/frek;
                    print = print + avg + " ";
                }
            }
            write(print);

        }

        super.done();
    }

}
