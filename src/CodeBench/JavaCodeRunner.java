package CodeBench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class JavaCodeRunner extends CodeRunner {
    public JavaCodeRunner(ArrayList<CodeFile> codeFiles, String directoryName) {
        super(codeFiles, directoryName);
    }

    @Override
    public String runProgram(String[] input) throws IOException, InterruptedException {
        String[] args = new String[4 + input.length];
        //Define the classpath for java
        args[0] = "java";
        args[1] = "-cp";
        args[2] = directoryName;
        args[3] = codeFiles.get(0).getProgramName();
        //Add the input as arguments to the java program
        for (int i = 0; i < input.length; i++) {
            args[i + 4] = input[i];
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
        args[0] = "javac";
        args[1] = "-sourcepath";
        args[2] = directoryName;
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
