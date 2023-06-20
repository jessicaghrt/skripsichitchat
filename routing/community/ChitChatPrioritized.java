package routing.community;

import core.*;
import java.text.DecimalFormat;
import java.util.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author IONE
 */
public class ChitChatPrioritized implements RoutingDecisionEngine, TransientEngine {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<String, Double> connHistoryT; // save endtime with interest
    protected Map<DTNHost, Double> connHistoryN; // save endtime with node
    protected Map<String, Double> transcient;
    protected List<Connection> connections;

    public static final String ChitChatPrioritized = "ChitChatPrioritized";
    private String LOC_GEN = "Message Location Generated";
    private String M_TOPIC = "Message Topic";
    private String PRIORITY = "Priority";

    protected double timeStart;

    public static String randomContentSocial;
    public static String randomContentNonSocial;
    protected Map<String, Set<String>> topics;
    protected Map<String, Set<String>> topicsUrgent;
    private double lastCheck;
    private static int LIMIT_TW = 300;
    private Map<DTNHost, Tuple<String, List<Integer>>> idf;
    protected List<String> allTopics;

    public ChitChatPrioritized(Settings s) {

        startTimestamps = new HashMap<DTNHost, Double>();
        connHistoryT = new HashMap<String, Double>();
        connHistoryN = new HashMap<DTNHost, Double>();
        this.transcient = new HashMap<String, Double>();
        connections = new LinkedList<Connection>();

        topics = new HashMap<String, Set<String>>();
        topics.put("Sport", new HashSet<>(Arrays.asList("Football", "Basketball", "Tennis", "Swimming", "Running")));
        topics.put("Cooking", new HashSet<>(Arrays.asList("Baking", "Grilling", "Sushi", "Vegetarian", "Cake")));
        topics.put("Film", new HashSet<>(Arrays.asList("Drama", "Comedy", "Action", "Romance", "Documentary")));
        topics.put("Traveling", new HashSet<>(Arrays.asList("Beach", "Mountains", "Museum", "Parks", "Cities")));
        topics.put("Music", new HashSet<>(Arrays.asList("Rock", "Pop", "Blues", "Classical", "Jazz")));

        topicsUrgent = new HashMap<>();
        topicsUrgent.put("Urgent", new HashSet<>(Arrays.asList("Fire", "Fainting", "Chaos")));

        idf = new HashMap<>();

        allTopics = new LinkedList<>();
        allTopics.add("Sport");
        allTopics.add("Cooking");
        allTopics.add("Film");
        allTopics.add("Traveling");
        allTopics.add("Music");
        allTopics.add("Urgent");
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected ChitChatPrioritized(ChitChatPrioritized r) {
        this.startTimestamps = r.startTimestamps;
        this.connHistoryT = r.connHistoryT;
        this.connHistoryN = r.connHistoryN;
        this.transcient = r.transcient;
        this.connections = r.connections;

        this.topics = r.topics;
        this.topicsUrgent = r.topicsUrgent;
        this.idf = r.idf;
        this.allTopics = r.allTopics;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // ketika transcient masih kosong maka perlu diisi dulu
        ChitChatPrioritized de = getOtherDecisionEngine(peer);
        if (this.transcient.isEmpty()) {
            for (String i : allTopics) {
                if (thisHost.getSocialProfile().contains(i)) {
                    this.transcient.put(i, 0.5);
                } else {
                    this.transcient.put(i, 0.0);
                }
            }
        }
        if (de.transcient.isEmpty()) {
            for (String i : allTopics) {
                if (peer.getSocialProfile().contains(i)) {
                    de.transcient.put(i, 0.5);
                } else {
                    de.transcient.put(i, 0.0);
                }
            }
        }
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double etime = SimClock.getTime();
        ChitChatPrioritized de = this.getOtherDecisionEngine(peer);
        for (Map.Entry<String, Double> entry : de.transcient.entrySet()) {
//            System.out.println("Interest: " + entry.getKey() + " trs: " + entry.getValue());
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
        ChitChatPrioritized de = this.getOtherDecisionEngine(peer);

        //mencatat waktu mulai connection
        timeStart = SimClock.getTime();

        double time = SimClock.getTime();

        this.startTimestamps.put(peer, time);
        de.startTimestamps.put(myHost, time);

        this.connections = myHost.getConnections();

        //menghitung decay sebelumnya
        for (String interest : this.transcient.keySet()) {
            double trsNew = this.countTrsDecay(myHost, peer, interest);
            this.transcient.put(interest, trsNew);
        }

        for (String interest : de.transcient.keySet()) {
            double trsNew = de.countTrsDecay(peer, myHost, interest);
            de.transcient.put(interest, trsNew);
        }

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
        //add property for message priority
        m.addProperty(PRIORITY, 0);
        //add property for message generated location
        double x = m.getFrom().getLocation().getX();
        double y = m.getFrom().getLocation().getY();
        DecimalFormat df = new DecimalFormat("#.####");
        double hasilX = Double.parseDouble(df.format(x));
        double hasilY = Double.parseDouble(df.format(y));
        Tuple<Double, Double> location = new Tuple<>(hasilX, hasilY);
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

        if ((int) m.getProperty(PRIORITY) > 10) {
            return true;
        }

        //menghitung grow tepat sebelum ngirim aja, karena diperlukan untuk forwarding
        ChitChatPrioritized de = getOtherDecisionEngine(otherHost);
        for (String interest : this.transcient.keySet()) {
            double trsNew = this.countTrsGrowth(thisHost, otherHost, interest);
            Map<String, Double> newTranscient = new HashMap<>(this.transcient);
            newTranscient.put(interest, trsNew);
            this.transcient = newTranscient;
        }
        for (String interest : de.transcient.keySet()) {
            double trsNew = de.countTrsGrowth(otherHost, thisHost, interest);
            Map<String, Double> newTranscient = new HashMap<>(de.transcient);
            newTranscient.put(interest, trsNew);
            de.transcient = newTranscient;
        }

        if (m.getTo() == otherHost) {
            return true;
        }

        // membandingkan prioritas dan interest
        Tuple<String, String> topic = (Tuple<String, String>) m.getProperty(M_TOPIC);
        String interest = topic.getKey();

        double myTrs = this.transcient.get(interest);
        double peerTrs = de.transcient.get(interest);
        if (peerTrs > myTrs) {
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
        double trs = this.transcient.get(interest);
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
        double trs = this.transcient.get(interest);

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

        ChitChatPrioritized de;

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
                    && this.transcient.get(interest) > de.transcient.get(interest)) {
                damper = 5;
            } else if (!myHost.getSocialProfile().contains(interest)
                    && this.transcient.containsKey(interest)) {
                damper = 6;
            } else {
                damper = 0;
            }

            double stime1 = timeStart;

            double trs;
            if (de.transcient.containsKey(interest)) {
                trs = de.transcient.get(interest);
            } else {
                trs = 0;
            }
            double stime2 = SimClock.getTime();

            double count = trs * (stime2 - stime1) / damper;
            delta = delta + count;
        }

        return delta;
    }

    public List<Message> getMessagesWithTopic(String term, DTNHost myHost) {
        List<Message> messagesWithTopic = new ArrayList<>();
        for (Message message : myHost.getMessageCollection()) {
            // Ambil term pesan
            Tuple<String, String> topic = (Tuple<String, String>) message.getProperty(M_TOPIC);
            if (topic.getValue().equals(term)) {
                messagesWithTopic.add(message);
            }
        }
        return messagesWithTopic;
    }

    private double calculateTF(String term, DTNHost host) {
        double tf = 0.0;
        double sum = 0.0;

        // Ambil pesan yang spesifik dengan term
        List<Message> messagesWithTerm = getMessagesWithTopic(term, host);

        // Ambil jumlah pesan yang spesifik dengan term
        sum = messagesWithTerm.size();
        if (sum > 0.0) {
            tf = sum / host.getMessageCollection().size();
        }
        return Math.pow(tf, 2);
    }

    private double calculateIDF(String term, DTNHost host) {
        int N = 0; // jumlah dokumen dalam koleksi
        int n = 0; // jumlah dokumen yang mengandung term
        int tampungAllDoc = 0;
        int tampungTerm = 0;
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        Map<DTNHost, Tuple<String, List<Integer>>> mapTerm = new HashMap<>();
        List<Integer> listAllDoc = new ArrayList();
        // Menghitung jumlah dokumen dalam koleksi
        for (DTNHost h : hosts) {
            N += h.getMessageCollection().size();
        }
        listAllDoc.add(N);
        for (Integer intAllDoc : listAllDoc) {
            tampungAllDoc += intAllDoc;
        }
        // Menghitung jumlah dokumen yang mengandung term
        for (DTNHost h : hosts) {
            for (Message message : h.getMessageCollection()) {
                Tuple<String, String> topic = (Tuple<String, String>) message.getProperty(M_TOPIC);
                if (topic.getValue() != null && topic.getValue().equals(term)) {
                    n++;
                    break;
                }
            }
        }
        List<Integer> valueTerm = new ArrayList();
        valueTerm.add(n);
        Tuple<String, List<Integer>> tupleTerm = new Tuple<>(term, valueTerm);
        mapTerm.put(host, tupleTerm);
        if (mapTerm.get(host).getKey().equals(term)) {
            for (Integer intTerm : mapTerm.get(host).getValue()) {
                tampungTerm += intTerm;
            }
        }
        // Menghitung IDF
        return Math.log(tampungAllDoc / tampungTerm);
    }

    // rumus Euclidean
    public double hitungJarak(Tuple<Double, Double> now, Tuple<Double, Double> next) {
        double x = next.getKey() - now.getKey();
        double y = next.getValue() - now.getValue();
        return Math.sqrt((x * x) + (y * y));
    }

    public double calculateSD(String term, DTNHost host) {
        double sd = 0, n = 0, jmlhD = 0;
        List<Tuple<Double, Double>> pesan = new ArrayList<>();
        List<Message> messagesWithTerm = getMessagesWithTopic(term, host);
        // Menambahkan lokasi tiap pesan yang isinya sama
        for (Message mList : messagesWithTerm) {
            pesan.add((Tuple<Double, Double>) mList.getProperty(LOC_GEN));
        }
        // faktorial dari jarak antar pesan yang isinya sama
        for (int j = 0; j < messagesWithTerm.size(); j++) {
            for (int k = j + 1; k < messagesWithTerm.size(); k++) {
                Tuple<Double, Double> pesan1 = pesan.get(j);
                Tuple<Double, Double> pesan2 = pesan.get(k);
                // disimpan dalam jmlhD
                jmlhD += hitungJarak(pesan1, pesan2);
                n++;
            }
        }

        double E_X = jmlhD / n;
        double sum = 0, sum2 = 0;
        for (int i = 0; i < pesan.size(); i++) {
            for (int j = i + 1; j < pesan.size(); j++) {
                double d = hitungJarak(pesan.get(i), pesan.get(j));
                sum += Math.pow(d - E_X, 2);
                sum2 += d - E_X;
            }
        }

        sd = (Math.sqrt((pesan.size() * sum - Math.pow(sum2, 2)) / (pesan.size() * (pesan.size() - 1)))) / 1000;
        if (Double.isNaN(sd)) {
            sd = 0.0;
        }
        return sd;

    }

    private double TFIDFModified(String term, DTNHost thisHost) {
        // Hitung TF-IDFModified = TF^2*LOG[IDF]/SD
        return (calculateTF(term, thisHost) * calculateIDF(term, thisHost)) / (calculateSD(term, thisHost));
    }

    private void calculatePriority(DTNHost thisHost) {
        double sumTFIDF = 0.0; // Penjumlahan nilai TF-IDF
        double idf = 0; // Nilai IDF dari masing-masing Host yang memiliki term yg sama
        int mt = 0; //Jumlah pesan yang terkait/term yang sama

        // Ambil seluruh koleksi pesan dari thisHost
        for (Message message : thisHost.getMessageCollection()) {
            mt = 0;
            sumTFIDF = 0.0;
            // Ambil term pesan
            Tuple<String, String> topic = (Tuple<String, String>) message.getProperty(M_TOPIC);
            String messageTerm = topic.getValue();
            // Ambil pesan yang spesifik dengan term dan dibuat List
            List<Message> messagesWithTerm = getMessagesWithTopic(messageTerm, thisHost);
            // Hitung TF-IDF dari term yang sama
            if (messagesWithTerm.size() >= 2) {
                for (Message m : messagesWithTerm) {
                    Tuple<String, String> topicTerm = (Tuple<String, String>) m.getProperty(M_TOPIC);
                    if (topicTerm.getValue() != null && topicTerm.getValue().equals(messageTerm)) {
                        mt++;
                        sumTFIDF += TFIDFModified(topicTerm.getValue(), thisHost);
                    }
                }
            }
            // Hitung prioritas pesan
            double priority = sumTFIDF / mt;
            if (Double.isInfinite(priority)) {
                // Jika hasil priority adalah infinity, ganti nilai priority dengan 0.0
                priority = 0.0;
            }
            int priorityInt = (int) priority;
            if (message.getProperty(PRIORITY) == null || (int) message.getProperty(PRIORITY) <= 0) {
                message.updateProperty(PRIORITY, priorityInt);
            } else {
                message.updateProperty(PRIORITY, priorityInt);
            }
        }

    }

    @Override
    public void update(DTNHost thisHost) {
        // lastCheck = 0,LIMIT_TW = 900 detik / 15 menit
        // Menghitung Prioritas setiap 15 menit sekali atau TWCur
        if (SimClock.getTime() - lastCheck > LIMIT_TW) {
            calculatePriority(thisHost);
            lastCheck = SimClock.getTime();
        }
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new ChitChatPrioritized(this);
    }

    private ChitChatPrioritized getOtherDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ChitChatPrioritized) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public Map<String, Double> getTrs() {
        return this.transcient;
    }
}
