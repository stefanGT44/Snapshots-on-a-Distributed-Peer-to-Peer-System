package servent.message;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;

public class RejectResponseMessage extends BasicMessage {

	private static final long serialVersionUID = 1L;
	private LYSnapshotResult result;
	
	public RejectResponseMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, LYSnapshotResult result) {
		super(MessageType.REJECT_RESPONSE_MESSAGE, originalSenderInfo, receiverInfo);
		this.result = result;
	}	
	
	@Override
	public Message setRedColor() {
		return new RejectResponseMessage(getOriginalSenderInfo(), getReceiverInfo(), result);
	}
	
	public LYSnapshotResult getResult() {
		return result;
	}

}
