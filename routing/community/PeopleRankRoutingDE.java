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

public class PeopleRankRoutingDE implements RoutingDecisionEngine, PeopleRankEngine {

    public double d = 0.5;
    public int threshold = 15;

    public double thisRank = 0;

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
    private Map<DTNHost, Tuple<Double, Integer>> peopleRank;

    /**
     * Constructs a DistributedBubbleRap Decision Engine based upon the settings
     * defined in the Settings object parameter. The class looks for the class
     * names of the community detection and centrality algorithms that should be
     * employed used to perform the routing.
     *
     * @param s Settings to configure the object
     */
    public PeopleRankRoutingDE(Settings s) {

    }

    /**
     * Constructs a DistributedBubbleRap Decision Engine from the argument
     * prototype.
     *
     * @param proto Prototype DistributedBubbleRap upon which to base this
     * object
     */
    public PeopleRankRoutingDE(PeopleRankRoutingDE proto) {
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
        peopleRank = new HashMap<DTNHost, Tuple<Double, Integer>>();
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
        PeopleRankRoutingDE de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

        //buat kalo udah temenan
        if (this.peopleRank.keySet().contains(peer)) {
            Tuple<Double, Integer> f = new Tuple<Double, Integer>(de.countRank(), de.countPeer());

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

        //buat cek temen baru, apakah udah bisa jadi temen atau belum?
        PeopleRankRoutingDE de = this.getOtherDecisionEngine(peer);
        if (!this.peopleRank.containsKey(peer)) { //kalo belum temenan
            if (this.countTime(peer) >= threshold) { //kalo udah kontak lebih dari threshold
                Tuple<Double, Integer> f = new Tuple<Double, Integer>(de.countRank(), de.countPeer());
                this.peopleRank.put(peer, f);
                this.thisRank = this.countRank();
            }
        }

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

        // Which of us has the dest in our local communities, this host or the peer
        PeopleRankRoutingDE de = this.getOtherDecisionEngine(otherHost);
        double myRank = this.countRank();
        double peerRank = de.countRank();

        if (myRank < peerRank) {
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
        return new PeopleRankRoutingDE(this);
    }

    //menghitung total contact time thisHost dengan node dest yang diinginkan
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

    //menghitung rank dari thisHost
    protected double countRank() {
        double total = 0;
        double totalRank = 0;

        for (Map.Entry<DTNHost, Tuple<Double, Integer>> entry : peopleRank.entrySet()) {

            double rank = entry.getValue().getKey(); //rank temen
            int sum = entry.getValue().getValue(); //jumlah temennya temen

            if (sum != 0) {
                total = rank / sum;
            }

            totalRank += total;
        }
        return (1 - d) + d * totalRank;
    }

    //menghitung peer yang sudah menjadi teman dari thisHost
    protected int countPeer() {
        return this.peopleRank.size();
    }

    private PeopleRankRoutingDE getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (PeopleRankRoutingDE) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    //mengambil nilai thisRank untuk report
    @Override
    public double getThisRank() {
        return thisRank;
    }

    @Override
    public void update(DTNHost thisHost) {
    }
}
