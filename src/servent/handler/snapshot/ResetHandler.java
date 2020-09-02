package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.SumMessage;

public class ResetHandler implements MessageHandler {

	private Message clientMessage;
	private LaiYangBitcakeManager bitcakeManager;
	
	public ResetHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = snapshotCollector.getBitcakeManager();
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.RESET_MESSAGE) {
			
			bitcakeManager.sendResetToChildren();
			bitcakeManager.resetSKparameters();
			
		} else {
			AppConfig.timestampedErrorPrint("ResetHandler got: " + clientMessage);
		}
	}

}
