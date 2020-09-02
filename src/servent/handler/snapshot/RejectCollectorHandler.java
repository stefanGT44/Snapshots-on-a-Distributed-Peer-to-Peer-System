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

public class RejectCollectorHandler implements MessageHandler {

	private Message clientMessage;
	private LaiYangBitcakeManager bitcakeManager;
	private SnapshotCollectorWorker snapshotCollector;
	
	public RejectCollectorHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = snapshotCollector.getBitcakeManager();
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.REJECT_COLLECTOR_MESSAGE) {
			synchronized(AppConfig.colorLock) {
			RejectCollectorMessage msg = (RejectCollectorMessage)clientMessage;
			
			AppConfig.timestampedStandardPrint("Odbio nas cvor [" + clientMessage.getOriginalSenderInfo().getId() + "], vec pripada drugom regionu, saljem mu svoju istoriju");
			
			//dodati border - sender kod nas, obeleziti da je odgovor od suseda stigao
			bitcakeManager.addBorder(msg.getResult());
			
			//TREBA ODGOVORITI POSILJAOCU 
			RejectResponseMessage resp = new RejectResponseMessage(AppConfig.myServentInfo, msg.getOriginalSenderInfo(),
					bitcakeManager.getOtherRegionResult(msg.getResult().getMaster(), msg.getOriginalSenderInfo().getId()));
			//RejectResponseMessage resp = new RejectResponseMessage(AppConfig.myServentInfo, msg.getOriginalSenderInfo(), bitcakeManager.getResultById(AppConfig.myServentInfo.getId()));
			MessageUtil.sendMessage(resp);
			
			bitcakeManager.markRecievedMessage(clientMessage.getOriginalSenderInfo().getId());
			
			//ako smo mi inicijator, i ako samo ovaj odgovor samo cekali
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
			AppConfig.timestampedErrorPrint("RejectCollectorHandler got: " + clientMessage);
		}
	}

}
