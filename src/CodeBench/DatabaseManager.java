package CodeBench;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;

public class DatabaseManager {
    /**
     * Get all the data corresponding to the submission with the given ID.
     *
     * @param submissionID
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public static void getData(int submissionID)
            throws ClassNotFoundException, SQLException, IOException, InterruptedException {
        //Connect to the postgres database
        Class.forName("org.postgresql.Driver");
        Connection connection = null;
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "postgres",
                "yoloswag");
        connection.setAutoCommit(false);

        //Get all the code for the current submission
        String sql = "SELECT * FROM codebench.code WHERE submission_id=" + submissionID + " ORDER BY code_id;";
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql);

        //The list of code files that correspond to the current submission
        ArrayList<CodeFile> codeFiles = new ArrayList<>();

        //Add each code block to the list
        while (result.next()) {
            String submissionCode = result.getString("code");
            String fileName = result.getString("class_name");

            //Skip over invalid submissions
            if (submissionCode == null || fileName == null) {
                System.out.println("Invalid submission");
                continue;
            }

            CodeFile codeFile = new CodeFile(fileName, submissionCode);
            codeFiles.add(codeFile);
        }

        //Create the .java and .class files
        String directory = "sub_" + submissionID;
        createJavaFile(codeFiles, directory);
        String compileResult = compileJavaFiles(codeFiles, directory);

        //Update the table with the result of the compilation
        if (compileResult == null) {
            statement.executeUpdate("UPDATE codebench.submission SET errors = null WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        else {
            statement.executeUpdate("UPDATE codebench.submission SET errors = '" + compileResult + "' WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        connection.commit();

        //Get the output for the program
        String output = runProgram(codeFiles, directory);

        //--------Get the correct output from the table
        //Get the current submission
        ResultSet resultSet = statement.executeQuery("SELECT * FROM codebench.submission WHERE submission_id=" +
                submissionID + ";");
        resultSet.next();
        //Get the ID for the question this submission is for
        String questionID = resultSet.getString("question");
        //Get the question from the ID
        resultSet = statement.executeQuery("SELECT * FROM codebench.question WHERE question_id=" + questionID + ";");
        resultSet.next();
        //Get the correct output for the question
        String correctOutput = resultSet.getString("output");

        //Trim the output to get rid of trailing new line characters
        output=output.trim();

        if (output.equals(correctOutput)) {
            statement.executeUpdate("UPDATE codebench.submission SET errors = null WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        else {
            statement.executeUpdate("UPDATE codebench.submission SET errors = 'Incorrect output! Your output was: " +
                    output + " but the correct output is: " + correctOutput + "' WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        connection.commit();
        statement.close();
        connection.close();
    }

    private static void createJavaFile(ArrayList<CodeFile> codeFiles, String directoryName) {
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
     * Compiles the java files. Returns null if the compilation was successful.
     *
     * @param directoryName
     *
     * @return
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private static String compileJavaFiles(ArrayList<CodeFile> codeFiles, String directoryName) throws IOException,
            InterruptedException {
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

    public static String runProgram(ArrayList<CodeFile> codeFiles, String directoryName) throws IOException,
            InterruptedException {
        String[] args = new String[4];
        //Define the classpatch for java
        args[0] = "java";
        args[1] = "-cp";
        args[2] = directoryName;
        args[3] = codeFiles.get(0).getProgramName();
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

    /**
     * Returns the errors encountered by the given Process.
     *
     * @param p
     *
     * @return
     *
     * @throws IOException
     */
    private static String getError(Process p) throws IOException {
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
    private static String getOutput(Process p) throws IOException {
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

    public static void main(String args[]) throws Exception {
        getData(0);
        /*ConnectionFactory factory = new ConnectionFactory();
        factory.setUri("amqp://guest:guest@107.170.12.71:5672");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "#");
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        System.out.println("running!");
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            try {
                int submissionId = Integer.parseInt(message);
                getData(submissionId);
            } catch (ClassNotFoundException | SQLException | IOException | NumberFormatException e) {
                e.printStackTrace();
            }
            System.out.println(message);
        }*/
    }
}
