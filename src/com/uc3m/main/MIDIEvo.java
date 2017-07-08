package com.uc3m.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;


/**
 * MIDIEvo class.
 * Main class for evolving MIDI file's tracks using evolutionary algorithms
 * @author antonio
 *
 * Notas: Primero hay que definir la selección (Torneo)
 * 	Se seleccionan 3 aleatoriamente y se elige el mejor.
 *  Se repite el proceso y los dos seleccionados se recombinan.
 *  
 *  Recombinación: No se puede tomar en cuenta el fitness de
 *  los genes, solamente el fitness global, por ende se debe hacer
 *  una recombinación diferente, puede ser uniforme
 *  (un gen de un individuo - un gen del otro)
 *  
 *  Mutación: Luego de recombinar, se hace una mutación en todos los elementos.
 *  Por cada mensaje, seleccionar un valor aleatorio entre 0 y 1.
 *  Si el valor es menor a 0,1 (10%) mutar el mensaje. Si es mayor a 0,1
 *  no hacer nada.
 *  
 *  Reemplazo: Se seleccionan los mejores individuos de las dos poblaciones
 *  (Inicial + Copia)
 *  
 */
public class MIDIEvo implements ActionListener {

	private ArrayList<MIDIEvoTrack> population;			//Generated Population
	private ArrayList<MIDIEvoTrack> selection;			//Tournament selection
	private ArrayList<MIDIEvoTrack> offspring;			//Recombined population
	
	private int POPULATION_SIZE = 8000;		//Size of the population
	private int TOURNAMENT_ROUNDS = 3000;	//Size of the population
	public static final int EASY_MODE = 1;	//Only evolve Message type (NOTE_ON, NOTE_OFF) and Key (Use original timing)
	public static final int HARD_MODE = 2;	//Evolve Message type (NOTE_ON, NOTE_OFF), Key and timing
	public static final int STRATEGY_NONE = 1;
	public static final int STRATEGY_AMR = 2;
	public static final int STRATEGY_ROG = 3;
	public static final int MUTATION_TEST = 100;
	public int MAX_GENERATIONS = 2000;		//Max number of generations
	private MIDIEvoTrack midievotrack;		//Keeping original track
	private int mode;
	private int strategy;
	private int m_test;
	private double mrate = 0.1;
	private double baseCoefficient = 2;
	private double topCoefficient = 20;
	private MIDIEvoGUI gui;
	private File inputFile;
	private Sequence sequence;
	private boolean stop = false;
	private Player player;
	private Thread playerThread;
	private Thread evoThread;
	
	/**
	 * MIDIEvo constructor
	 */
	public MIDIEvo(){
		population = new ArrayList<MIDIEvoTrack>();
		selection = new ArrayList<MIDIEvoTrack>();
		this.mode = EASY_MODE;
		this.strategy = STRATEGY_NONE;
		gui = new MIDIEvoGUI(this);
		player = new Player(0);
	}
	
	/**
	 * MIDIEvo constructor
	 */
	public MIDIEvo(int mode){
		this();
		this.mode = mode;
	}
	
	/**
	 * Initializing from source file.
	 * @param filename
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	/*public void initializeSource(String filename) throws InvalidMidiDataException, IOException{
		sequence = MidiSystem.getSequence(new File(filename));
		
        for (Track track :  sequence.getTracks()) {
        	MIDIEvoTrack midievotrack = new MIDIEvoTrack(track);
        	sourceTracks.add(midievotrack);
        }
	}*/
	
	/**
	 * Main process method.
	 * Take the second track of the source file and start evolving a population to match the original track.
	 * 
	 * @param sequence
	 * @param mode
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public void MIDIEvoProcess() throws InvalidMidiDataException, IOException{
		System.out.println(":::::: MIDIEvo Process ::::::");
		
		Track track = sequence.getTracks()[1];
		midievotrack = new MIDIEvoTrack(track);
		gui.setNotes(""+midievotrack.getMaxSize());
		
		//Initialization: Create a population of randomly generated individuals
		gui.setStatus("Initialzing Population...");
		for(int i = 0; i< POPULATION_SIZE; i++){
			//Create an empty individual (Using the data from the source)
			MIDIEvoTrack evolvedTrack = new MIDIEvoTrack(midievotrack.getMaxSize(), 
					midievotrack.getMaxTick(), midievotrack.getChannel(), 
					midievotrack.getOriginalMessages(), midievotrack.getOriginalMessagesTicks());
			
			evolvedTrack.createRandom(midievotrack);	//Generate random notes and timing
			//If EASY_MODE then copy the timing from the original source.
			if(mode == EASY_MODE){
				evolvedTrack.copyTicks(midievotrack);
			}
			//Calculate and update fitness of the individual
			evolvedTrack.calculateFitness(midievotrack);
			population.add(evolvedTrack);	//Add it to the population
		}
				
		//Start the evolution process
		gui.setStatus("Running...");
		m_test = -1;
		for(int i = 0; i< MAX_GENERATIONS && !stop; i++){
			evolve();	//evolve the population
			m_test++;
			if(strategy == STRATEGY_AMR && m_test == MUTATION_TEST){
				evaluteMutationRate();
				m_test = 0;
			}
			gui.setBestfit(""+population.get(0).getTotalFitness());
			gui.setWorstfit(""+population.get(POPULATION_SIZE-1).getTotalFitness());
			gui.setCurrgen(""+(i+1));

			//If the first element has fitness zero then end.
			if(population.get(0).getTotalFitness().compareTo(BigInteger.valueOf(0))==0)
				i = MAX_GENERATIONS;
		}
		gui.switchEvo();
		gui.toggleSelect();
		gui.setStatus("Finished...");
	}

	/**
	 * Method to evolve the population of tracks to match an original track
	 * This method calls recombination and mutation and then sort by fitness.
	 * @param method
	 */
	public void evolve(){

		//Natural Selection
		tournament();
		
		//Recombination
		recombination();
		
		//Mutation of the elements
		mutation();
		
		//Replacement
		replacement();

	}

	
	/**
	 * Selection Tournament
	 * Select three individuals randomly, the best of 
	 * the three is selected for recombination.
	 */
	public void tournament(){
		selection = new ArrayList<MIDIEvoTrack>();
		int ind_a, ind_b, ind_c;	//individuals
		int best;
		for(int ii = 0; ii < TOURNAMENT_ROUNDS; ii++){
			ind_a = ThreadLocalRandom.current().nextInt(0, POPULATION_SIZE);
			ind_b = ThreadLocalRandom.current().nextInt(0, POPULATION_SIZE);
			ind_c = ThreadLocalRandom.current().nextInt(0, POPULATION_SIZE);
			
			if( population.get(ind_a).getTotalFitness().compareTo( 
					population.get(ind_b).getTotalFitness()) < 0 )
				best = ind_a;
			else
				best = ind_b;
			
			if( population.get(ind_c).getTotalFitness().compareTo( 
					population.get(best).getTotalFitness()) < 0 )
				best = ind_c;
			
			selection.add(population.get(best));
		}
	}

	/**
	 * Recombination method
	 * Create a new MIDIEvoTrack by recombining two individuals
	 * Takes randomly one shortmessage of one of the parents, for every shortmessage    
	 * @param mode EASY_MODE or HARD_MODE
	 * @param met1	MIDIEvoTrack 1 to recombine
	 * @param met2	MIDIEvoTrack 2 to recombine
	 * @return
	 */
	
	public void recombination(){
		offspring = new ArrayList<MIDIEvoTrack>();
		for(int i = 0; i < TOURNAMENT_ROUNDS; i+=2){
			if( strategy == STRATEGY_ROG &&
					selection.get(i).isEqualTo(selection.get(i+1))){

				MIDIEvoTrack randomOffspring = new MIDIEvoTrack(midievotrack.getMaxSize(), 
						midievotrack.getMaxTick(), midievotrack.getChannel(), 
						midievotrack.getOriginalMessages(), midievotrack.getOriginalMessagesTicks());
				
				randomOffspring.createRandom(midievotrack);	//Generate random notes and timing
				//If EASY_MODE then copy the timing from the original source.
				if(mode == EASY_MODE){
					randomOffspring.copyTicks(midievotrack);
				}
				//Calculate and update fitness of the individual
				randomOffspring.calculateFitness(midievotrack);
				offspring.add(randomOffspring);
			}else{
				offspring.add(
					recombine(selection.get(i), selection.get(i+1)));
			}
		}		
	}
	
	public MIDIEvoTrack recombine(MIDIEvoTrack met1, MIDIEvoTrack met2){
		MIDIEvoTrack newTrack = new MIDIEvoTrack(met1);

		//For every shortmessage in the track (met1 and met2 have the same size)
		for(int i = 0; i < met1.getSize(); i++){
			
			int parent = ThreadLocalRandom.current().nextInt(0, 2);
			
			SimplifiedShortMessage ssm = null;
			if(parent == 1)
				ssm = met1.getSimplifiedShortMessages().get(i);
			else
				ssm = met2.getSimplifiedShortMessages().get(i);
			
			newTrack.getSimplifiedShortMessages().add(new SimplifiedShortMessage(ssm));
			newTrack.setTotalFitness(newTrack.getTotalFitness().add(BigInteger.valueOf(ssm.getFitness())));
		}
		//Calculate the new MIDIEvoTrack fitness
		//newTrack.calculateFitness();
		return newTrack;
	}

	/**
	 * Mutation method
	 * Select randomly an individual from the offspring 
	 * Then call it's mutation method and calculate fitness
	 * @param method
	 */
	public void mutation(){
		for(int i = 0; i < offspring.size(); i++){
			offspring.get(i).mutation(this.mode, this.mrate, midievotrack);
			offspring.get(i).calculateFitness();
		}
		/*for(int i = 0; i < population.size(); i++){
			population.get(i).mutation(this.mode, this.mrate, midievotrack);
			population.get(i).calculateFitness();
		}*/
	}

	
	/**
	 * Replacement
	 */
	public void replacement(){
		Collections.sort(population);
		population.subList(population.size() - offspring.size(), population.size()).clear();
		population.addAll(offspring);
		Collections.sort(population);
	}
	
	public void evaluteMutationRate(){
		BigDecimal avg = avgTopFitness();
		BigDecimal var = BigDecimal.valueOf(0);
		for(int i=0; i<population.size(); i++){
			BigDecimal aux = new BigDecimal(population.get(i).getTotalFitness()).subtract(avg);
			var = var.add(aux.pow(2));
		}
		BigDecimal std = var.divide(BigDecimal.valueOf(population.size()-1), 2, RoundingMode.HALF_UP);
		BigDecimal stdDev = BigDecimal.valueOf(StrictMath.sqrt(std.doubleValue()));
		BigDecimal coef = stdDev.divide(avg, 2, RoundingMode.HALF_UP);
		if(coef.multiply(BigDecimal.valueOf(100)).compareTo(BigDecimal.valueOf(baseCoefficient)) < 0){
			mrate += 0.05;
			if(mrate > 0.7)
				mrate = 0.7;
			gui.setMRate(mrate*100+"%");
		}else if(coef.multiply(BigDecimal.valueOf(100)).compareTo(BigDecimal.valueOf(topCoefficient)) > 0){
			mrate -= 0.05;
			if(mrate < 0.1)
				mrate = 0.0;
			gui.setMRate(mrate*100+"%");
		}
	}
	
		
	/**
	 * Calculate average fitness from the population
	 * @return BigInteger average fitness
	 */
	public BigDecimal avgTopFitness(){
		BigDecimal fitness = BigDecimal.valueOf(0);
		for(int i=0; i<population.size(); i++){
			fitness = fitness.add(new BigDecimal(population.get(i).getTotalFitness()));
		}
		fitness = fitness.divide(BigDecimal.valueOf(population.size()));
		return fitness;
	}
		
	/**
	 * Main Method - take as arguments the source file name and the mode (EASY_MODE/HARD_MODE)
	 * @param args
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InvalidMidiDataException, IOException{
		
		MIDIEvo main = new MIDIEvo();
		main.getGUI().constructWindow();
	}
	
	/*private void writeOutput(Sequence sequence) throws InvalidMidiDataException, IOException{
		System.out.println(":::::: MIDIEvo Finish::::::");
		System.out.println("Total time: "+((tEnd - tStart)/1000)+" sec");
		//Create an output sequence to save the best generated individual as a file
		Sequence outSequence = new Sequence(sequence.getDivisionType(), sequence.getResolution());
		population.get(0).convertToTrack(outSequence);
		MidiSystem.write(outSequence, 1, new File("/home/antonio/sounds/myEvolvedTrack.mid"));
	}*/

	private MIDIEvoGUI getGUI(){
		return gui;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch(e.getActionCommand()){
		case "start": start();
			break;
		case "stop": stop();
			break;
		case "playbest": playBest();
			break;
		case "playworst": playWorst();
			break;
		case "playorig": playOrig();
			break;
		case "select": selectFile();
			break;
		case "stopbest": gui.switchBest(); stopPlay();
			break;
		case "stopworst": gui.switchWorst(); stopPlay();
			break;
		case "stoporig": gui.switchOrig(); stopPlay();
			break;
		}	
	}
	
	private void selectFile(){
		sequence = null;
		inputFile = gui.selectFile();
		if(inputFile != null){
			try {
				sequence = MidiSystem.getSequence(inputFile);
				Track track = sequence.getTracks()[1];
				midievotrack = new MIDIEvoTrack(track);
				gui.setNotes(""+midievotrack.getMaxSize());
			} catch (InvalidMidiDataException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void reset(){
		gui.setBestfit("0");
		gui.setWorstfit("0");
		gui.setCurrgen("1");
		gui.setMRate("10%");
		population = new ArrayList<MIDIEvoTrack>();
		selection = new ArrayList<MIDIEvoTrack>();
		this.mode = gui.getMode();
		this.POPULATION_SIZE = gui.getPopulation();
		this.TOURNAMENT_ROUNDS = gui.getTournaments();
		this.MAX_GENERATIONS = gui.getGenerations();
		this.strategy = gui.getStrategy();
		this.mrate = 0.1;
	}
	
	private void stop(){
		stop = true;
	}
	
	private void start(){
		if(inputFile == null){
			System.out.println("Please select a file first.");
			return;
		}
		reset();
		gui.toggleSelect();

		gui.switchEvo();
		
		if(evoThread != null && evoThread.isAlive())
			evoThread.interrupt();
		evoRunnable evoRun = new evoRunnable();
        evoThread = new Thread(evoRun);
        evoThread.start();
		
	}
	
	private void playOrig(){
		if(sequence == null)
			return;
		gui.switchOrig();
		gui.switchBest("play");
		gui.switchWorst("play");
		player.stopPlayer();
		if(playerThread != null && playerThread.isAlive())
			playerThread.interrupt();
		player.setPos(-1);
		playerThread = new Thread(player);
		playerThread.start();
	}
	
	private void playBest(){
		if(population == null || population.size() == 0)
			return;
		gui.switchBest();
		gui.switchOrig("play");
		gui.switchWorst("play");
		player.stopPlayer();
		if(playerThread != null && playerThread.isAlive())
			playerThread.interrupt();
		player.setPos(0);
		playerThread = new Thread(player);
		playerThread.start();
	}
	
	private void playWorst(){
		if(population == null || population.size() == 0)
			return;
		gui.switchWorst();
		gui.switchOrig("play");
		gui.switchBest("play");
		player.stopPlayer();
		if(playerThread != null && playerThread.isAlive())
			playerThread.interrupt();
		player.setPos(POPULATION_SIZE - 1);
		playerThread = new Thread(player);
		playerThread.start();
	}
	
	private void stopPlay(){
		player.stopPlayer();
		if(playerThread != null && playerThread.isAlive())
			playerThread.interrupt();
	}
	
	public class evoRunnable implements Runnable{

		@Override
		public void run() {
			try {
				stop = false;
				MIDIEvoProcess();
			} catch (InvalidMidiDataException | IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public class Player implements Runnable{

		int pos;
		Sequencer sequencer;
		
		public Player(int pos){
			this.pos = pos;
		}
		@Override
		public void run() {
			playSeq();
		}
		
		public void setPos(int pos){
			this.pos = pos;
		}
		
		public void stopPlayer(){
			if(sequencer!=null)
				sequencer.stop();
		}
		
		public void playSeq(){
			try {
				sequencer = MidiSystem.getSequencer();
				sequencer.open();
				sequencer.setTempoInBPM(240);
				Sequence outSequence;
				if(pos > -1){
					outSequence = new Sequence(sequence.getDivisionType(), sequence.getResolution());
					ArrayList<MIDIEvoTrack> pop = new ArrayList<MIDIEvoTrack>(population);
					Collections.sort(pop);
					pop.get(pos).convertToTrack(outSequence);
					sequencer.setSequence(outSequence);
				}else
					sequencer.setSequence(sequence);
				sequencer.start();
			} catch (MidiUnavailableException e) {
				e.printStackTrace();
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
		}
	}
}
