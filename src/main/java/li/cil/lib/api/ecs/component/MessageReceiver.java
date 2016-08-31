package li.cil.lib.api.ecs.component;

import io.netty.buffer.ByteBuf;

/**
 * Components implementing this interface may receive data packets sent from
 * the remote side (i.e. client can receive messages from the server, server
 * can receive messages from the client).
 * <p>
 * While it is possible to send these messages manually, it is generally
 * recommended to use the utility methods in <code>AbstractComponent</code>.
 */
public interface MessageReceiver extends Component {
    /**
     * Called when data is received from the other side.
     * <p>
     * Note that this is called from the network handler thread.
     *
     * @param data the data that was received.
     */
    void handleComponentData(final ByteBuf data);
}
