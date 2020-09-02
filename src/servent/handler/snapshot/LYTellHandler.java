package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class LYTellHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollectorWorker snapshotCollector;
	
	public LYTellHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;
			
			synchronized(AppConfig.colorLock) {
				LaiYangBitcakeManager bitcakManager = snapshotCollector.getBitcakeManager();
				
				//dodajem rezultate
				bitcakManager.addResults(lyTellMessage.getResults(), lyTellMessage.getBorder());
				
				//oznacavam da sam dobio rezultat od deteta
				bitcakManager.markRecievedMessage(lyTellMessage.getOriginalSenderInfo().getId());
				
				//dodajem ga kao svoj child
				bitcakManager.addChild(lyTellMessage.getOriginalSenderInfo().getId());
				
				AppConfig.timestampedStandardPrint("Dobio rezultate od [" + clientMessage.getOriginalSenderInfo().getId() + "]");
				
				//ako smo spremni, poslati rezultat roditelju i resetovati parametre, ukoliko nemamo roditelja onda smo mi inicijator
				if (bitcakManager.getParent() != null) {
					if (bitcakManager.readyToSendResult()) {
						//ako imamo roditelja, i spremni smo, saljemo rezultat
						bitcakManager.sendResultsToParent();
						//bitcakManager.resetSKparameters();
					}
				} else {
					//mi smo inicijator
					
					snapshotCollector.addAllGatheredResults(lyTellMessage.getResults());
					snapshotCollector.addToBorder(lyTellMessage.getBorder());
					
					AppConfig.timestampedStandardPrint("Mi smo inicijator, dodajemo pristigle rezultate");
					
					//ako su ovo poslednji rezultati koji su nedostajali
					if (bitcakManager.readyToSendResult()) {
						//spremni smo da kalkulisemo rezultat
						//bitcakManager.resetSKparameters();
						AppConfig.timestampedStandardPrint("Krece racunanje REZULTATA ZA OVAJ REGION");
						snapshotCollector.resultsReady = true;
					}
				}
				
			}
			
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
