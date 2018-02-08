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
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import roll.automata.FDFA;
import roll.automata.NBA;
import roll.automata.operations.NBAGenerator;
import roll.automata.operations.NBAOperations;
import roll.automata.operations.nba.inclusion.NBAInclusionCheckTool;
import roll.learner.fdfa.LearnerFDFA;
import roll.learner.nba.lomega.UtilLOmega;
import roll.learner.nba.lomega.translator.TranslatorFDFA;
import roll.learner.nba.lomega.translator.TranslatorFDFAUnder;
import roll.main.complement.TeacherNBAComplement;
import roll.main.inclusion.NBAInclusionCheck;
import roll.parser.PairParser;
import roll.parser.Parser;
import roll.parser.UtilParser;
import roll.query.Query;
import roll.table.HashableValue;
import roll.table.HashableValueBoolean;
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
		this.args = args;
;		this.rollOut = new PipedOutputStream(intIn);
		this.rollIn = new PipedInputStream(intOut);
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
            runPlayingMode(options);
            break;
        case LEARNING:
            options.log.info("ROLL for BA learning via rabit...");
            runLearningMode(options, false, this.rollOut, this.rollIn);
            break;
        default :
                options.log.err("Incorrect running mode.");
        }
	}
    
    
    public  void runPlayingMode(Options options) {
        InteractiveMode.interact(options, this.rollOut, this.rollIn);
    }
    
    public static void runLearningMode(Options options, boolean sampling, PipedOutputStream out, PipedInputStream in) {

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
        }else {
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
        }
        parser.close();
        // output statistics
        options.stats.numOfTransInTraget = NBAOperations.getNumberOfTransitions(target);
        options.stats.numOfTransInHypothesis = NBAOperations.getNumberOfTransitions(options.stats.hypothesis);
        options.stats.print();
        
    }

    


	

}
