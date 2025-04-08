import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class BFTChecker {
    private final int N;
    private final int F;

    BFTChecker(int N) {
        this.N = N;
        F = (N) / 3;
    }

    boolean checkQuorumHighest(Map<Integer, State> stateMap, State targetState) {
        int ts = targetState.getValts();
        String v = targetState.getVal();
        int count = 0;

        for (State s : stateMap.values()) {
            int tsPrime = s.getValts();
            String vPrime = s.getVal();

            if (tsPrime < ts || (tsPrime == ts && vPrime.equals(v))) {
                count++;
            }
        }
        return count > (N + F) / 2;
    }

    boolean checkCertifiedValue(Map<Integer, State> stateMap, State targetState) {
        int ts = targetState.getValts();
        String v = targetState.getVal();
        int count = 0;

        for (State s : stateMap.values()) {
            Set<Writeset.Pair<Integer, String>> set = s.getWriteset().getWriteSet();
            for (Writeset.Pair<Integer, String> pair : set) {
                int tsPrime = pair.getKey();
                String vPrime = pair.getValue();
                if (tsPrime >= ts && vPrime.equals(v)) {
                    count++;
                    break;  // No need to check further writes in the current writeset
                }
            }
        }
        return count > F;
    }

    boolean checkUnbound(Map<Integer, State> stateMap) {
        int count = 0;
        for (State s : stateMap.values()) {
            int tsPrime = s.getValts();
            if (tsPrime == 0 ) {
                count++;
            }
        }
        return count > (N + F) / 2;
    }

    boolean checkIfBinds(Map<Integer, State> stateMap){
        for (State s : stateMap.values()) {
            if(checkCertifiedValue(stateMap, s) && checkQuorumHighest(stateMap, s)){
                return true;
            }
        }
        return false;
    }

    boolean checkIfSound(Map<Integer, State> stateMap){
        return checkIfBinds(stateMap) || checkUnbound(stateMap);
    }
}



class BFTCheckerTest {

    // Helper method to create a State
    private static State createState(int ts, String val) {
        return new State(ts, val, 0, new Writeset());
    }

    // Method to manually run the tests
    public static void runTests() {
        testHighestTrue();
        testHighestFalse();
        testHighestExactlyHalf();
        testHighestNoProcesses();
        testCertifiedValueTrue();
        testCertifiedValueFalse();
        testUnboundTrue();
        testUnboundFalse();
        testBindsTrue();
        testBindsLowFalse();
        testBindsNotCertified();
        testSoundCertified();
        testSoundUnbound();
        testSoundFalse();
    }

    public static void testHighestTrue() {
        BFTChecker checker = new BFTChecker(4); // N=4, F=1, threshold = (4+1)/2 = 2.5

        Map<Integer, State> states = new HashMap<>();
        states.put(1, createState(5, "x"));
        states.put(2, createState(11, "v"));
        states.put(3, createState(10, "v"));
        states.put(4, createState(8, "y"));

        State target = createState(10, "v");

        boolean result = checker.checkQuorumHighest(states, target);
        System.out.println("testQuorumReached: " + (result ? "PASS" : "FAIL"));
    }

    public static void testHighestFalse() {
        BFTChecker checker = new BFTChecker(5); // (5+1)/2 = 3

        Map<Integer, State> states = new HashMap<>();
        states.put(1, createState(10, "v"));
        states.put(2, createState(9, "w"));
        states.put(3, createState(11, "v"));
        states.put(4, createState(10, "x"));
        states.put(5, createState(12, "z"));

        State target = createState(10, "v");

        boolean result = checker.checkQuorumHighest(states, target);
        System.out.println("testQuorumNotReached: " + (!result ? "PASS" : "FAIL"));
    }

    public static void testHighestExactlyHalf() {
        BFTChecker checker = new BFTChecker(6); // (6+2)/2 = 4

        Map<Integer, State> states = new HashMap<>();
        states.put(1, createState(10, "v"));
        states.put(2, createState(10, "z"));
        states.put(3, createState(9, "x"));
        states.put(4, createState(8, "y"));
        states.put(5, createState(11, "z"));
        states.put(6, createState(13, "v"));

        State target = createState(10, "v");

        boolean result = checker.checkQuorumHighest(states, target);
        System.out.println("testExactlyHalfPlusF: " + (!result ? "PASS" : "FAIL"));
    }

    public static void testHighestNoProcesses() {
        BFTChecker checker = new BFTChecker(4);
        Map<Integer, State> states = new HashMap<>();
        State target = createState(5, "val");

        boolean result = checker.checkQuorumHighest(states, target);
        System.out.println("testNoProcesses: " + (!result ? "PASS" : "FAIL"));
    }

    public static void testCertifiedValueTrue() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(7, "v", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(7, "v");
        ws2.addElement(8, "b");
        State state2 = new State(7, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "v");
        State state3 = new State(7, "v", 7, ws3);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3
        );

        State targetState = new State(7, "v", 7, new Writeset());

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkCertifiedValue(stateMap, targetState);
        System.out.println("testCertifiedValueTrue: " + (result ? "PASS" : "FAIL")); // Only state 1 and 2 are correct so it should pass
    }

    public static void testCertifiedValueFalse() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(7, "v", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(8, "b");
        State state2 = new State(7, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(7, "v", 7, ws3);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3
        );

        State targetState = new State(7, "v", 7, new Writeset());

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkCertifiedValue(stateMap, targetState);
        System.out.println("testCertifiedValueFalse: " + (!result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }
    public static void testUnboundTrue() {
        BFTChecker checker = new BFTChecker(4); // N=4, F=1, threshold = (4+1)/2 = 2.5

        Map<Integer, State> states = new HashMap<>();
        states.put(1, createState(1, "x"));
        states.put(2, createState(0, "v"));
        states.put(3, createState(0, "v"));
        states.put(4, createState(0, "y"));


        boolean result = checker.checkUnbound(states);
        System.out.println("testUnboundTrue: " + (result ? "PASS" : "FAIL"));
    }

    public static void testUnboundFalse() {
        BFTChecker checker = new BFTChecker(4); // N=4, F=1, threshold = (4+1)/2 = 2.5

        Map<Integer, State> states = new HashMap<>();
        states.put(1, createState(1, "x"));
        states.put(2, createState(2, "v"));
        states.put(3, createState(0, "v"));
        states.put(4, createState(0, "y"));


        boolean result = checker.checkUnbound(states);
        System.out.println("testUnboundFalse: " + (!result ? "PASS" : "FAIL"));
    }

    public static void testBindsTrue() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(5, "x", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(11, "v");
        State state2 = new State(11, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(10, "v", 7, ws3);

        Writeset ws4 = new Writeset();
        ws3.addElement(12, "a");
        State state4 = new State(8, "y", 7, ws4);

        Writeset ws5 = new Writeset();
        ws3.addElement(12, "a");
        State state5 = new State(8, "y", 7, ws5);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3,
                4, state4,
                5, state5
        );

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkIfBinds(stateMap);
        System.out.println("testBindsTrue: " + (result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }

    public static void testBindsLowFalse() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(99, "x", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(11, "v");
        State state2 = new State(11, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(10, "v", 7, ws3);

        Writeset ws4 = new Writeset();
        ws3.addElement(12, "a");
        State state4 = new State(8, "y", 7, ws4);

        Writeset ws5 = new Writeset();
        ws3.addElement(12, "a");
        State state5 = new State(8, "y", 7, ws5);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3,
                4, state4,
                5, state5
        );

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkIfBinds(stateMap);
        System.out.println("testBindsFalse: " + (!result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }

    public static void testBindsNotCertified() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(5, "x", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(11, "a");
        State state2 = new State(11, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(10, "v", 7, ws3);

        Writeset ws4 = new Writeset();
        ws3.addElement(12, "a");
        State state4 = new State(8, "y", 7, ws4);

        Writeset ws5 = new Writeset();
        ws3.addElement(12, "a");
        State state5 = new State(8, "y", 7, ws5);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3,
                4, state4,
                5, state5
        );


        State targetState = createState(10, "v");

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkIfBinds(stateMap);
        System.out.println("testBindsNotCertified: " + (!result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }

    public static void testSoundCertified() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(5, "x", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(11, "v");
        State state2 = new State(11, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(10, "v", 7, ws3);

        Writeset ws4 = new Writeset();
        ws3.addElement(12, "a");
        State state4 = new State(8, "y", 7, ws4);

        Writeset ws5 = new Writeset();
        ws3.addElement(12, "a");
        State state5 = new State(8, "y", 7, ws5);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3,
                4, state4,
                5, state5
        );


        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkIfSound(stateMap);
        System.out.println("testSoundCertified: " + (result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }

    public static void testSoundUnbound() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(0, "x", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(11, "a");
        State state2 = new State(0, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(0, "v", 7, ws3);

        Writeset ws4 = new Writeset();
        ws3.addElement(12, "a");
        State state4 = new State(0, "y", 7, ws4);

        Writeset ws5 = new Writeset();
        ws3.addElement(12, "a");
        State state5 = new State(1, "y", 7, ws5);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3,
                4, state4,
                5, state5
        );

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkIfSound(stateMap);
        System.out.println("testSoundUnbound: " + (result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }

    public static void testSoundFalse() {
        BFTChecker checker = new BFTChecker(5); // Example: f = 1, allow up to 1 faulty node

        // Sample writesets for testing
        Writeset ws1 = new Writeset();
        ws1.addElement(5, "a");
        ws1.addElement(10, "v");
        State state1 = new State(2, "x", 7, ws1);

        Writeset ws2 = new Writeset();
        ws2.addElement(11, "a");
        State state2 = new State(0, "v", 7, ws2);

        Writeset ws3 = new Writeset();
        ws3.addElement(12, "a");
        State state3 = new State(0, "v", 7, ws3);

        Writeset ws4 = new Writeset();
        ws3.addElement(12, "a");
        State state4 = new State(0, "y", 7, ws4);

        Writeset ws5 = new Writeset();
        ws3.addElement(12, "a");
        State state5 = new State(1, "y", 7, ws5);

        // Map to hold states (using process IDs as keys)
        Map<Integer, State> stateMap = Map.of(
                1, state1,
                2, state2,
                3, state3,
                4, state4,
                5, state5
        );

        // Check if there exists a quorum for a given timestamp and value
        boolean result = checker.checkIfSound(stateMap);
        System.out.println("testSoundFalse: " + (!result ? "PASS" : "FAIL")); // Only state 1 is correct so it should be false
    }



    public static void main(String[] args) {
        // Run all tests
        runTests();
    }
}

