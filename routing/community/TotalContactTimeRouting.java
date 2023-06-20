/*
 * @(#)DistributedBubbleRap.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing.community;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class TotalContactTimeRouting implements RoutingDecisionEngine {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;

    /**
     * Constructs a DistributedBubbleRap Decision Engine based upon the settings
     * defined in the Settings object parameter. The class looks for the class
     * names of the community detection and centrality algorithms that should be
     * employed used to perform the routing.
     *
     * @param s Settings to configure the object
     */
    public TotalContactTimeRouting(Settings s) {

    }

    /**
     * Constructs a DistributedBubbleRap Decision Engine from the argument
     * prototype.
     *
     * @param proto Prototype DistributedBubbleRap upon which to base this
     * object
     */
    public TotalContactTimeRouting(TotalContactTimeRouting proto) {
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
//        contactDur = new HashMap<DTNHost, Double>();
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    /**
     * Starts timing the duration of this new connection and informs the
     * community detection object that a new connection was formed.
     *
     * @see
     * routing.RoutingDecisionEngine#doExchangeForNewConnection(core.Connection,
     * core.DTNHost)
     */
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        TotalContactTimeRouting de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

//        this.community.newConnection(myHost, peer, de.community);
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = startTimestamps.get(peer);
//		double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        // add this connection to the list
        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }

        //hitung kontak durasi
//        double timeContact = etime - time;
//        if (!contactDur.containsKey(peer)) { //kalo belum pernah ketemu
//            contactDur.put(peer, timeContact);
//        } else { //kalo udah pernah ketemu
//            double newTime = contactDur.get(peer) + timeContact;
//            contactDur.put(peer, newTime);
//        }

        startTimestamps.remove(peer);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            startTimestamps.get(peer);
        }
        return 0;
    }

    public boolean newMessage(Message m) {
        return true; // Always keep and attempt to forward a created message
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost; // Unicast Routing
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true; // trivial to deliver to final dest
        }
        /*
		 * Here is where we decide when to forward along a message. 
		 * 
		 * DiBuBB works such that it first forwards to the most globally central
		 * nodes in the network until it finds a node that has the message's 
		 * destination as part of it's local community. At this point, it uses 
		 * the local centrality metric to forward a message within the community. 
         */

        // Which of us has the dest in our local communities, this host or the peer
        TotalContactTimeRouting de = this.getOtherDecisionEngine(otherHost);
        double myTotal = this.countTime(m.getTo());
        double peerTotal = de.countTime(m.getTo());

        if (myTotal < peerTotal) {
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    public RoutingDecisionEngine replicate() {
        return new TotalContactTimeRouting(this);
    }

    private TotalContactTimeRouting getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (TotalContactTimeRouting) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    public double countTime(DTNHost h) {
        if(this.connHistory.containsKey(h)){
            double totalTime = 0.0;
            List<Duration> duration = new LinkedList<>(connHistory.get(h));
            Iterator<Duration> i = duration.iterator();
            
            Duration d = new Duration(0,0);
            while(i.hasNext()){
                d = i.next();
                double time = d.end - d.start;
                totalTime += time;
            }
            
            return totalTime;
        } else {
            return 0;
        }
    }
    
    @Override
    public void update(DTNHost thisHost) {
    }
}
