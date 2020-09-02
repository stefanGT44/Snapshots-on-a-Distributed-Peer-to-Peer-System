package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class RejectCollectorMessage extends BasicMessage {

	private static final long serialVersionUID = 1L;
	private LYSnapshotResult result;
	
	public RejectCollectorMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, LYSnapshotResult result) {
		super(MessageType.REJECT_COLLECTOR_MESSAGE, originalSenderInfo, receiverInfo);
		this.result = result;
	}
	
	@Override
	public Message setRedColor() {
		return new RejectCollectorMessage(getOriginalSenderInfo(), getReceiverInfo(), result);
	}
	
	public LYSnapshotResult getResult() {
		return result;
	}
	
}
