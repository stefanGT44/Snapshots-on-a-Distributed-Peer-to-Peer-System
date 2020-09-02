package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.RejectResponseMessage;
import servent.message.snapshot.RejectCollectorMessage;
import servent.message.util.MessageUtil;

public class RejectResponseHandler implements MessageHandler {

	private Message clientMessage;
	private LaiYangBitcakeManager bitcakeManager;
	private SnapshotCollectorWorker snapshotCollector;

	public RejectResponseHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = snapshotCollector.getBitcakeManager();
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.REJECT_RESPONSE_MESSAGE) {
			synchronized (AppConfig.colorLock) {
				RejectResponseMessage msg = (RejectResponseMessage)clientMessage;
				
				bitcakeManager.addBorder(msg.getResult());
				bitcakeManager.markRecievedMessage(clientMessage.getOriginalSenderInfo().getId());
				AppConfig.timestampedStandardPrint("Dobio sam istoriju od granicnog cvora [" + msg.getOriginalSenderInfo().getId() + "]");
				
				if (bitcakeManager.getParent() == null) {
					if (bitcakeManager.readyToSendResult()) {
						//bitcakeManager.resetSKparameters();
						AppConfig.timestampedStandardPrint("Krece racunanje REZULTATA ZA OVAJ REGION");
						snapshotCollector.resultsReady = true;
					}
				} else {
					//ako su sve komsije odgovorile, saljemo odgovor roditelju
					if (bitcakeManager.readyToSendResult()) {
						bitcakeManager.sendResultsToParent();
						//bitcakeManager.resetSKparameters();
					}
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("RejectResponseHandler got: " + clientMessage);
		}
	}

}
