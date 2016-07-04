package li.cil.lib.network.handler;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public abstract class AbstractMessageHandlerNoResponse<REQ extends IMessage> extends AbstractMessageHandler<REQ, IMessage> {
}
