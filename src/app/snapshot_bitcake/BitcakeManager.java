package app.snapshot_bitcake;

/**
 * Describes a bitcake manager. These classes will have the methods
 * for handling snapshot recording and sending info to a collector.
 * 
 * @author stefanGT44
 *
 */
public interface BitcakeManager {

	public void takeSomeBitcakes(int amount);
	public void addSomeBitcakes(int amount);
	public int getCurrentBitcakeAmount();
	
}
