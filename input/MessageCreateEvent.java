/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import core.Coord;
import core.DTNHost;
import core.Message;
import core.SimScenario;
import core.World;
import java.util.List;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {

    private int size;
    private int responseSize;

    private boolean burst;

    /**
     * Creates a message creation event with a optional response request
     *
     * @param from The creator of the message
     * @param to Where the message is destined to
     * @param id ID of the message
     * @param size Size of the message
     * @param responseSize Size of the requested response message or 0 if no
     * response is requested
     * @param time Time, when the message is created
     */
    public MessageCreateEvent(int from, int to, String id, int size,
            int responseSize, double time, boolean burst) {
        super(from, to, id, time);
        this.size = size;
        this.responseSize = responseSize;
        this.burst = burst;
    }

    /**
     * Creates the message this event represents.
     */
    @Override
    public void processEvent(World world) {
        if (burst) {
            // get random node inside the crowd
            DTNHost trigger;
            double locXf = 0, locYf = 0;
            do {
                trigger = world.getNodeByAddress(this.fromAddr);
                locXf = trigger.getLocation().getX();
                locYf = trigger.getLocation().getY();
            } while (locXf > world.getSizeX() * 2 / 3 && locXf < world.getSizeX() * 1 / 3
                        && locYf > world.getSizeY() * 2 / 3 && locYf < world.getSizeY() * 1 / 3);
            
            // create message from several hosts around random node
            List<DTNHost> nodes = SimScenario.getInstance().getHosts();
            int n = 0;
            int range = 100;
            for (DTNHost h : nodes) { 
                double locX = h.getLocation().getX();
                double locY = h.getLocation().getY();
                if (locX < (locXf+range) && locX > (locXf-range)
                        && locY < (locYf+range) && locY > (locYf-range)) {
                    DTNHost to = null;
                    DTNHost from = h;
                    Message m = new Message(from, to, this.id+"-"+n, this.size);
                    m.setResponseSize(this.responseSize);
                    from.createNewMessage(m);
                   n++;
                }
            }
        } else {
            DTNHost to = null;
            DTNHost from = world.getNodeByAddress(this.fromAddr);

            Message m = new Message(from, to, this.id, this.size);
            m.setResponseSize(this.responseSize);
            from.createNewMessage(m);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->" + toAddr + "] "
                + "size:" + size + " CREATE";
    }
}
