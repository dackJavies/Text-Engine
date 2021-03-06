package bobbin.usability.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BufferedReaderTester extends BufferedReader {
    private final Map<Integer, List<Consumer<String>>> testActions;
    private final ByteArrayOutputStream stream;
    private final PrintWriter writer;
    private final String[] testInputs;
    private int lineNumber = 0;

    BufferedReaderTester(
            BufferedUserInput in, ByteArrayOutputStream stream, PrintWriter writer, StringReader stringReader) {
        super(stringReader);
        this.stream = stream;
        this.writer = writer;
        this.testActions = in.testActions;
        this.testInputs = new String[ in.inputs.size() ];
        in.inputs.toArray(testInputs);
    }

    @Override
    public String readLine() {
        testActions.getOrDefault(lineNumber, Collections.singletonList(s -> {}))
                   .forEach(stringConsumer -> stringConsumer.accept(this.stream.toString()));

        // Include inputs in the program outputs to make the output look like
        // it would were a user actually typing the commands into a terminal.
        this.writer.println(testInputs[ lineNumber ]);
        String input = testInputs[ lineNumber ];
        lineNumber++;
        return input;
    }
}
