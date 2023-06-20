/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently Connected
 * Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class ChitChatDE1 implements RoutingDecisionEngine {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<String, Double> connHistoryT; // save endtime with interest
    protected Map<DTNHost, Double> connHistoryN; // save endtime with node
    protected Map<String, Double> transcient;
    protected List<Connection> connections;

    private String LOC_GEN = "Message Location Generated";
    private String M_TOPIC = "Message Topic";

    public static String randomContentSocial;
    public static String randomContentNonSocial;
    protected Map<String, Set<String>> topics;
    protected Map<String, Set<String>> topicsUrgent;

    public ChitChatDE1(Settings s) {
        topics = new HashMap<String, Set<String>>();
        topics.put("Sport", new HashSet<>(Arrays.asList("Football", "Basketball", "Tennis", "Swimming", "Running")));
        topics.put("Cooking", new HashSet<>(Arrays.asList("Baking", "Grilling", "Sushi", "Vegetarian", "Cake")));
        topics.put("Film", new HashSet<>(Arrays.asList("Drama", "Comedy", "Action", "Romance", "Documentary")));
        topics.put("Traveling", new HashSet<>(Arrays.asList("Beach", "Mountains", "Museum", "Parks", "Cities")));
        topics.put("Music", new HashSet<>(Arrays.asList("Rock", "Pop", "Blues", "Classical", "Jazz")));

        topicsUrgent = new HashMap<>();
        topicsUrgent.put("Urgent", new HashSet<>(Arrays.asList("Fire", "Fainting", "Chaos", "Crime", "Terrorism")));
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected ChitChatDE1(ChitChatDE1 r) {
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistoryT = new HashMap<String, Double>();
        connHistoryN = new HashMap<DTNHost, Double>();
        this.transcient = new HashMap<String, Double>();
        connections = new LinkedList<Connection>();
        randomContentSocial = r.randomContentSocial;
        randomContentNonSocial = r.randomContentNonSocial;
        topics = r.topics;
        topicsUrgent = r.topicsUrgent;
    }

    @Override
    public ChitChatDE1 replicate() {
        return new ChitChatDE1(this);
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // ketika transcient masih kosong maka perlu diisi dulu
        ChitChatDE1 de = getOtherDecisionEngine(peer);
        if (this.transcient.isEmpty()) {
            for (String i : thisHost.getSocialProfile()) {
                this.transcient.put(i, 0.5);
            }
        }
        if (de.transcient.isEmpty()) {
            for (String i : peer.getSocialProfile()) {
                de.transcient.put(i, 0.5);
            }
        }
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double etime = SimClock.getTime();

        ChitChatDE1 de = this.getOtherDecisionEngine(peer);
        for (Map.Entry<String, Double> entry : de.transcient.entrySet()) {
            if (entry.getValue() > 0.0) {
                String interest = entry.getKey();
                connHistoryT.put(interest, etime);
            }
        }

        connHistoryN.put(peer, etime);

        // Find or create the connection history list
        startTimestamps.remove(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ChitChatDE1 de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

        this.connections = myHost.getConnections();

        for (String interest : this.transcient.keySet()) {
            this.countTrsDecay(myHost, peer, interest);
        }
        System.out.println(transcient);
//        for (String interest : de.transcient.keySet()) {
//            de.countTrsDecay(peer, myHost, interest);
//        }
        for (String interest : this.transcient.keySet()) {
            this.countTrsGrowth(myHost, peer, interest);
        }
        System.out.println(transcient);
//        for (String interest : de.transcient.keySet()) {
//            de.countTrsGrowth(peer, myHost, interest);
//        }
        
//        System.out.println(myHost.getAddress() + " " + this.transcient.size());
    }

    private Tuple<String, String> setRandomContent(String code) {
        String randomKey;
        String randomContent;

        Set<String> keys;
        Set<String> randomValue;

        Random random = new Random();

        if (code.equalsIgnoreCase("M")) {
            keys = topics.keySet();
        } else {
            keys = topicsUrgent.keySet();
        }

        String[] stringArray = keys.toArray(new String[keys.size()]);
        int randomIndex = random.nextInt(stringArray.length);
        randomKey = stringArray[randomIndex];

        if (code.equalsIgnoreCase("M")) {
            randomValue = topics.get(randomKey);
        } else {
            randomValue = topicsUrgent.get(randomKey);
        }

        String[] array = randomValue.toArray(new String[randomValue.size()]);
        int randomIndexValue = random.nextInt(array.length);
        randomContent = array[randomIndexValue];

        Tuple<String, String> content = new Tuple<String, String>(randomKey, randomContent);
        return content;
    }

    // sama dengan create new message
    @Override
    public boolean newMessage(Message m) {
        //add property for message generated location
        double x = m.getFrom().getLocation().getX();
        double y = m.getFrom().getLocation().getY();
        Tuple<Double, Double> location = new Tuple<Double, Double>(x, y);
        m.addProperty(LOC_GEN, location);

        //add propoerty for interest
        Tuple<String, String> topic;
        if (m.getId().contains("M")) {
            topic = setRandomContent("M");
        } else {
            topic = setRandomContent("S");
        }
        m.addProperty(M_TOPIC, topic);

        return true;
    }

    // method yang dipanggil ketika kita menerima pesan, sama dengan message transferred?
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    // kalau bukan tujuan brati tidak perlu menyimpan
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    // apakah harus mengirimkan pesan ini ke host yang ditemui?
    // kalau otherHost adalah tujuan, pesannya dikirim
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
//        System.out.println("Mau sent. ID "+m.getId()+" | LOC "+m.getProperty(LOC_GEN));
        if (m.getTo() == otherHost) {
            return true;
        }

        // membandingkan prioritas dan interest
//        Tuple<String, String> topic = (Tuple<String, String>) m.getProperty(M_TOPIC);
//        String interest = topic.getKey();
//
//        ChitChatDE1 de = getOtherDecisionEngine(otherHost);
//        double myTrs;
//        try {
//            myTrs = this.transcient.get(interest);
//        } catch (NullPointerException e) {
//            myTrs = 0;
//        }
//        double peerTrs;
//        try {
//            peerTrs = de.transcient.get(interest);
//        } catch (NullPointerException e) {
//            peerTrs = 0;
//        }
//
//        if (peerTrs > myTrs) {
//            return true;
//        }
        return false;
    }

    // dilakukan when transfer done, setelah transfer tidak perlu hapus 
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    private double countTrsDecay(DTNHost myHost, DTNHost peer, String interest) {
        double beta = 0.7;
        double trs = this.transcient.get(interest);
        double trsNew;

        double stime1;
        if (connHistoryT.containsKey(interest)) {
            stime1 = connHistoryT.get(interest);
        } else {
            stime1 = 0;
        }
//        double stime1 = connHistoryT.get(interest).get(connHistoryT.get(interest).size() - 1).end;
        double stime2 = startTimestamps.get(peer);
//        System.out.println(stime1);
//        System.out.println(stime2);

        if (myHost.getSocialProfile().contains(interest)) {
            trsNew = (trs - 0.5) / beta * (stime2 - stime1) + 0.5;
//            System.out.println("up");
        } else {
            trsNew = trs / beta * (stime2 - stime1);
//            System.out.println("down");
        }

//        System.out.println("Decay "+trsNew);
        return trsNew;
    }

    private double countTrsGrowth(DTNHost myHost, DTNHost peer, String interest) {
        double trs = this.transcient.get(interest);
        
        double delta = countDelta(myHost, peer, interest);
        
        double trsNew = trs + delta;
//        System.out.println("TRS "+trs+" Delta "+delta+" Growth "+trsNew);

        if (trsNew >= 1) {
            return 1;
        } else {
            return trsNew;
        }
    }

    private double countDelta(DTNHost myHost, DTNHost peer, String interest) {
        double delta = 0.0;
        Connection c;
        ChitChatDE1 de;
        Iterator<Connection> i = connections.iterator();

        int damper;

        while (i.hasNext()) {
            c = i.next();
            de = this.getOtherDecisionEngine(c.getOtherNode(myHost));

            if (myHost.getSocialProfile().contains(interest)
                    && c.getOtherNode(myHost).getSocialProfile().contains(interest)) {
                damper = 1;
            } else if (myHost.getSocialProfile().contains(interest)
                    && !c.getOtherNode(myHost).getSocialProfile().contains(interest)) {
                damper = 2;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && c.getOtherNode(myHost).getSocialProfile().contains(interest)) {
                damper = 3;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && !c.getOtherNode(myHost).getSocialProfile().contains(interest)) {
                damper = 4;
            } else if (c.getOtherNode(myHost).getSocialProfile().contains(interest)
                    && this.transcient.get(interest) > de.transcient.get(interest)) {
                damper = 5;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && this.transcient.containsKey(interest)) {
                damper = 6;
            } else {
                damper = 0;
            }

            double stime1;
            double trs;
            if (connHistoryN.containsKey(de)) {
                stime1 = connHistoryN.get(de);
            } else {
                stime1 = 0;
            }
            if (de.transcient.containsKey(interest)) {
                trs = de.transcient.get(interest);
            } else {
                trs = 0;
            }
//            double stime1 = connHistoryN.get(de).get(connHistoryN.get(de).size() - 1).end;
            double stime2 = startTimestamps.get(peer);

//            double trs = de.transcient.get(interest);

            double count = trs * (stime2 - stime1) / damper;
            delta = delta + count;
        }
//        System.out.println("Delta "+delta);
        return delta;
    }

    private ChitChatDE1 getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ChitChatDE1) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {

    }
}