package com.uc3m.main;

/**
 * Abstract representation of the MIDI ShortMessage class
 * Used to handle the short message's basic information needed to evolve the individuals
 * @author antonio
 *
 */
public class SimplifiedShortMessage{

	private int type;	// NOTE_ON - NOTE_OFF
	private int key;	// 0 ... 127
	private long tick;	// 0 ... TrackMax (From the original track)
	private int channel;
	private long fitness;
	
	/**
	 * Constructor
	 * 
	 * @param type	NOTE_ON or NOTE_OFF
	 * @param key	Key pressed (0..127)
	 * @param tick	Tick (Timing)
	 * @param channel	Fixed channel
	 * 
	 */
	public SimplifiedShortMessage(int type, int key, long tick, int channel) {
		super();
		this.type = type;
		this.key = key;
		this.tick = tick;
		this.channel = channel;
	}
	
	/**
	 * Constructor from another SimplifiedShortMessage (Copy)
	 * @param ssm
	 */
	public SimplifiedShortMessage(SimplifiedShortMessage ssm){
		this.type = ssm.type;
		this.key = ssm.getKey();
		this.tick = ssm.getTick();
		this.channel = ssm.getChannel();
		this.fitness = ssm.getFitness();
	}
	
	/**
	 * Fitness calculation (Zero Fitness)
	 * Calculated adding the absolute value of the difference between the source type and the generated type
	 * plus the absolute value of the difference between the source key and the generated key
	 * plus the absolute value of the difference between the source tick and the generated tick
	 * s: source g: generated
	 * fitness = |s_type - g_type| + |s_key - g_key| + |s_tick - g_tick|
	 * @param mode
	 * @param ssm
	 * @return
	 */
	public long calculateFitness(SimplifiedShortMessage ssm){
		int noteDiff = ssm.getType() - type;
		int keyDiff = ssm.getKey() - key;
		long tickDiff = ssm.getTick() - tick;
		fitness = Math.abs(noteDiff) + Math.abs(keyDiff) + Math.abs(tickDiff);

		return fitness;
	}
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getKey() {
		return key;
	}
	public void setKey(int key) {
		this.key = key;
	}
	public long getTick() {
		return tick;
	}
	public void setTick(long tick) {
		this.tick = tick;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public long getFitness() {
		return fitness;
	}

	public void setFitness(long fitness) {
		this.fitness = fitness;
	}

}
