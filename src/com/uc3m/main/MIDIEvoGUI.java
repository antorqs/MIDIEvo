package com.uc3m.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.MatteBorder;

public class MIDIEvoGUI {
	
	private JFrame window;
	private static Font baseFont;
	private JLabel filename;
	private JLabel notes;
	private JLabel bestfit;
	private JLabel worstfit;
	private JLabel currgen;
	private JLabel status;
	private JLabel mrate;
	private JFormattedTextField population;
	private JFormattedTextField tournaments;
	private JFormattedTextField generations;
	private JButton start;
	private JButton best;
	private JButton worst;
	private JButton orig;
	private JButton select; 
	private ActionListener listener;
	private JFileChooser fc;
	private JRadioButton mode1, mode2;
	private JRadioButton stra1, stra2, stra3;
	
	public MIDIEvoGUI(ActionListener listen){
		try {
			baseFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/unispace.ttf"));
			baseFont = baseFont.deriveFont(Font.BOLD, 12);
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		listener = listen;
		
		filename = label("No file selected...", Color.ORANGE);
		notes = label("0", Color.ORANGE);
		bestfit = label("0", Color.ORANGE);
		worstfit = label("0", Color.ORANGE);
		currgen = label("1", Color.ORANGE);
		status = label("Status", Color.ORANGE);
		mrate = label("10%", Color.ORANGE);
		population = text(5000);
		generations = text(20000);
		tournaments = text(3000);
		
		start = button("start");
		best = button("playbest");
		worst = button("playworst");
		orig = button("playorig");
		
		fc = new JFileChooser();
	}
	
	public JFrame constructWindow(){
		window = new JFrame("MidiEvo");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Toolkit kit = Toolkit.getDefaultToolkit();
		java.net.URL url = getClass().getResource("/MIDIEvoIcon.png");
		Image img = kit.createImage(url);
		window.setIconImage(img);
		JPanel containerPanel = new JPanel(new FlowLayout());
		containerPanel.setBackground(Color.darkGray);
		JLabel back = new JLabel(new ImageIcon(getClass().getResource("/MIDIEvoLogo3.png")));
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(Color.darkGray);
		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.setBackground(Color.darkGray);
		
		ButtonGroup bg1 = new ButtonGroup( );
		mode1 = new JRadioButton("EASY ");
		mode2 = new JRadioButton("HARD");
		mode1.setForeground(Color.ORANGE);
		mode1.setBackground(Color.darkGray);
		mode1.setSelected(true);
		mode2.setForeground(Color.ORANGE);
		mode2.setBackground(Color.darkGray);
		bg1.add(mode1);
		bg1.add(mode2);
		
		ButtonGroup bg2 = new ButtonGroup( );
		stra1 = new JRadioButton("NONE");
		stra1.setForeground(Color.ORANGE);
		stra1.setBackground(Color.darkGray);
		stra1.setSelected(true);
		stra2 = new JRadioButton("AMR");
		stra2.setForeground(Color.ORANGE);
		stra2.setBackground(Color.darkGray);
		stra3 = new JRadioButton("ROG");
		stra3.setForeground(Color.ORANGE);
		stra3.setBackground(Color.darkGray);
		JRadioButton dummy = new JRadioButton("Dummy");
		bg2.add(stra1);
		bg2.add(stra2);
		bg2.add(stra3);
		bg2.add(dummy);
		
		JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		modePanel.add(mode1);
		modePanel.add(mode2);
		modePanel.setBackground(Color.darkGray);
		
		JPanel straPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		straPanel.add(stra1);
		straPanel.add(stra2);
		straPanel.setBackground(Color.darkGray);
		JPanel straPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		straPanel2.add(stra3);
		straPanel2.add(dummy);
		dummy.setVisible(false);
		straPanel2.setBackground(Color.darkGray);
		
		Dimension dim = new Dimension(300,250);
		JLabel l_filename = label(" Filename: ");
		JLabel l_notes = label("<html>&nbsp;# Notes: <br />&nbsp;</html>");
		JLabel l_mode = label(" Mode: ");
		JLabel l_stra = label(" Diversity ");
		JLabel l_stra2 = label(" Strategy: ");
		JLabel l_mrate = label(" Mutation rate: ");
		JLabel l_population = label(" Population: ");
		JLabel l_repalcement = label(" Tournaments: ");
		JLabel l_generations = label(" Generations: ");
		JLabel l_bestfitness = label(" Best Fitness: ");
		JLabel l_worstfitness = label(" Worst Fitness: ");
		JLabel l_gen = label(" Current Gen: ");
		JLabel l_status = label(" Status: ");
		
		select = button("select");
		
		JPanel parameterPanelLeft = new JPanel();
		JPanel parameterPanelRight = new JPanel();
		parameterPanelLeft.setLayout(new GridLayout(7,2));
		parameterPanelRight.setLayout(new GridLayout(7,2));
		parameterPanelLeft.setBackground(Color.darkGray);
		parameterPanelRight.setBackground(Color.darkGray);
		
		parameterPanelLeft.setPreferredSize(dim);
		parameterPanelRight.setPreferredSize(dim);		
		parameterPanelLeft.add(l_filename);
		parameterPanelLeft.add(select);
		parameterPanelLeft.add(l_notes);
		parameterPanelLeft.add(notes);
		parameterPanelLeft.add(l_mode);
		parameterPanelLeft.add(modePanel);
		parameterPanelLeft.add(l_stra);
		parameterPanelLeft.add(straPanel);
		parameterPanelLeft.add(l_stra2);
		parameterPanelLeft.add(straPanel2);
		parameterPanelLeft.add(l_bestfitness);
		parameterPanelLeft.add(bestfit);
		parameterPanelLeft.add(l_gen);
		parameterPanelLeft.add(currgen);
		parameterPanelRight.add(l_population);
		parameterPanelRight.add(population);
		parameterPanelRight.add(l_repalcement);
		parameterPanelRight.add(tournaments);
		parameterPanelRight.add(l_generations);
		parameterPanelRight.add(generations);
		parameterPanelRight.add(l_mrate);
		parameterPanelRight.add(mrate);
		parameterPanelRight.add(new JLabel(""));
		parameterPanelRight.add(new JLabel(""));
		parameterPanelRight.add(l_worstfitness);
		parameterPanelRight.add(worstfit);
		parameterPanelRight.add(l_status);
		parameterPanelRight.add(status);
		
		buttonPanel.add(start);
		buttonPanel.add(orig);
		buttonPanel.add(best);
		buttonPanel.add(worst);
		
		mainPanel.add(filename, BorderLayout.PAGE_START);
		mainPanel.add(parameterPanelLeft, BorderLayout.LINE_START);
		mainPanel.add(parameterPanelRight, BorderLayout.LINE_END);
		mainPanel.add(buttonPanel, BorderLayout.PAGE_END);
		
		containerPanel.add(Box.createRigidArea(new Dimension(10,0)));
		containerPanel.add(back);
		containerPanel.add(Box.createRigidArea(new Dimension(5,0)));
		containerPanel.add(mainPanel);
		window.getContentPane().add(containerPanel);
		window.pack();
		window.setVisible(true);
		window.setResizable(false);
		return window;
	}
	
	private JButton button(String buttonname){
		JButton button = new JButton("", new ImageIcon(getClass().getResource("/"+buttonname+".png")));
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorder(null);
		button.setPressedIcon(new ImageIcon(getClass().getResource("/"+buttonname+"P.png")));
		button.setActionCommand(buttonname);
		button.addActionListener(listener);
		return button;
	}
	
	private JLabel label(String label, Color color){
		JLabel jlabel = new JLabel(label);
		jlabel.setFont(baseFont);
		jlabel.setForeground(color);
		return jlabel;
	}
	
	private JLabel label(String label){
		return label(label, Color.white);
	}
	
	private JFormattedTextField text(int value){
		JFormattedTextField field = new JFormattedTextField();
		field.setOpaque(false);
		field.setFont(baseFont);
		field.setForeground(Color.ORANGE);
		field.setValue(value);
		MatteBorder border = new MatteBorder(0, 0, 2, 0, Color.lightGray);
		field.setBorder(border);
		return field;
	}

	public void switchOrig(){
		switchOrig("");
	}
	public void switchOrig(String command){
		if(command.equals("")){
			if(orig.getActionCommand().startsWith("play")){
				command = "stop";
			}else{
				command = "play";
			}
		}
		orig.setIcon(new ImageIcon(getClass().getResource("/"+command+"orig.png")));
		orig.setPressedIcon(new ImageIcon(getClass().getResource("/"+command+"origP.png")));
		orig.setActionCommand(command+"orig");
	}
	
	public void switchBest(){
		switchBest("");
	}
	
	public void switchBest(String command){
		if(command.equals("")){
			if(best.getActionCommand().startsWith("play")){
				command = "stop";
			}else{
				command = "play";
			}
		}
		best.setIcon(new ImageIcon(getClass().getResource("/"+command+"best.png")));
		best.setPressedIcon(new ImageIcon(getClass().getResource("/"+command+"bestP.png")));
		best.setActionCommand(command+"best");
	}
	
	public void switchWorst(){
		switchWorst("");
	}
	
	public void switchWorst(String command){
		if(command.equals("")){
			if(worst.getActionCommand().startsWith("play")){
				command = "stop";
			}else{
				command = "play";
			}
		}
		worst.setIcon(new ImageIcon(getClass().getResource("/"+command+"worst.png")));
		worst.setPressedIcon(new ImageIcon(getClass().getResource("/"+command+"worstP.png")));
		worst.setActionCommand(command+"worst");
	}
	
	public void switchEvo(){
		if(start.getActionCommand().startsWith("start")){
			start.setIcon(new ImageIcon(getClass().getResource("/stop.png")));
			start.setPressedIcon(new ImageIcon(getClass().getResource("/stopP.png")));
			start.setActionCommand("stop");
		}else{
			start.setIcon(new ImageIcon(getClass().getResource("/start.png")));
			start.setPressedIcon(new ImageIcon(getClass().getResource("/startP.png")));
			start.setActionCommand("start");
		}
	}
	
	public File selectFile(){
		File file = null;
		int ret = fc.showOpenDialog(window);

		filename.setText("No file selected...");
		if(ret == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile();
			filename.setText(file.getName());
		}
		return file;
	}
	
	public int getMode(){
		if(mode2.isSelected())
			return 2;
		return 1;
	}

	public String getNotes() {
		return notes.getText();
	}

	public void setNotes(String notes) {
		this.notes.setText(notes);
	}

	public String getBestfit() {
		return bestfit.getText();
	}

	public void setBestfit(String bestfit) {
		this.bestfit.setText(bestfit);
	}

	public String getWorstfit() {
		return worstfit.getText();
	}

	public void setWorstfit(String worstfit) {
		this.worstfit.setText(worstfit);
	}

	public String getCurrgen() {
		return currgen.getText();
	}

	public void setCurrgen(String currgen) {
		this.currgen.setText(currgen);
	}

	public int getPopulation() {
		return (Integer) population.getValue();
	}

	public void setPopulation(int population) {
		this.population.setValue(population);
	}

	public int getTournaments() {
		return (Integer) tournaments.getValue();
	}

	public void setTournaments(int replacement) {
		this.tournaments.setValue(replacement);
	}

	public int getGenerations() {
		return (Integer) generations.getValue();
	}

	public void setGenerations(int generations) {
		this.generations.setValue(generations);
	}
	
	public String getStatus() {
		return status.getText();
	}

	public void setStatus(String status) {
		this.status.setText(status);
	}
	
	public String getMRate() {
		return mrate.getText();
	}

	public void setMRate(String mrate) {
		this.mrate.setText(mrate);
	}
	
	public void toggleSelect(){
		this.select.setEnabled(!this.select.isEnabled());
	}
	
	public int getStrategy(){
		if(stra2.isSelected())
			return 2;
		if(stra3.isSelected())
			return 3;
		return 1;
	}
}
