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

public class PeopleRankRouting implements RoutingDecisionEngine, PeopleRankEngine {

    /**
     * Community Detection Algorithm to employ -setting id {@value}
     */
    public static final String COMMUNITY_ALG_SETTING = "communityDetectAlg";
    /**
     * Centrality Computation Algorithm to employ -setting id {@value}
     */
    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";

    public double d = 0.5;
    public int threshold = 15;

    public double thisRank = 0;

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
    private Map<DTNHost, Tuple<Double,Integer>> peopleRank;
    private Map<DTNHost, Double> contactDur;

    protected CommunityDetection community;
    protected Centrality centrality;

    /**
     * Constructs a DistributedBubbleRap Decision Engine based upon the settings
     * defined in the Settings object parameter. The class looks for the class
     * names of the community detection and centrality algorithms that should be
     * employed used to perform the routing.
     *
     * @param s Settings to configure the object
     */
    public PeopleRankRouting(Settings s) {
        if (s.contains(COMMUNITY_ALG_SETTING)) {
            this.community = (CommunityDetection) s.createIntializedObject(s.getSetting(COMMUNITY_ALG_SETTING));
        } else {
            this.community = new SimpleCommunityDetection(s);
        }

        if (s.contains(CENTRALITY_ALG_SETTING)) {
            this.centrality = (Centrality) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        } else {
            this.centrality = new SWindowCentrality(s);
        }
    }

    /**
     * Constructs a DistributedBubbleRap Decision Engine from the argument
     * prototype.
     *
     * @param proto Prototype DistributedBubbleRap upon which to base this
     * object
     */
    public PeopleRankRouting(PeopleRankRouting proto) {
        this.community = proto.community.replicate();
        this.centrality = proto.centrality.replicate();
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
        peopleRank = new HashMap<DTNHost, Tuple<Double,Integer>>();
        contactDur = new HashMap<DTNHost, Double>();
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
        PeopleRankRouting de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

        //buat kalo udah temenan
        if (this.peopleRank.keySet().contains(peer)) {
            Tuple<Double,Integer> f = new Tuple<Double,Integer>(de.countRank(), de.countPeer());

            this.peopleRank.put(peer, f);
            this.thisRank = this.countRank();
        }

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
        double timeContact = etime-time;
        if (!contactDur.containsKey(peer)) { //kalo belum pernah ketemu
            contactDur.put(peer, timeContact);
        } else { //kalo udah pernah ketemu
            double newTime = contactDur.get(peer) + timeContact;
            contactDur.put(peer, newTime);
        }
        
        //buat cek temen baru, apakah udah bisa jadi temen atau belum?
        PeopleRankRouting de = this.getOtherDecisionEngine(peer);
        if (!this.peopleRank.containsKey(peer)) { //kalo belum temenan
            if (this.contactDur.get(peer) >= threshold) { //kalo udah kontak lebih dari threshold
                Tuple<Double,Integer> f = new Tuple<Double,Integer>(de.countRank(), de.countPeer());
                this.peopleRank.put(peer, f);
                this.thisRank = this.countRank();
            }
        }

        CommunityDetection peerCD = this.getOtherDecisionEngine(peer).community;

        // inform the community detection object that a connection was lost.
        // The object might need the whole connection history at this point.
        community.connectionLost(thisHost, peer, peerCD, history);

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
        PeopleRankRouting de = this.getOtherDecisionEngine(otherHost);
        double myRank = this.countRank();
        double peerRank = de.countRank();

        if (myRank < peerRank) {
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        // DiBuBB allows a node to remove a message once it's forwarded it into the
        // local community of the destination
        PeopleRankRouting de = this.getOtherDecisionEngine(otherHost);
        return de.commumesWithHost(m.getTo())
                && !this.commumesWithHost(m.getTo());
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        PeopleRankRouting de = this.getOtherDecisionEngine(hostReportingOld);
        return de.commumesWithHost(m.getTo())
                && !this.commumesWithHost(m.getTo());
    }

    public RoutingDecisionEngine replicate() {
        return new PeopleRankRouting(this);
    }

    protected double totalContactTime(DTNHost dest) {
//        double avg = 0;
        double totalDuration = 0;
        for (Map.Entry<DTNHost, List<Duration>> conn : connHistory.entrySet()) {
            if (conn.getKey() == dest) {
                List<Duration> dur = conn.getValue();
                Iterator<Duration> i = dur.iterator();
//                int frek = 0;
                double total = 0;

                Duration d = new Duration(0, 0);
                while (i.hasNext()) {
                    i.next();
                    total = d.end - d.start;
                    totalDuration += total;
//                    frek++;
                }

//                if (!dur.isEmpty()) {
//                    avg = totalDuration / frek;
//                }
            }
        }
        return totalDuration;
    }

    protected double countRank() {
        double total = 0;
        double totalRank = 0;

        for (Map.Entry<DTNHost, Tuple<Double,Integer>> peRank : peopleRank.entrySet()) {
            
            double rank = peRank.getValue().getKey(); //rank temen
            int sum = peRank.getValue().getValue(); //jumlah temennya temen

            if (sum != 0){
                total = rank / sum;
            }
            
            totalRank += total;
        }
        return (1 - d) + d * totalRank;
    }

    protected int countPeer() {
        return this.peopleRank.size();
    }

    protected boolean commumesWithHost(DTNHost h) {
        return community.isHostInCommunity(h);
    }

    protected double getLocalCentrality() {
        return this.centrality.getLocalCentrality(connHistory, community);
    }

    protected double getGlobalCentrality() {
        return this.centrality.getGlobalCentrality(connHistory);
    }

    private PeopleRankRouting getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (PeopleRankRouting) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public double getThisRank() {
        return thisRank;
    }
    
    @Override
    public void update(DTNHost thisHost) {
    }
}
