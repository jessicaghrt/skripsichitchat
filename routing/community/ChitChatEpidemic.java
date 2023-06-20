package routing.community;

import core.*;
import java.util.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author IONE
 */
public class ChitChatEpidemic implements RoutingDecisionEngine, TransientEngine {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<String, Double> connHistoryT; // save endtime with interest
    protected Map<DTNHost, Double> connHistoryN; // save endtime with node
    protected Map<String, Double> trs;
    protected Map<String, Double> trsAwal;
    protected List<Connection> connections;

    public static final String ChitChat = "ChitChat";
    private String M_TOPIC = "Message Topic";

    protected double timeStart;

    public static String randomContentSocial;
    public static String randomContentNonSocial;
    protected Map<String, Set<String>> topics;
    protected List<String> allTopics;

    private List<Tuple<DTNHost, Tuple<Map<String, Double>, Map<String, Double>>>> trsInterval;

    public ChitChatEpidemic(Settings s) {

        startTimestamps = new HashMap<DTNHost, Double>();
        connHistoryT = new HashMap<String, Double>();
        connHistoryN = new HashMap<DTNHost, Double>();
        this.trs = new HashMap<String, Double>();
        this.trsAwal = new HashMap<String, Double>();
        connections = new LinkedList<Connection>();

        trsInterval = new ArrayList<>();

        topics = new HashMap<String, Set<String>>();
        topics.put("Sport", new HashSet<>(Arrays.asList("Football", "Basketball", "Tennis", "Swimming", "Running")));
        topics.put("Cooking", new HashSet<>(Arrays.asList("Baking", "Grilling", "Sushi", "Vegetarian", "Cake")));
        topics.put("Film", new HashSet<>(Arrays.asList("Drama", "Comedy", "Action", "Romance", "Documentary")));
        topics.put("Traveling", new HashSet<>(Arrays.asList("Beach", "Mountains", "Museum", "Parks", "Cities")));
        topics.put("Music", new HashSet<>(Arrays.asList("Rock", "Pop", "Blues", "Classical", "Jazz")));

        allTopics = new LinkedList<>();
        allTopics.add("Sport");
        allTopics.add("Cooking");
        allTopics.add("Film");
        allTopics.add("Traveling");
        allTopics.add("Music");
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected ChitChatEpidemic(ChitChatEpidemic r) {
        this.startTimestamps = r.startTimestamps;
        this.connHistoryT = r.connHistoryT;
        this.connHistoryN = r.connHistoryN;
        this.trs = r.trs;
        this.trsAwal = r.trsAwal;
        this.trsInterval = r.trsInterval;
        this.connections = r.connections;

        this.topics = r.topics;
        this.allTopics = r.allTopics;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // ketika transcient masih kosong maka perlu diisi dulu
        ChitChatEpidemic de = getOtherDecisionEngine(peer);
        if (this.trs.isEmpty()) {
            for (String i : allTopics) {
                if (thisHost.getSocialProfile().contains(i)) {
                    this.trs.put(i, 0.5);
                } else {
                    this.trs.put(i, 0.0);
                }
            }
        }
        if (de.trs.isEmpty()) {
            for (String i : allTopics) {
                if (peer.getSocialProfile().contains(i)) {
                    de.trs.put(i, 0.5);
                } else {
                    de.trs.put(i, 0.0);
                }
            }
        }
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double etime = SimClock.getTime();
        ChitChatEpidemic de = this.getOtherDecisionEngine(peer);
        for (Map.Entry<String, Double> entry : de.trs.entrySet()) {
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
        ChitChatEpidemic de = this.getOtherDecisionEngine(peer);

        //mencatat waktu mulai connection
        timeStart = SimClock.getTime();

        double time = SimClock.getTime();

        this.startTimestamps.put(peer, time);
        de.startTimestamps.put(myHost, time);

        this.connections = myHost.getConnections();

        //menghitung decay sebelumnya
        for (String interest : this.trs.keySet()) {
            double trsNew = this.countTrsDecay(myHost, peer, interest);
            this.trs.put(interest, trsNew);
        }

        for (String interest : de.trs.keySet()) {
            double trsNew = de.countTrsDecay(peer, myHost, interest);
            de.trs.put(interest, trsNew);
        }

        this.trsAwal = this.trs;
    }

    private Tuple<String, String> setRandomContent(String code) {
        String randomKey;
        String randomContent;

        Set<String> keys;
        Set<String> randomValue;

        Random random = new Random();

        keys = topics.keySet();

        String[] stringArray = keys.toArray(new String[keys.size()]);
        int randomIndex = random.nextInt(stringArray.length);
        randomKey = stringArray[randomIndex];

        randomValue = topics.get(randomKey);

        String[] array = randomValue.toArray(new String[randomValue.size()]);
        int randomIndexValue = random.nextInt(array.length);
        randomContent = array[randomIndexValue];

        Tuple<String, String> content = new Tuple<String, String>(randomKey, randomContent);
        return content;
    }

    // sama dengan create new message
    @Override
    public boolean newMessage(Message m) {
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
//        return true;
        
        Tuple<String,String> topic = (Tuple<String,String>) m.getProperty(M_TOPIC);
        if (otherHost.getSocialProfile().contains(topic.getKey())){
            return true;
        }
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
        double beta = 20; //bebas dicari yang paling bagus
        double trs = this.trs.get(interest);
        double trsNew;

        double stime1;
        if (connHistoryT.containsKey(interest)) {
            stime1 = connHistoryT.get(interest);
        } else {
            stime1 = 0;
        }
        double stime2 = startTimestamps.get(peer);

        if (myHost.getSocialProfile().contains(interest)) {
            trsNew = (trs - 0.5) / beta * (stime2 - stime1) + 0.5;
        } else {
            trsNew = trs / beta * (stime2 - stime1);
        }

        return trsNew;
    }

    private double countTrsGrowth(DTNHost myHost, DTNHost peer, String interest) {
        double trs = this.trs.get(interest);

        double delta = countDelta(myHost, peer, interest);

        double trsNew = trs + delta;

        if (trsNew >= 1) {
            return 1;
        } else {
            return trsNew;
        }
    }

    private double countDelta(DTNHost myHost, DTNHost peer, String interest) {
        double delta = 0.0;

        int damper;

        ChitChatEpidemic de;

        Iterator<Connection> i = connections.iterator();
        while (i.hasNext()) {
            Connection c = i.next();
            de = this.getOtherDecisionEngine(c.getOtherNode(myHost));

            if (myHost.getSocialProfile().contains(interest)
                    && peer.getSocialProfile().contains(interest)) {
                damper = 1;
            } else if (myHost.getSocialProfile().contains(interest)
                    && !peer.getSocialProfile().contains(interest)) {
                damper = 2;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && peer.getSocialProfile().contains(interest)) {
                damper = 3;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && !peer.getSocialProfile().contains(interest)) {
                damper = 4;
            } else if (peer.getSocialProfile().contains(interest)
                    && this.trs.get(interest) > de.trs.get(interest)) {
                damper = 5;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && this.trs.containsKey(interest)) {
                damper = 6;
            } else {
                damper = 0;
            }

            double stime1 = timeStart;

            double trs;
            if (de.trs.containsKey(interest)) {
                trs = de.trs.get(interest);
            } else {
                trs = 0;
            }
            double stime2 = SimClock.getTime();

            double count = trs * (stime2 - stime1) / damper;
            delta = delta + count;
        }

        return delta;
    }

    @Override
    public void update(DTNHost thisHost) {

    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new ChitChatEpidemic(this);
    }

    private ChitChatEpidemic getOtherDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ChitChatEpidemic) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public List<Tuple<DTNHost, Tuple<Map<String, Double>, Map<String, Double>>>> getTrs() {
        return this.trsInterval;
    }
}
