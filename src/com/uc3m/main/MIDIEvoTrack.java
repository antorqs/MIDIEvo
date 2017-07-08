package com.uc3m.main;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Abstract (and Biological) representation of a MIDI Track.
 * This represents one individual
 * 
 * @author antonio
 *
 */
public class MIDIEvoTrack implements Comparable<MIDIEvoTrack>{

	private ArrayList<SimplifiedShortMessage> simplifiedShortMessages;	//List of shortMessages
	private ArrayList<Long> originalMessagesTicks;						//Timing of the original track's messages
	private ArrayList<MidiMessage> originalMessages;					//Original track's messages (Not Short Messages)
	private long maxTick = 0;			//Last Short Message Tick
	private int maxSize;				//Number of Short Messages
	private int channel = -1;			//Track's channel
	private boolean hasTempo = false;	//This track has a tempo message
	private boolean hasDifferentChannels = false;	//This track has different channels
	private static final int MIN_KEY = 0;		//Minimum key value
	private static final int MAX_KEY = 127;		//Maximum key value
	private static final int MUTATE_NOTE = 0;	//Mutation mode: Command
	private static final int MUTATE_KEY = 1;	//Mutation mode: Key
	private static final int MUTATE_TICK = 2;	//Mutation mode: Tick
	private static final int DEFAULT_VELOCITY = 80;	//Default velocity (How hard a key is pressed .. Volume)
	private final int[] commands = {ShortMessage.NOTE_OFF, ShortMessage.NOTE_ON}; //Command types
	private BigInteger totalFitness = BigInteger.valueOf(0);	//Sum of all short messages fitness

	/**
	 * Constructor: Creates a base track (No messages)
	 * @param maxSize	Number of messages
	 * @param maxTick	Last message tick
	 * @param channel	Channel
	 * @param origMessages	List of original track's messages
	 * @param origMessagesTicks List of original track's ticks
	 */
	public MIDIEvoTrack(int maxSize, long maxTick, int channel, 
			ArrayList<MidiMessage> origMessages, ArrayList<Long> origMessagesTicks) {
		this.simplifiedShortMessages = new ArrayList<SimplifiedShortMessage>();
		this.maxSize = maxSize;
		this.maxTick = maxTick;
		this.originalMessages = origMessages;
		this.originalMessagesTicks = origMessagesTicks;
		this.channel = channel;
	}
	
	/**
	 * Constructor: Creates a base track from anorhter MIDIEvoTrack
	 * @param met
	 */
	public MIDIEvoTrack(MIDIEvoTrack met){
		this(met.getMaxSize(), met.getMaxTick(), met.getChannel(), 
				met.getOriginalMessages(), met.getOriginalMessagesTicks());
	}
	
	/**
	 * Constructor: Create a MIDIEvoTrack from a source MIDI Track
	 * @param track
	 */
	public MIDIEvoTrack(Track track) {
		try {
			preprocessTrack(track);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates maxSize random simplifiedShortMessages to populate the MIDIEvoTrack
	 * Select a random command (NOTE_ON, NOTE_OFF)
	 * Select a random Key (MIN_KEY..MAX_KEY)
	 * Select a random Tick (0..MaxTick)
	 * Instantiate a new SimplifiedShortMessage with those values and add it to the track's list.
	 */
	public void createRandom(){
		createRandom(null);
	}
	public void createRandom(MIDIEvoTrack track){
		for (int i=0; i < maxSize; i++) {
			int command = commands[ThreadLocalRandom.current().nextInt(0, 2)];
			int key = ThreadLocalRandom.current().nextInt(MIN_KEY, MAX_KEY + 1);
			long tick = ThreadLocalRandom.current().nextLong(maxTick + 1);
			
			SimplifiedShortMessage ssm = new SimplifiedShortMessage(command, key, tick, channel);
			if(track != null)
				ssm.calculateFitness(track.getSimplifiedShortMessages().get(i));
			this.simplifiedShortMessages.add(ssm);
		}
	}
	
	/**
	 * Generate a MIDIEvoTrack form a MIDI Track.
	 * For every ShortMessage creates a new SimplifiedShortMessage
	 * The rest of the MIDI Messages are added to the originalMessages array list.
	 * 
	 * @param track
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public void preprocessTrack(Track track) 
			throws InvalidMidiDataException, IOException{
		
		simplifiedShortMessages = new ArrayList<SimplifiedShortMessage>();
		originalMessages = new ArrayList<MidiMessage>();
		originalMessagesTicks = new ArrayList<Long>();
		
		for (int i=0; i < track.size(); i++) { 

			MidiEvent event = track.get(i);
			MidiMessage message = event.getMessage();
			long tick = event.getTick();
			
			//If the event is a short message
			if (message instanceof ShortMessage) {
				if(tick > maxTick)
					maxTick = tick;
				ShortMessage sm = (ShortMessage) message;
                int channel = sm.getChannel();
                if(this.channel == -1)
                	this.channel = channel;
                else if(this.channel != channel)
                	this.hasDifferentChannels = true;

                int key = sm.getData1();
                
                if (sm.getCommand() == ShortMessage.NOTE_ON 
                		|| sm.getCommand() == ShortMessage.NOTE_OFF) {
                	
                	SimplifiedShortMessage se = new SimplifiedShortMessage(sm.getCommand(), key, tick, channel);
                	simplifiedShortMessages.add(se);
                }else{
                	originalMessages.add(message);
            		originalMessagesTicks.add(tick);
                }
				
			}else{
				//Checking for a Tempo Message
				if(message instanceof MetaMessage){
					MetaMessage metaMessage = (MetaMessage) message;
					if(metaMessage.getType() == 81)
						this.hasTempo = true;
				}
				//All other messages (not short messages) are copied to the MIDIEvoTrack
        		originalMessages.add(message);
        		originalMessagesTicks.add(tick);
			}
		}
		maxSize = simplifiedShortMessages.size();
	}

	/**
	 * Track's fitness calculation
	 * For every Short Message calculate it's fitness comparing it to the original MIDIEvoTrack
	 * Then sum all short message's fitness
	 * The bigger the fitness, the worst the individual
	 * @param originalTrack
	 * @return
	 */
	public BigInteger calculateFitness(){
		return calculateFitness(null, false);
	}
	
	public BigInteger calculateFitness(MIDIEvoTrack originalTrack){
		return calculateFitness(originalTrack, true);
	}
	
	public BigInteger calculateFitness(MIDIEvoTrack originalTrack, boolean full){

		BigInteger fitness = BigInteger.valueOf(0);
		for (int i=0; i < maxSize; i++) {
			long noteFitness;
			if(full){
				noteFitness = simplifiedShortMessages.get(i).
					calculateFitness(originalTrack.getSimplifiedShortMessages().get(i));
			}else{
				noteFitness = simplifiedShortMessages.get(i).getFitness();
			}
			
			fitness = fitness.add(BigInteger.valueOf(noteFitness));
		}
		
		//Update and return the fitness
		totalFitness = fitness;
		return totalFitness;
	}
	
	/**
	 * Copy all ticks from the original track to the individual (For EASY_MODE)
	 * @param origTrack
	 */
	public void copyTicks(MIDIEvoTrack origTrack){
		
		for(int i=0; i<origTrack.getSize();i++){
			simplifiedShortMessages.get(i).setTick(
					origTrack.getSimplifiedShortMessages().get(i).getTick());
		}
	}
	
	/**
	 * Create a MIDI Track from the MIDIEvoTrack
	 * (Reverse process)
	 * @param sequence
	 * @return
	 */
	public Track convertToTrack(Sequence sequence){
		Track track = sequence.createTrack();
		int i = 0;
		//Add every other message.
		for (MidiMessage ev : originalMessages) {
			MidiEvent event = new MidiEvent(ev, originalMessagesTicks.get(i));
			i++;
			track.add(event);
		}
		
		//Create a MIDI ShortMessage from every SimplifiedShortMessage
		for (SimplifiedShortMessage ssm : simplifiedShortMessages) {
			MidiEvent event = createNoteEvent(ssm.getType(), ssm.getKey(), DEFAULT_VELOCITY, ssm.getTick(), ssm.getChannel());
			track.add(event);
		}
		
		return track;
	}
	
	/**
	 * Creates a MIDIEvent containing a ShortMessage
	 * @param nCommand
	 * @param nKey
	 * @param nVelocity
	 * @param lTick
	 * @param channel
	 * @return
	 */
	private MidiEvent createNoteEvent(int nCommand,
			 int nKey,
			 int nVelocity,
			 long lTick,
			 int channel){
		ShortMessage	message = new ShortMessage();
		try{
			message.setMessage(nCommand,
			channel,	
			nKey,
			nVelocity);
		}
		catch (InvalidMidiDataException e){
			e.printStackTrace();
		}
		MidiEvent	event = new MidiEvent(message, lTick);
		return event;
	}
	
	/**
	 * Mutation Method
	 * Select a random part of the individuals to mutate (MUTATE_NOTE, MUTATE_KEY or MUTATE_TICK)
	 * Then for every SimplifiedShortMessage flip a coin (Random 0..1)
	 * if 1 then change the selected part (Note, Key or Tick) for a randomly generated new one.
	 * if 0 do nothing to that simplifiedShortMessage
	 * @param mode
	 */
	public void mutation(int mode, double mrate, MIDIEvoTrack originalTrack){
		
		int type = 0;
		if(mode == MIDIEvo.EASY_MODE)
			type = ThreadLocalRandom.current().nextInt(0, 2);
		else
			type = ThreadLocalRandom.current().nextInt(0, 3);
		//boolean isMutable = ThreadLocalRandom.current().nextInt(0, 5) < 6;
		int ii = 0;
		//if(isMutable){
			for (SimplifiedShortMessage ssm : simplifiedShortMessages) {
				
				double mutationPercentage = ThreadLocalRandom.current().nextDouble();
	
				if(mutationPercentage < mrate){
					if(type == MUTATE_NOTE){
						int command = commands[ThreadLocalRandom.current().nextInt(0, 2)];
						ssm.setType(command);
					}else if(type == MUTATE_KEY){
						int key = ThreadLocalRandom.current().nextInt(MIN_KEY, MAX_KEY + 1);
						ssm.setKey(key);
					}else if(type==MUTATE_TICK && mode==MIDIEvo.HARD_MODE){
						long tick = ThreadLocalRandom.current().nextLong(maxTick + 1);
						ssm.setTick(tick);
					}
					ssm.calculateFitness(originalTrack.getSimplifiedShortMessages().get(ii));
				}
				ii++;
			}
		//}
	}
	
	public void setOriginalEvents(ArrayList<SimplifiedShortMessage> originalEvents) {
		this.simplifiedShortMessages = originalEvents;
	}


	public long getMaxTick() {
		return maxTick;
	}


	public void setMaxTick(long maxTick) {
		this.maxTick = maxTick;
	}


	public boolean isHasTempo() {
		return hasTempo;
	}


	public void setHasTempo(boolean hasTempo) {
		this.hasTempo = hasTempo;
	}
	
	public int getSize(){
		return simplifiedShortMessages.size();
	}

	public int getMaxSize(){
		return this.maxSize;
	}

	public void setMaxSize(int size) {
		this.maxSize = size;
	}

	public boolean isHasDifferentChannels() {
		return hasDifferentChannels;
	}

	public void setHasDifferentChannels(boolean hasDifferentChannels) {
		this.hasDifferentChannels = hasDifferentChannels;
	}

	public ArrayList<SimplifiedShortMessage> getSimplifiedShortMessages() {
		return simplifiedShortMessages;
	}

	public void setSimplifiedShortMessages(ArrayList<SimplifiedShortMessage> simplifiedShortMessages) {
		this.simplifiedShortMessages = simplifiedShortMessages;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public ArrayList<MidiMessage> getOriginalMessages() {
		return originalMessages;
	}

	public void setOriginalMessages(ArrayList<MidiMessage> originalMessages) {
		this.originalMessages = originalMessages;
	}

	public BigInteger getTotalFitness() {
		return totalFitness;
	}

	public void setTotalFitness(BigInteger totalFitness) {
		this.totalFitness = totalFitness;
	}
	
	public ArrayList<Long> getOriginalMessagesTicks() {
		return originalMessagesTicks;
	}

	public void setOriginalMessagesTicks(ArrayList<Long> originalMessagesTicks) {
		this.originalMessagesTicks = originalMessagesTicks;
	}
	
	/**
	 * CompareTo method to sort the MIDIEvoTrack Array Lists by fitness.
	 */
	@Override
	public int compareTo(MIDIEvoTrack midievotrack) {
		
		return totalFitness.compareTo(midievotrack.getTotalFitness());
	}
	
	public boolean isEqualTo(MIDIEvoTrack midievotrack) {
		int ii = 0;
		for (SimplifiedShortMessage ssm : simplifiedShortMessages) {
			
			if(ssm.getKey() != midievotrack.getSimplifiedShortMessages().get(ii).getKey()){
				return false;
			}
			if(ssm.getTick() != midievotrack.getSimplifiedShortMessages().get(ii).getTick())
				return false;
			if(ssm.getType() != midievotrack.getSimplifiedShortMessages().get(ii).getType())
				return false;
			ii++;
		}
		return true;
	}
	
	/**
	 * toString Method
	 */
	@Override
	public String toString(){
		String output = "";
		for (SimplifiedShortMessage ssm : simplifiedShortMessages) {
			output += "Type = "+ssm.getType()+"\t";
			output += "Key = "+ssm.getKey()+"\t";
			output += "Tick = "+ssm.getTick()+"\t";
			output += "Fitness = "+ssm.getFitness()+"\n";
		}
		return output;
	}
}
