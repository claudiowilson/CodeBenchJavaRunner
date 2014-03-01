package CodeBench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CCodeRunner extends CodeRunner {
    public CCodeRunner(ArrayList<CodeFile> codeFiles, String directoryName) {
        super(codeFiles, directoryName);
    }

    @Override
    public String runProgram(String[] input) throws IOException, InterruptedException {
        String[] args = new String[1 + input.length];
        //Define the classpath for java
        args[0] = "."+File.separator+directoryName+File.separator+codeFiles.get(0).getProgramName();
        //Add the input as arguments to the java program
        for (int i = 0; i < input.length; i++) {
            args[i + 1] = input[i];
        }

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
        String[] args = new String[codeFiles.size() + 3];
        //Define the classpatch for javac so the user can call methods in other classes
        args[0] = "gcc";
        args[1] = "-o";
        args[2] = directoryName+File.separator+codeFiles.get(0).getProgramName();
        for (int i = 0; i < codeFiles.size(); i++) {
            args[i + 3] = directoryName + File.separator + codeFiles.get(i).getFileName();
        }
        Process process = new ProcessBuilder(args).start();

        process.waitFor();

        //Get any errors from the compilation
        String error = getError(process);

        if (process.exitValue() != 0) {
            return error;
        }
        return null;
    }
}
