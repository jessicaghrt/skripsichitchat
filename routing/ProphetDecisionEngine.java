package routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class ProphetDecisionEngine implements RoutingDecisionEngine {

    protected final static String BETA_SETTING = "beta";
    protected final static String P_INIT_SETTING = "initial_p";
    protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

    protected static final double DEFAULT_P_INIT = 0.75; //delivery predictability initialization constant
    protected static final double GAMMA = 0.92; //delivery predictability aging constant
    protected static final double DEFAULT_BETA = 0.45; //delivery predictability transitivity scaling constant default value
    protected static final int DEFAULT_UNIT = 30; //untuk pembagian dalam rumus preds

    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate; //menyimpan waktu terakhir pred diupdate
    protected int secondsInTimeUnit; //buat bagi tapi gatau bagi apa
    
    private Set<Message> msgStamp; //menyimpan pesan
    private Map<DTNHost, Integer> relayed;
    private DTNHost meHost;

    //delivery predictabilities me to other hosts
    private Map<DTNHost, Double> preds;

    public ProphetDecisionEngine(Settings s) {
        if (s.contains(BETA_SETTING)) {
            beta = s.getDouble(BETA_SETTING);
        } else {
            beta = DEFAULT_BETA;
        }

        if (s.contains(P_INIT_SETTING)) {
            pinit = s.getDouble(P_INIT_SETTING);
        } else {
            pinit = DEFAULT_P_INIT;
        }

        if (s.contains(SECONDS_IN_UNIT_S)) {
            secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
        } else {
            secondsInTimeUnit = DEFAULT_UNIT;
        }

        preds = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = 0.0;
    }

    public ProphetDecisionEngine(ProphetDecisionEngine de) {
        beta = de.beta;
        pinit = de.pinit;
        secondsInTimeUnit = de.secondsInTimeUnit;
        meHost = de.meHost;
        msgStamp = new HashSet<>();
        relayed = new HashMap<>();
        preds = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = de.lastAgeUpdate;
    }

    public RoutingDecisionEngine replicate() {
        return new ProphetDecisionEngine(this);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ProphetDecisionEngine de = getOtherProphetDecisionEngine(peer);
        //membuat set DTNHost baru yang isinya gabungan dari temanku dan teman de
        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.preds.size() + de.preds.size());
        hostSet.addAll(this.preds.keySet());
        hostSet.addAll(de.preds.keySet());

        // update preds
        this.agePreds();
        de.agePreds();

        // Update preds for this connection
        // rumus pertama
        double myOldValue = this.getPredFor(peer),
                peerOldValue = de.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;
        preds.put(peer, myPforHost);
        de.preds.put(myHost, peerPforMe);

        // Update transistivities
        // rumus ketiga
        for (DTNHost h : hostSet) {
            myOldValue = 0.0;
            peerOldValue = 0.0;

            if (preds.containsKey(h)) {
                myOldValue = preds.get(h);
            }
            if (de.preds.containsKey(h)) {
                peerOldValue = de.preds.get(h);
            }

            if (h != myHost) {
                preds.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != peer) {
                de.preds.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }
    }

    // selalu membuat pesan baru
    public boolean newMessage(Message m) {
        return true;
    }

    //pesan dikirimkan apabila aHost adalah tujuan
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        msgStamp.add(m); //menyimpan pesan
        meHost = thisHost;
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        //jika otherHost adalah tujuan maka true
        if (m.getTo() == otherHost) {
            return true;
        }

        //masih bingung yang ini, kayanya buat nyatet doang tapi ga dipake
        ProphetDecisionEngine de = getOtherProphetDecisionEngine(otherHost);
        if (msgStamp.contains(m)) { //pesannya sudah pernah disimpan
            //kalau belum dicatat, dimasukan nilai 1, kalau sudah, nilai yang sudah ada +1
            relayed.put(meHost, !relayed.containsKey(meHost)? 1 : relayed.get(meHost) + 1);
        }
        
        //pred de bertemu tujuan lebih besar drpd pred this bertemua tujuan, maka true
        return de.getPredFor(m.getTo()) > this.getPredFor(m.getTo());
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    private ProphetDecisionEngine getOtherProphetDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ProphetDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    // for update preds
    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        // jika baru saja bertemu, tidak dilakukan apa2
        if (timeDiff == 0) {
            return;
        }

        // update isi dalam preds, masing2 value dikali gamma^perbedaan waktu
        // rumus kedua
        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    /**
     * Returns the current prediction (P) value for a host or 0 if entry for the
     * host doesn't exist.
     *
     * @param host The host to look the P for
     * @return the current P value
     */
    private double getPredFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (preds.containsKey(host)) { //kalo udah pernah ketemu
            return preds.get(host);
        } else { //kalo belum pernah ketemu
            return 0;
        }
    }

    @Override
    public void update(DTNHost thisHost) {}
}
