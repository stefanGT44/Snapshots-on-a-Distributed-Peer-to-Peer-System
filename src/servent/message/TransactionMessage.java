package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author stefanGT44
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;
	
	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
		this.bitcakeManager = bitcakeManager;
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	
	@Override
	public Message setRedColor() {
		return new TransactionMessage(getOriginalSenderInfo(), getReceiverInfo(), Integer.parseInt(getMessageText()), this.bitcakeManager);
	}
	
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());
		//ako je poruka bila bela kad smo je kreirali
		bitcakeManager.takeSomeBitcakes(amount);
		//compare test bez icega
		//System.out.println("PRE IFA ZA " + getReceiverInfo().getId() + " amount = " + amount);
		if (bitcakeManager instanceof LaiYangBitcakeManager) {
			LaiYangBitcakeManager lyBitcakeManager = (LaiYangBitcakeManager)bitcakeManager;
			
			//AppConfig.timestampedStandardPrint("POKUSAVAM u istoriju da sam dao cvoru " + getReceiverInfo().getId() + " " + amount);
			lyBitcakeManager.recordGiveTransaction(getReceiverInfo().getId(), amount);
			//AppConfig.timestampedStandardPrint("ZAPISANO u istoriju da sam dao cvoru " + getReceiverInfo().getId() + " " + amount);
		}
	}
}
