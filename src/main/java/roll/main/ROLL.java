/* Copyright (c) 2016, 2017                                               */
/*       Institute of Software, Chinese Academy of Sciences               */
/* This file is part of ROLL, a Regular Omega Language Learning library.  */
/* ROLL is free software: you can redistribute it and/or modify           */
/* it under the terms of the GNU General Public License as published by   */
/* the Free Software Foundation, either version 3 of the License, or      */
/* (at your option) any later version.                                    */

/* This program is distributed in the hope that it will be useful,        */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of         */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          */
/* GNU General Public License for more details.                           */

/* You should have received a copy of the GNU General Public License      */
/* along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

package roll.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;

import roll.automata.NBA;
import roll.automata.operations.NBAOperations;
import roll.parser.Parser;
import roll.parser.UtilParser;
import roll.util.Timer;

/**
 * 
 * Main entry of the tool Regular Omega Language Learning Library
 * 
 * 
 * @author Yong Li (liyong@ios.ac.cn)
 * */
public final class ROLL extends Thread{
	
	private String[] args;
	private PipedOutputStream rollOut;
	private PipedInputStream rollIn;
	
	public ROLL(String[] args, PipedInputStream intIn, PipedOutputStream intOut) throws IOException {
		System.out.println("Initializing ROLL");
		this.args = args;
;		this.rollOut = new PipedOutputStream(intIn);
		this.rollIn = new PipedInputStream(intOut);
		System.out.println("Initializing ROLL over");
	}
	
	@Override
	public void run() {
		// select mode to execute
        CLParser clParser = new CLParser();
        clParser.prepareOptions(this.args);
        Options options = clParser.getOptions();
        options.log.println("\n" + options.toString());
        switch(options.runningMode) {
        case PLAYING:
            options.log.info("ROLL for interactive play...");
            byte[] alphabetBytes = new byte[1024];
            
            int len = 0;
            String alphabetStr = null;
			try {
				len = this.rollIn.read(alphabetBytes);
				alphabetStr = new String(alphabetBytes, 0, len);
	            this.rollOut.write(("ALPHA OKAY " + alphabetStr).getBytes());
	            this.rollOut.flush(); 
	            
			} catch (IOException e) {
				e.printStackTrace();
			}
			byte[] alphaNumBytes = new byte[1024];
            try {
				len = this.rollIn.read(alphaNumBytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
            //assert(alphaNumBytes != null);
            String alphaNumStr = new String(alphaNumBytes, 0, len);
            alphaNumStr = alphaNumStr.trim();
            int alphaNum = Integer.parseInt(alphaNumStr);
            //parse alphabet
            String[] alphabetStrArray = new String[alphaNum];
           
            for(int i = 0; i < alphaNum; i ++) {
            	Character temp = alphabetStr.toCharArray()[i];
            	alphabetStrArray[i] = "" + temp;
            }
            																	
            runPlayingMode(options, alphabetStrArray, alphaNum);
            try {
            	//System.out.println("pipe Close");
				this.rollOut.close();
				this.rollIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
           
            break;
        case LEARNING:
            options.log.info("ROLL for BA learning via rabit...");
			try {
				runLearningMode(options, false, this.rollOut, this.rollIn);
			} catch (IOException e) {
				e.printStackTrace();
			}
            break;
        default :
                options.log.err("Incorrect running mode.");
        }
	}
    
    
    public  void runPlayingMode(Options options, String[] alpha, int alphaNum) {
        InteractiveMode.interact(options, this.rollOut, this.rollIn, alpha, alphaNum);
    }
    
    public static void runLearningMode(Options options, boolean sampling, PipedOutputStream out, PipedInputStream in) throws IOException{

        Timer timer = new Timer();
        timer.start();
        // prepare the parser
        Parser parser = UtilParser.prepare(options, options.inputFile, options.format);
        NBA target = parser.parse();
        options.stats.numOfLetters = target.getAlphabetSize();
        options.stats.numOfStatesInTraget = target.getStateSize();
        // learn the target automaton
        
        if(sampling) {
            Executor.executeSampler(options, target);
        } else {
            Executor.executeRABIT(options, target);
        }
        timer.stop();
        options.stats.timeInTotal = timer.getTimeElapsed();
        // output target automaton
        if(options.outputFile != null) {
            try {
                parser.print(options.stats.hypothesis, new FileOutputStream(new File(options.outputFile)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else {
            options.log.println("\ntarget automaton:");
            parser.print(target, options.log.getOutputStream());
            options.log.println("\nhypothesis automaton:");
            parser.print(options.stats.hypothesis, options.log.getOutputStream());
            //TODO: change path when release
            File file = new File("C:\\Users\\10244\\Desktop\\outputBA.ba");
            FileOutputStream fop = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(file);
            writer.write("");
            writer.close();
            parser.print(options.stats.hypothesis, fop);
            fop.flush();
            fop.close();
            out.write("C-Complete".getBytes());
            out.flush();
        }
        parser.close();
        // output statistics
        options.stats.numOfTransInTraget = NBAOperations.getNumberOfTransitions(target);
        options.stats.numOfTransInHypothesis = NBAOperations.getNumberOfTransitions(options.stats.hypothesis);
        options.stats.print();
        
    }

    


	

}
