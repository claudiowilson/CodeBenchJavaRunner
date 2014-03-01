package CodeBench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PythonCodeRunner extends CodeRunner {
    public PythonCodeRunner(ArrayList<CodeFile> codeFiles, String directoryName) {
        super(codeFiles, directoryName);
    }

    @Override
    public String runProgram() throws IOException, InterruptedException {
        String[] args = new String[2];
        //Run the python code
        args[0] = "python3";
        args[1] = directoryName + File.separator + codeFiles.get(0).getFileName();
        Process process = new ProcessBuilder(args).start();

        //Get any errors from the running
        String error = getError(process);
        String output = getOutput(process);

        process.waitFor();

        //If there was an error running the program, return it
        if (error != null && error.length() > 0) {
            System.out.println(error);
            return error;
        }
        return output;
    }

    @Override
    public String compileFiles() throws IOException, InterruptedException {
        return null;
    }
}
