/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.RoutingDecisionEngine;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently Connected
 * Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class SprayAndWaitKu implements RoutingDecisionEngine {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "DecisionSprayAndWaitRouter";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";

    protected int initialNrofCopies;
    protected boolean isBinary;

    public SprayAndWaitKu(Settings s) {
//        s = new Settings(SPRAYANDWAIT_NS);
        initialNrofCopies = s.getInt(NROF_COPIES);
        isBinary = s.getBoolean(BINARY_MODE);
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected SprayAndWaitKu(SprayAndWaitKu r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
    }

    @Override
    public SprayAndWaitKu replicate() {
        return new SprayAndWaitKu(this);
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    // sama dengan create new message
    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    // method yang dipanggil ketika kita menerima pesan, sama dengan message transferred?
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        } else {
            nrofCopies = 1;
        }
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        return m.getTo() == aHost;
    }

    // kalau bukan tujuan brati tidak perlu menyimpan
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
//        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
//        if (isBinary) {
//            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
//        } else {
//            nrofCopies = 1;
//        }
//        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        return m.getTo() != thisHost;
    }

    // apakah harus mengirimkan pesan ini ke host yang ditemui?
    // kalau otherHost adalah tujuan, pesannya dikirim, kalau tidak, cek nrofCopies m
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }
        
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (nrofCopies > 1) {
            return true;
        } else {
            return false;
        }
    }

    // dilakukan when transfer done, setelah transfer tidak perlu hapus 
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies /= 2;
        } else {
            nrofCopies--;
        }
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    @Override
    public void update(DTNHost thisHost) {}
}
