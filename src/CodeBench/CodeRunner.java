package CodeBench;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;

abstract class CodeRunner {
    protected ArrayList<CodeFile> codeFiles;
    protected String directoryName;

    /**
     * Returns an instance of a sub-class of CodeRunner corresponding to the given language. If the language is not
     * recognized, null is returned.
     *
     * @param language
     * @param codeFiles
     * @param directoryName
     *
     * @return
     */
    public static CodeRunner createRunner(String language, ArrayList<CodeFile> codeFiles, String directoryName) {
        switch (language.trim().toLowerCase()) {
            case "java":
                return new JavaCodeRunner(codeFiles, directoryName);
            case "python":
                return new PythonCodeRunner(codeFiles, directoryName);
            case "c":
                return new CCodeRunner(codeFiles, directoryName);
            default:
                return null;
        }
    }

    public CodeRunner(ArrayList<CodeFile> codeFiles, String directoryName) {
        this.codeFiles = codeFiles;
        this.directoryName = directoryName;
    }

    abstract public String runProgram(String input[]) throws IOException,
            InterruptedException;

    abstract public String compileFiles() throws IOException,
            InterruptedException;

    public void createFiles() {
        //Create the folder
        File directory = new File(directoryName);
        directory.mkdirs();
        //Clean out all the files already in the directory
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Look at each code file
        for (CodeFile codeFile : codeFiles) {
            //Create the file
            String fileName = directoryName + File.separator + codeFile.getFileName();
            File file = new File(fileName);
            try {
                file.createNewFile();

                //Add the code to the file
                PrintWriter writer = new PrintWriter(fileName, "UTF-8");
                writer.print(codeFile.getCode());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the errors encountered by the given Process.
     *
     * @param p
     *
     * @return
     *
     * @throws IOException
     */
    protected static String getError(Process p) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                p.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        String result = builder.toString();
        return result;
    }

    /**
     * Returns the output of the given Process.
     *
     * @param p
     *
     * @return
     *
     * @throws IOException
     */
    protected static String getOutput(Process p) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        String result = builder.toString();
        return result;
    }
}
