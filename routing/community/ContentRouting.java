/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently Connected
 * Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class ContentRouting implements RoutingDecisionEngine {

    public static final String MSG_CONTENT_PROPERTY = "Content";

    protected String id;
    protected Connection connection;

    public ContentRouting(Settings s) {
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected ContentRouting(ContentRouting r) {
    }

    @Override
    public ContentRouting replicate() {
        return new ContentRouting(this);
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        this.id = thisHost.getGroupId();
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        this.connection = con;
    }

    // sama dengan create new message
    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_CONTENT_PROPERTY, id);
        return true;
    }

    // method yang dipanggil ketika kita menerima pesan, sama dengan message transferred?
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        if (m.getProperty(MSG_CONTENT_PROPERTY) == aHost.getGroupId()) {
            return true;
        } else {
            return false;
        }
    }

    // kalau bukan tujuan brati tidak perlu menyimpan
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        if (m.getProperty(MSG_CONTENT_PROPERTY) != thisHost.getGroupId()) {
            return true;
        } else {
            return false;
        }
    }

    // apakah harus mengirimkan pesan ini ke host yang ditemui?
    // kalau otherHost adalah tujuan, pesannya dikirim, kalau tidak, cek nrofCopies m
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getProperty(MSG_CONTENT_PROPERTY) == otherHost.getGroupId()) {
            return true;
        } else {
            return false;
        }
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

    private ContentRouting getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ContentRouting) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }
}
