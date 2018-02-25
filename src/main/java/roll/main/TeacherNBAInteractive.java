package roll.main;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import roll.automata.NBA;
import roll.oracle.Teacher;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.table.HashableValueBoolean;
import roll.words.Word;

public class TeacherNBAInteractive implements Teacher<NBA, Query<HashableValue>, HashableValue> {
    	
    	private PipedOutputStream rollOut;
    	private PipedInputStream rollIn;
    	
    	
    	public TeacherNBAInteractive(PipedOutputStream rollOut, PipedInputStream rollIn) {
    		this.rollIn = rollIn;
            this.rollOut = rollOut;
    	}
    	
        @Override
        public HashableValue answerMembershipQuery(Query<HashableValue> query) {
            Word prefix = query.getPrefix();
            Word suffix = query.getSuffix();
            String memQ = "M-Is w-word (" + prefix.toStringWithAlphabet() + ", " + suffix.toStringWithAlphabet()  + ") in the unknown languge: Yes/No";
            try {
				this.rollOut.write(memQ.getBytes());
				this.rollOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            System.out.println(memQ);
            boolean answer = InteractiveMode.getInputAnswer(this.rollOut, this.rollIn);
            HashableValue result = new HashableValueBoolean(answer);
            query.answerQuery(result);
            return result;
        }

        @Override
        public Query<HashableValue> answerEquivalenceQuery(NBA hypothesis) {
           
            List<String> apList = new ArrayList<>();
            for(int i = 0; i < hypothesis.getAlphabetSize(); i ++) {
                apList.add(hypothesis.getAlphabet().getLetter(i) + "");
            }
            assert hypothesis != null;
            String equiQ = "S-Is following automaton the unknown automaton: 1/0?";
            System.out.println(equiQ);
            byte[] syncBytes = new byte[1024];
            int len;
			try {
				this.rollOut.write(equiQ.getBytes());
				this.rollOut.flush();
				len = this.rollIn.read(syncBytes);
				String syncStr = new String(syncBytes, 0, len);
	            syncStr = syncStr.trim();
	            assert (syncStr.toCharArray()[0] == 'A');
	            System.out.println("E-equiQ Question synced");
	            String hypothesisAutomata ="E-" + hypothesis.toString(apList);
	            this.rollOut.write(hypothesisAutomata.getBytes());
	            this.rollOut.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            boolean answer = InteractiveMode.getInputAnswer(rollOut, rollIn);
            Word wordEmpty = hypothesis.getAlphabet().getEmptyWord();
            Query<HashableValue> ceQuery = new QuerySimple<>(wordEmpty, wordEmpty);
            ceQuery.answerQuery(new HashableValueBoolean(answer));
            return ceQuery;
        }
        
    }