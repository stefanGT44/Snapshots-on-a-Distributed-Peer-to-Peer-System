package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class RejectParentHandler implements MessageHandler {
	
	private Message clientMessage;
	private LaiYangBitcakeManager bitcakeManager;
	private SnapshotCollectorWorker snapshotCollector;
	
	public RejectParentHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = snapshotCollector.getBitcakeManager();
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.REJECT_PARENT_MESSAGE) {
			synchronized(AppConfig.colorLock) {
				
				AppConfig.timestampedStandardPrint("Odbio nas cvor [" + clientMessage.getOriginalSenderInfo().getId() + "], vec ima roditelja");
				
				//obelezimo da je stigao odgovor
				bitcakeManager.markRecievedMessage(clientMessage.getOriginalSenderInfo().getId());
				
				//ako smo mi inicijator i svi su odgovorili, krenemo racunanje rezultata za nas region
				if (bitcakeManager.getParent() == null) {
					if (bitcakeManager.readyToSendResult()) {
						//bitcakeManager.resetSKparameters();
						snapshotCollector.resultsReady = true;
						AppConfig.timestampedStandardPrint("Krece racunanje REZULTATA ZA OVAJ REGION");
					}
				} else {//ako su sva deca odgovorila, saljemo rezultate roditelju
					if (bitcakeManager.readyToSendResult()) {
						bitcakeManager.sendResultsToParent();
						//bitcakeManager.resetSKparameters();
					}
				}
				
			}
		} else {
			AppConfig.timestampedErrorPrint("RejectParentHandler got: " + clientMessage);
		}
	}

}
