/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.SimScenario;
import core.Tuple;
import core.UpdateListener;
import java.util.HashSet;
import java.util.Set;
import routing.*;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P>
 * <strong>Note:</strong> if some statistics could not be created (e.g. overhead
 * ratio if no messages were delivered) "NaN" is reported for double values and
 * zero for integer median(s).
 */
public class MessageStatsReportMod extends Report implements MessageListener, UpdateListener {

    private Map<String, Double> creationTimes;
    private List<Double> latencies;
    private List<Integer> hopCounts;
    private List<Double> msgBufferTime;
    private List<Double> rtt; // round trip times
    private List<Double> delivery;
    private List<String> deliveryInterval;

    private Map<String, Integer> nrofNodeInterest; //menyimpan jumlah node dengan interest tertentu
    private Map<String, List<Double>> latencyperMsg; //menyimpan jumlah node dengan interest tertentu
    private Map<String, Set<DTNHost>> nrofMsgInterest; //menyimpan masing-masing jumlah pesan
    private Map<String, Message> messages; //menyimpan pesan yang dibuat

    private int interval = 6000;
    private double lastRecord = 0.0;

    private int nrofDropped;
    private int nrofRemoved;
    private int nrofStarted;
    private int nrofAborted;
    private int nrofRelayed;
    private int nrofCreated;
    private int nrofResponseReqCreated;
    private int nrofResponseDelivered;
    private int nrofDelivered;

    private String M_TOPIC = "Message Topic";

    /**
     * Constructor.
     */
    public MessageStatsReportMod() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.creationTimes = new HashMap<String, Double>();
        this.latencies = new ArrayList<Double>();
        this.msgBufferTime = new ArrayList<Double>();
        this.hopCounts = new ArrayList<Integer>();
        this.rtt = new ArrayList<Double>();
        this.delivery = new ArrayList<Double>();
        this.deliveryInterval = new ArrayList<String>();

        this.nrofDropped = 0;
        this.nrofRemoved = 0;
        this.nrofStarted = 0;
        this.nrofAborted = 0;
        this.nrofRelayed = 0;
        this.nrofCreated = 0;
        this.nrofResponseReqCreated = 0;
        this.nrofResponseDelivered = 0;
        this.nrofDelivered = 0;

        this.nrofMsgInterest = new HashMap<String, Set<DTNHost>>();
        this.messages = new HashMap<String, Message>();
        this.latencyperMsg = new HashMap<String, List<Double>>();
        this.nrofNodeInterest = new HashMap<String, Integer>();
        this.nrofNodeInterest.put("Sport", 0);
        this.nrofNodeInterest.put("Cooking", 0);
        this.nrofNodeInterest.put("Film", 0);
        this.nrofNodeInterest.put("Traveling", 0);
        this.nrofNodeInterest.put("Music", 0);
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if (dropped) {
            this.nrofDropped++;
        } else {
            this.nrofRemoved++;
        }

        this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofAborted++;
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
            boolean finalTarget) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofRelayed++;

        Tuple<String, String> topic = (Tuple<String, String>) m.getProperty(M_TOPIC);
        if (to.getSocialProfile().contains(topic.getKey())) {
            Set<DTNHost> host;
            if (nrofMsgInterest.containsKey(m.getId())) {
                host = new HashSet<>(nrofMsgInterest.get(m.getId()));
            } else {
                host = new HashSet<>();
            }
            
            List<Double> latency;
            if (latencyperMsg.containsKey(m.getId())) {
                latency = new ArrayList<>(latencyperMsg.get(m.getId()));
            } else {
                latency = new ArrayList<>();
            }

            if (!host.contains(to)) {
                latency.add(getSimTime() - this.creationTimes.get(m.getId()));
            }
            
            host.add(to);
            nrofMsgInterest.put(m.getId(), host);
            latencyperMsg.put(m.getId(), latency);
            
            this.nrofDelivered++;
            this.hopCounts.add(m.getHops().size() - 1);

            if (m.isResponse()) {
                this.rtt.add(getSimTime() - m.getRequest().getCreationTime());
                this.nrofResponseDelivered++;
            }
        }
    }

    public void newMessage(Message m) {
        if (isWarmup()) {
            addWarmupID(m.getId());
            return;
        }

        this.creationTimes.put(m.getId(), getSimTime());
        this.nrofCreated++;
        this.messages.put(m.getId(), m);
        if (m.getResponseSize() > 0) {
            this.nrofResponseReqCreated++;
        }
    }

    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofStarted++;
    }

    @Override
    public void done() {
        write("Message stats for scenario " + getScenarioName()
                + "\nsim_time: " + format(getSimTime()));

        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (DTNHost h : nodes) {
            for (String interest : nrofNodeInterest.keySet()) {
                if (h.getSocialProfile().contains(interest)) {
                    int n = nrofNodeInterest.get(interest) + 1;
                    nrofNodeInterest.put(interest, n);
                }
            }
        }

//        write(this.deliveryInterval+"");
        for (Map.Entry<String, Set<DTNHost>> entry : nrofMsgInterest.entrySet()) {
            Message m = this.messages.get(entry.getKey());
            Tuple<String, String> topic = (Tuple<String, String>) m.getProperty(M_TOPIC);

            double nrofEachDelivered = entry.getValue().size();
            double nrofNode = this.nrofNodeInterest.get(topic.getKey());

            double deliveryEachMsg = nrofEachDelivered / nrofNode;

            this.delivery.add(deliveryEachMsg);
        }
        write("Delivery = " + getAverage(this.delivery));
        
        
        for (Map.Entry<String, List<Double>> entry : latencyperMsg.entrySet()) {            
            double latency = 0.0;
            for (Double l : entry.getValue()) {
                latency += l;
            }

            double latencyEachMsg = latency / entry.getValue().size();
                    
            this.latencies.add(latencyEachMsg);
        }
        write("Latency = " + getAverage(this.latencies));

        String statsText = "created: " + this.nrofCreated
                + "\nstarted: " + this.nrofStarted
                + "\nrelayed: " + this.nrofRelayed
                + "\naborted: " + this.nrofAborted
                + "\ndropped: " + this.nrofDropped
                + "\nremoved: " + this.nrofRemoved
                + "\ndelivered: " + this.nrofDelivered;
        write(statsText);
        super.done();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
//        if (SimClock.getTime() - lastRecord >= interval) {
//            lastRecord = SimClock.getTime();
//            this.delivery.clear();
//            for (Map.Entry<String, Set<DTNHost>> entry : nrofMsgInterest.entrySet()) {
//                Message m = this.messages.get(entry.getKey());
//                Tuple<String, String> topic = (Tuple<String, String>) m.getProperty(M_TOPIC);
//
//                double nrofEachDelivered = entry.getValue().size();
//                double nrofNode;
//                if (entry.getKey().contains("S")) {
//                    nrofNode = 500;
//                } else {
//                    nrofNode = this.nrofNodeInterest.get(topic.getKey());
//                }
//
//                double deliveryEachMsg = nrofEachDelivered / nrofNode;
//                System.out.println(deliveryEachMsg);
//                this.delivery.add(deliveryEachMsg);
//            }
//            this.deliveryInterval.add(getAverage(this.delivery));
//        }
    }

}
