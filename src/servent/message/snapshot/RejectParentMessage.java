package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class RejectParentMessage extends BasicMessage {

	public RejectParentMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
		super(MessageType.REJECT_PARENT_MESSAGE, originalSenderInfo, receiverInfo);
	}

	private static final long serialVersionUID = 1L;

	
	
}
