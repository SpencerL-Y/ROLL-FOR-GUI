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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


import roll.automata.NBA;
import roll.learner.LearnerBase;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.words.Alphabet;
import roll.words.Word;

/**
 * @author Yong Li (liyong@ios.ac.cn)
 * */

public class InteractiveMode {
    
    public static void interact(Options options, PipedOutputStream rollOut, PipedInputStream rollIn, String[] alpha, int alphaNum) {
        // prepare the alphabet
        Alphabet alphabet = prepareAlphabet(options, alpha, alphaNum);
        TeacherNBAInteractive teacher = new TeacherNBAInteractive(rollOut, rollIn);
        LearnerBase<NBA> learner = Executor.getLearner(options, alphabet, teacher);
        
        options.log.println("Initializing learning...");
        learner.startLearning();
        boolean result = false;
        while(! result ) {
            options.log.verbose("Table/Tree is both closed and consistent\n" + learner.toString());
            NBA hypothesis = learner.getHypothesis();
            // along with ce
            System.out.println("Resolving equivalence query for hypothesis (#Q=" + hypothesis.getStateSize() + ")...  ");
            Query<HashableValue> ceQuery = teacher.answerEquivalenceQuery(hypothesis);
            boolean isEq = ceQuery.getQueryAnswer().get();
            if(isEq == true) break;
            ceQuery = getOmegaCeWord(alphabet, rollOut, rollIn);
            ceQuery.answerQuery(null);
            learner.refineHypothesis(ceQuery);
        }
        
        System.out.println("Congratulations! Learning completed...");
    }
    
    private static Alphabet prepareAlphabet(Options options, String[] alpha, int alphaNum) {
        Alphabet alphabet = new Alphabet();
        System.out.println("Please input the number of letters ('a'-'z'): ");
        int numLetters = alphaNum;
        for(int letterNr = 0; letterNr < numLetters; letterNr ++) {
            System.out.println("Please input the " + (letterNr + 1) + "th letter: ");
            char letter = alpha[letterNr].toCharArray()[0];
            alphabet.addLetter(letter);
        }
        return alphabet;
    }
    //TODO:move this to the front end
//    private static char getLetter(Alphabet alphabet) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        char letter = 0;
//        do {
//            try {
//                String line = reader.readLine();
//                letter = line.charAt(0);
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//            if (letter < 'a' || letter > 'z')
//                System.out.println("Illegal input, try again!");
//            else if(alphabet.indexOf(letter) >= 0){
//                System.out.println("Duplicate input letter, try again!");
//            }else {
//                break;
//            }
//        } while (true);
//        return letter;
//    }
    
    
    //Okay
    public static boolean getInputAnswer(PipedOutputStream rollOut, PipedInputStream rollIn) {
        boolean answer = false;
        try {
            boolean finished = false;
            while(! finished) {
            	byte[] inputBytes = new byte[1024];
            	int len = rollIn.read(inputBytes);
            	String input = new String(inputBytes, 0, len);
            	input = input.trim();
            	System.out.println(input);
                if(input.equals("1")) {
                    answer = true;
                    finished = true;
                }else if(input.equals("0")) {
                    answer = false;
                    finished = true;
                }else {
                	String illeagal = "Illegal input, try again.";
                	rollOut.write(illeagal.getBytes());
                    System.out.println("Illegal input, try again!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return answer;
    }
    
//    private static Query<HashableValue> getCeWord(Alphabet alphabet) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        Word word = null;
//        try {
//            do {
//                String input = reader.readLine();
//                word = alphabet.getWordFromString(input);
//                if(word == null)    System.out.println("Illegal input, try again!");
//            }while(word == null);
//            
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return new QuerySimple<HashableValue>(word, alphabet.getEmptyWord());
//    }
    
   
    
    private static Query<HashableValue> getOmegaCeWord(Alphabet alphabet, PipedOutputStream rollOut, PipedInputStream rollIn) {
        Word prefix = null, suffix = null;
        System.out.println("Now you have to input a counterexample for inequivalence.");
        try {
            do {
                System.out.println("please input stem: ");
                byte[] inputBytes = new byte[1024];
                int len = rollIn.read(inputBytes);
                String input = new String(inputBytes, 0, len);
                input = input.trim();
                System.out.println("input stem is " + input);
                boolean valid = true;
                for(int i = 0; i < input.length(); i ++) {
                    int letter = alphabet.indexOf(input.charAt(i));
                    if(letter < 0) {
                        valid = false;
                        break;
                    }
                }
                if(valid) {
                    prefix = alphabet.getWordFromString(input);
                }else  {
                	String illegal = "Illegal input, try again!";
                	rollOut.write(illegal.getBytes());
                    System.out.println("Illegal input, try again!");
                }
            }while(prefix == null);
            System.out.println("You input a stem: " + prefix.toStringWithAlphabet());
            do {
                System.out.println("please input loop: ");
                byte[] inputBytes = new byte[1024];
                int len = rollIn.read(inputBytes);
                String input = new String(inputBytes, 0, len);
                input = input.trim();
                System.out.println("input stem is " + input);
                boolean valid = true;
                for(int i = 0; i < input.length(); i ++) {
                    int letter = alphabet.indexOf(input.charAt(i));
                    if(letter < 0) {
                        valid = false;
                        break;
                    }
                }
                if(valid) {
                    suffix = alphabet.getWordFromString(input);
                } else  {
                	String illegal = "Illegal input, try again!";
                	rollOut.write(illegal.getBytes());
                    System.out.println("Illegal input, try again!");
                }
            }while(suffix == null);
            System.out.println("You input a loop: " + suffix.toStringWithAlphabet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new QuerySimple<HashableValue>(prefix, suffix);
    }
    
    

}
