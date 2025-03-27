import java.util.regex.*;
import java.util.*;

public class CollectedStateRegex {
    public static  Map<Integer, String[]>  MatchState(String message) {

        // Regex pattern
        String regex = "<\\d+,[^,]+:\\d+:\\d+,[^,]*,\\{[^}]*\\}:.*?>";

        // Compile the regex
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);

        // Create a list to store matches
        List<String> matches = new ArrayList<>();

        // Find all matches and add them to the list
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return separateMatches(matches);
    }

    private static  Map<Integer, String[]> separateMatches(List<String> matches) {
        Map<Integer, String[]> separatedMatches = new HashMap<>();

        // Iterate over each match
        for (String match : matches) {
            // Find the last occurrence of '}:'
            int lastIndex = match.lastIndexOf("}:");

            if (lastIndex != -1) {
                // Split the string at the last occurrence of '}:'
                String part1 = match.substring(0, lastIndex + 1); // Include '}'
                part1 = part1.substring(1);
                String part2 = match.substring(lastIndex + 2);  // After '}'
                part2 = part2.substring(0, part2.length() -1);

                // Add the two parts to the list
                separatedMatches.put( Integer.parseInt( part1.substring(0,1) ),new String[] { part1.substring(2), part2 });
            }
        }

        return separatedMatches;
    }
}
