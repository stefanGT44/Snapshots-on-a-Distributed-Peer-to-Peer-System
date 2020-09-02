package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class ResetMessage extends BasicMessage {
	
	public ResetMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
		super(MessageType.RESET_MESSAGE, originalSenderInfo, receiverInfo);
	}

	private static final long serialVersionUID = 1L;

}
